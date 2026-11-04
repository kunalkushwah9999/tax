#!/usr/bin/env python3
"""Delivery-tool tests only; no independent tax approval."""
from pathlib import Path
import hashlib,importlib.util,json,tempfile,unittest
ROOT=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('taxdesk_manage',ROOT/'manage.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class DeliveryTests(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name);(self.root/'file.txt').write_text('synthetic');self.hash=hashlib.sha256(b'synthetic').hexdigest();self.manifest(self.hash+'  file.txt\n')
 def tearDown(self):self.temp.cleanup()
 def manifest(self,value):(self.root/'checksums.sha256').write_text(value)
 def reject(self,value):
  self.manifest(value)
  with self.assertRaises(m.IntegrityError):m.verify(self.root)
 def test_01_valid_manifest(self):self.assertEqual(m.verify(self.root)['files_verified'],1)
 def test_02_changed_file(self):
  (self.root/'file.txt').write_text('changed')
  with self.assertRaises(m.IntegrityError):m.verify(self.root)
 def test_03_missing_file(self):self.reject(self.hash+'  missing.txt\n')
 def test_04_missing_manifest(self):
  (self.root/'checksums.sha256').unlink()
  with self.assertRaises(m.IntegrityError):m.verify(self.root)
 def test_05_duplicate_path(self):self.reject((self.hash+'  file.txt\n')*2)
 def test_06_parent_escape(self):self.reject(self.hash+'  ../file.txt\n')
 def test_07_absolute_path(self):self.reject(self.hash+'  /tmp/file.txt\n')
 def test_08_windows_drive(self):self.reject(self.hash+'  C:/file.txt\n')
 def test_09_backslash(self):self.reject(self.hash+'  a\\file.txt\n')
 def test_10_noncanonical_paths(self):
  for name in ['a//b','./file.txt','a/./b','file.txt/']:self.reject(self.hash+'  '+name+'\n')
 def test_11_symlink(self):
  (self.root/'link').symlink_to(self.root/'file.txt');self.reject(self.hash+'  link\n')
 def test_12_bad_digest(self):self.reject('not-a-digest  file.txt\n')
 def test_13_required_file_coverage(self):
  with self.assertRaises(m.IntegrityError):m.verify(self.root,{'manage.py'})
 def test_14_unlisted_runtime_data_ignored(self):
  (self.root/'runtime-data').mkdir();(self.root/'runtime-data/private').write_text('synthetic');self.assertTrue(m.verify(self.root)['integrity_ok']);self.assertFalse(m.verify(self.root)['publisher_signature_verified'])
if __name__=='__main__':
 result=unittest.TextTestRunner(verbosity=1).run(unittest.defaultTestLoader.loadTestsFromTestCase(DeliveryTests));print(json.dumps({'delivery_tool_groups':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'tax_approval':False}));raise SystemExit(0 if result.wasSuccessful() else 1)
