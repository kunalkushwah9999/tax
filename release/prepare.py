#!/usr/bin/env python3
"""Prepare an origin-specific synthetic web/Android/VM bundle. Never publishes."""
from pathlib import Path
import argparse, hashlib, html, importlib.util, json, re, shutil, sys, tempfile, zipfile
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
HERE = Path(__file__).resolve().parent
JAR = 'tax-platform-startup-5.1.0.jar'

def configuration(origin, application_id, support_email, fingerprints):
    u = urlsplit(origin)
    if (u.scheme != 'https' or u.username or u.password or u.port not in (None, 443)
            or u.path or u.query or u.fragment or not u.hostname or origin != 'https://' + u.hostname):
        raise ValueError('Use a canonical HTTPS origin without port, path, credentials or query')
    if not re.fullmatch(r'[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+', u.hostname) or len(u.hostname) > 253 or u.hostname.rsplit('.', 1)[1].isdigit():
        raise ValueError('A DNS hostname is required')
    if not re.fullmatch(r'[a-z][a-z0-9_]*(?:\.[a-z][a-z0-9_]*){2,}', application_id) or len(application_id) > 180:
        raise ValueError('Use a lowercase reverse-domain Android application ID with 3+ components')
    if not re.fullmatch(r'[A-Za-z0-9._+-]{1,64}@[a-z0-9.-]+\.[a-z]{2,63}', support_email) or len(support_email) > 254:
        raise ValueError('Use a plain support mailbox, without a display name or URL parameters')
    normalized = []
    for fingerprint in fingerprints:
        if not re.fullmatch(r'(?:[0-9A-Fa-f]{2}:){31}[0-9A-Fa-f]{2}', fingerprint):
            raise ValueError('Signing fingerprint must be 32 colon-separated SHA-256 bytes')
        fingerprint = fingerprint.upper()
        if fingerprint not in normalized:
            normalized.append(fingerprint)
    if len(normalized) > 4:
        raise ValueError('At most four active Play app-signing certificates')
    return {'origin': origin, 'hostname': u.hostname, 'application_id': application_id,
            'support_email': support_email, 'fingerprints': normalized}

