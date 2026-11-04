#!/usr/bin/env python3
"""Meaningful release binding, injection, preservation and privacy checks; no SDK needed."""
from pathlib import Path
import hashlib, importlib.util, json, subprocess, sys, tempfile, unittest, xml.etree.ElementTree as ET
import prepare

ROOT = prepare.ROOT
class ReleaseTests(unittest.TestCase):
    def config(self, **changes):
        values = dict(origin='https://pilot.taxdesk.test', application_id='in.taxdesk.pilot',
                      support_email='support@taxdesk.test', fingerprints=['AB:' * 31 + 'AB'])
        values.update(changes)
        return prepare.configuration(**values)

    def test_origin_rejects_injection_credentials_and_ambiguous_hosts(self):
        for value in ['http://taxdesk.test', 'https://u:p@taxdesk.test', 'https://taxdesk.test/',
                      'https://taxdesk.test:443', 'https://taxdesk.test?q=1', 'https://taxdesk.test#x',
                      'https://Taxdesk.test', 'https://localhost', 'https://127.0.0.1',
                      'https://taxdesk.test;return 200', 'https://taxdesk.test\nother', 'https://-bad.test']:
            with self.subTest(value=value), self.assertRaises(ValueError): self.config(origin=value)

    def test_application_and_mailbox_reject_injection(self):
        for value in ['in.taxdesk', 'in.taxdesk.pilot;bad', "in.taxdesk.'bad", 'In.taxdesk.pilot']:
            with self.subTest(value=value), self.assertRaises(ValueError): self.config(application_id=value)
        for value in ['support@taxdesk.test?body=PAN', 'a@b.test\nBcc:x@y.test', '<x@y.test>']:
            with self.subTest(value=value), self.assertRaises(ValueError): self.config(support_email=value)

    def test_certificate_deduplication_and_strict_bytes(self):
        c = self.config(fingerprints=['ab:' * 31 + 'ab', 'AB:' * 31 + 'AB'])
        self.assertEqual(len(c['fingerprints']), 1)
        for value in ['AA', 'A' * 64, 'AB:' * 32, 'AB:' * 31 + 'ZZ']:
            with self.assertRaises(ValueError): self.config(fingerprints=[value])

    def test_artifact_binding_and_no_destructive_overwrite(self):
        with tempfile.TemporaryDirectory() as temp:
            out = Path(temp) / 'release'
            state = prepare.render(self.config(), out)
            self.assertFalse(state['production_ready'])
            self.assertTrue(state['public_launch_blocked'])
            self.assertFalse(state['android_build_verified'])
            original = (out / 'release-status.json').read_bytes()
            with self.assertRaises(ValueError): prepare.render(self.config(), out)
            self.assertEqual(original, (out / 'release-status.json').read_bytes())
            for name in ['app.js', 'security.js', 'workbench.js', 'styles.css']:
                self.assertEqual((out / 'web' / name).read_bytes(), (ROOT / 'startup_app/src/main/resources/web' / name).read_bytes())
            for line in (out / 'checksums.sha256').read_text().splitlines():
                digest, name = line.split('  ', 1)
                self.assertEqual(digest, hashlib.sha256((out / name).read_bytes()).hexdigest())

    def test_android_and_web_use_same_origin_app_and_play_certificate(self):
        with tempfile.TemporaryDirectory() as temp:
            out = Path(temp) / 'release'; prepare.render(self.config(), out)
            links = json.loads((out / 'web/.well-known/assetlinks.json').read_text())
            self.assertEqual(links[0]['target']['package_name'], 'in.taxdesk.pilot')
            self.assertEqual(links[0]['target']['sha256_cert_fingerprints'], ['AB:' * 31 + 'AB'])
            manifest = ET.parse(out / 'android/app/src/main/AndroidManifest.xml')
            ns = '{http://schemas.android.com/apk/res/android}'
            self.assertEqual([p.get(ns + 'name') for p in manifest.findall('uses-permission')], ['android.permission.INTERNET'])
            app = manifest.find('application'); self.assertEqual(app.get(ns + 'usesCleartextTraffic'), 'false')
            strings = ET.parse(out / 'android/app/src/main/res/values/strings.xml')
            asset = next(x.text for x in strings.getroot() if x.get('name') == 'asset_statements')
            self.assertEqual(json.loads(asset.replace('\\"', '"'))[0]['target']['site'], 'https://pilot.taxdesk.test')
            gradle = (out / 'android/app/build.gradle').read_text()
            self.assertIn('targetSdk 36', gradle)
            self.assertIn('androidbrowserhelper:2.7.4', gradle)

    def test_missing_certificate_does_not_invent_domain_verification(self):
        with tempfile.TemporaryDirectory() as temp:
            out = Path(temp) / 'release'; state = prepare.render(self.config(fingerprints=[]), out)
            self.assertFalse(state['digital_asset_links_configured'])
            self.assertEqual(json.loads((out / 'web/.well-known/assetlinks.json').read_text()), [])

    def test_nginx_keeps_native_limit_and_backend_loopback(self):
        with tempfile.TemporaryDirectory() as temp:
            out = Path(temp) / 'release'; prepare.render(self.config(), out)
            nginx = (out / 'cloud/nginx.conf').read_text()
            self.assertIn('client_max_body_size 400000;', nginx)
            self.assertIn('client_max_body_size 65536;', nginx)
            self.assertIn('manifest-src', nginx)
            self.assertIn('worker-src', nginx)
            service = (out / 'cloud/taxdesk.service').read_text()
            self.assertIn(prepare.JAR, service)
            self.assertNotIn('3.0.0', service)
            self.assertIn('--storage mysql-concurrent', service)
            self.assertNotIn('--storage local', service)

    def test_service_worker_executes_private_bypass_and_network_only_navigation(self):
        # Execute the actual worker with stubbed Web APIs; browser test separately checks real Cache Storage.
        script = ROOT / 'release/test_worker.mjs'
        subprocess.run(['node', str(script), str(ROOT / 'release/templates/web/sw.js')], check=True, capture_output=True, text=True)

if __name__ == '__main__':
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(ReleaseTests)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    print(json.dumps({'release_test_groups': result.testsRun, 'failures': len(result.failures), 'errors': len(result.errors),
                      'actual_android_build': False, 'production_ready': False}))
    sys.exit(not result.wasSuccessful())
