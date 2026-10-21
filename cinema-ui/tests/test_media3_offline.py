"""A fresh dependency checkout must validate without touching the network."""
from pathlib import Path
import hashlib, importlib.util, json, shutil, tempfile
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as d:
 root=Path(d)
 shutil.copy(ROOT/'fetch_media3.py',root/'fetch_media3.py')
 shutil.copy(ROOT/'media3-dependencies.json',root/'media3-dependencies.json')
 (root/'runtime-libs').mkdir()
 rows=json.loads((root/'media3-dependencies.json').read_text())
 for row in rows:
  name=row['url'].rsplit('/',1)[1].replace('.aar','.jar')
  shutil.copy(ROOT/'runtime-libs'/name,root/'runtime-libs'/name)
 spec=importlib.util.spec_from_file_location('isolated_dependencies',root/'fetch_media3.py');mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
 with patch.object(mod.urllib.request,'urlopen',side_effect=AssertionError('offline build tried the network')):
  mod.fetch()
  target=root/'runtime-libs'/name
  original=target.read_bytes();target.write_bytes(b'corrupt')
  try:mod.fetch()
  except FileNotFoundError:pass
  else:raise AssertionError('corrupt dependency was accepted')
  target.unlink()
  try:mod.fetch()
  except FileNotFoundError:pass
  else:raise AssertionError('missing dependency was accepted')
  target.write_bytes(original);mod.fetch()
print('PASS: fresh vendored dependency checkout validates offline; corrupt/missing files fail without networking.')