def source_integrity(root):
    spec = importlib.util.spec_from_file_location('taxdesk_delivery', root / 'manage.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    required = {p.relative_to(root).as_posix() for p in (HERE / 'templates').rglob('*') if p.is_file()}
    required.add('release/prepare.py')
    module.verify(root, module.REQUIRED | required)
    jar = root / 'startup_app/build' / JAR
    with zipfile.ZipFile(jar) as archive:
        for name in ['index.html', 'app.js', 'security.js', 'workbench.js', 'styles.css', 'privacy.js', 'deletion-status.js', 'deletion.html']:
            if archive.read('web/' + name) != (root / 'startup_app/src/main/resources/web' / name).read_bytes():
                raise ValueError('Bundled web source differs from tested runtime: ' + name)
    return hashlib.sha256(jar.read_bytes()).hexdigest()

def render(c, destination, root=ROOT):
    """Generate public assets/config only; no passwords, TLS keys, SDKs or signing keys."""
    jar_hash = source_integrity(root)
    destination = Path(destination).absolute()
    if destination.exists() or destination.is_symlink():
        raise ValueError('Destination must be new; existing bundles are never overwritten')
    if not destination.parent.is_dir():
        raise ValueError('Destination parent must already exist')
    template_files = {p.relative_to(HERE / 'templates').as_posix(): p.read_bytes()
                      for p in (HERE / 'templates').rglob('*') if p.is_file()}
    tokens = {'HOSTNAME': c['hostname'], 'ORIGIN': c['origin'],
              'APPLICATION_ID': c['application_id'], 'SUPPORT_EMAIL': c['support_email'], 'JAR': JAR,
              'ASSET_STATEMENTS': html.escape(json.dumps([{'relation': ['delegate_permission/common.handle_all_urls'],
                'target': {'namespace': 'web', 'site': c['origin']}}], separators=(',', ':')).replace('"', '\\"'), quote=False)}
    with tempfile.TemporaryDirectory(prefix='.taxdesk-build-', dir=destination.parent) as temporary:
        stage = Path(temporary) / 'bundle'
        stage.mkdir()
        def write(name, data):
            path = stage / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data if isinstance(data, bytes) else data.encode('utf-8'))
        for name, raw in template_files.items():
            if name.endswith('.png'):
                write(name, raw)
                continue
            text = raw.decode('utf-8')
            for key, value in tokens.items():
                text = text.replace('@@' + key + '@@', value)
            if re.search(r'@@[A-Z_]+@@', text):
                raise ValueError('Unresolved template token: ' + name)
            write(name, text)
        web = root / 'startup_app/src/main/resources/web'
        for name in ['app.js', 'security.js', 'workbench.js', 'styles.css', 'privacy.js', 'deletion-status.js', 'deletion.html']:
            write('web/' + name, (web / name).read_bytes())
        deletion_page=(web / 'deletion.html').read_text()
        deletion_page=deletion_page.replace('Lost-password identity recovery and an external support mailbox are not connected in this synthetic pilot.',
            'Lost-password recovery and automated identity verification are not connected. For a reviewed deletion request or lost-access assistance, contact '
            + '<a href="mailto:' + html.escape(c['support_email'], quote=True) + '">' + html.escape(c['support_email']) + '</a>. '
            + 'Do not send passwords, PAN, authenticator codes or tax documents. The deployment owner must monitor this mailbox and verify requests before acting.')
        write('web/account-deletion.html', deletion_page)
        index = (web / 'index.html').read_text()
        index = index.replace('</head>', '<link rel="manifest" href="/manifest.webmanifest"><meta name="theme-color" content="#123e35"><script src="/install.js" defer></script></head>')
        index = index.replace('</body>', '<div class="install-bar" id="install-bar" hidden><button type="button" class="button" id="install-app">Install TaxDesk pilot</button><button type="button" class="text-button" id="dismiss-install">Dismiss</button></div><link rel="stylesheet" href="/install.css"></body>')
        write('web/index.html', index)
        manifest = {'id': '/', 'name': 'TaxDesk Pilot', 'short_name': 'TaxDesk', 'lang': 'en-IN',
                    'description': 'Synthetic tax preparation workspace. Filing and live payments are unreleased.',
                    'start_url': '/', 'scope': '/', 'display': 'standalone',
                    'background_color': '#f8faf7', 'theme_color': '#123e35',
                    'icons': [{'src': '/icons/icon-' + str(size) + '.png', 'sizes': str(size) + 'x' + str(size),
                               'type': 'image/png', 'purpose': 'any maskable'} for size in [192, 512]]}
        write('web/manifest.webmanifest', json.dumps(manifest, indent=2) + '\n')
        links = []
        if c['fingerprints']:
            links.append({'relation': ['delegate_permission/common.handle_all_urls'], 'target': {
                'namespace': 'android_app', 'package_name': c['application_id'],
                'sha256_cert_fingerprints': c['fingerprints']}})
        write('web/.well-known/assetlinks.json', json.dumps(links, indent=2) + '\n')
        state = {'release_layer': 1, 'application_version': '5.1.0', 'runtime_jar_sha256': jar_hash,
                 'origin': c['origin'], 'application_id': c['application_id'], 'target_sdk': 36,
                 'digital_asset_links_configured': bool(links), 'android_build_verified': False,
                 'cloud_deployed': False, 'independent_tax_approvals': 0, 'production_ready': False,
                 'synthetic_data_only': True, 'public_launch_blocked': True,
                 'blocking_scope': ['independent tax expectations', 'full statutory ITR/GST/capital-gains implementation',
                     'authorized live filing', 'live payments and Play billing policy review', 'real CA verification',
                     'operator/payment-account deletion and external/backup privacy review', 'production recovery and scale validation',
                     'Android build/signing/device QA and Play review', 'actual HTTPS/VM/MySQL deployment validation']}
        write('release-status.json', json.dumps(state, indent=2) + '\n')
        checks = []
        for path in sorted(stage.rglob('*')):
            if path.is_file():
                checks.append(hashlib.sha256(path.read_bytes()).hexdigest() + '  ' + path.relative_to(stage).as_posix())
        write('checksums.sha256', '\n'.join(checks) + '\n')
        stage.rename(destination)
    return state

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--origin', required=True)
    p.add_argument('--application-id', required=True)
    p.add_argument('--support-email', required=True)
    p.add_argument('--app-signing-sha256', action='append', default=[])
    p.add_argument('--out', type=Path, required=True)
    args = p.parse_args()
    try:
        result = render(configuration(args.origin, args.application_id, args.support_email, args.app_signing_sha256), args.out)
    except (ValueError, OSError, zipfile.BadZipFile) as error:
        print(json.dumps({'status': 'RELEASE_PREPARATION_FAILED', 'reason': str(error), 'production_ready': False}))
        return 2
    print(json.dumps(result, indent=2))
    return 0

if __name__ == '__main__':
    sys.exit(main())
