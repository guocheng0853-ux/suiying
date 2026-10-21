"""Fetch pinned upstream Media3 binaries; verify both archive and extracted JAR hashes."""
from pathlib import Path
import hashlib,io,json,urllib.request,zipfile

def fetch(download=False):
 root=Path(__file__).resolve().parent
 for row in json.loads((root/'media3-dependencies.json').read_text()):
  filename=row['url'].rsplit('/',1)[1].replace('.aar','.jar')
  target=root/'runtime-libs'/filename
  if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest()==row['jar_sha256']:continue
  if not download:raise FileNotFoundError('Missing or corrupt vendored dependency: '+str(target)+'; restore it from git, or explicitly run fetch_media3.py --download')
  target.parent.mkdir(parents=True,exist_ok=True)
  with urllib.request.urlopen(row['url'],timeout=45) as response:archive=response.read()
  if hashlib.sha256(archive).hexdigest()!=row['aar_sha256']:raise ValueError('upstream checksum mismatch: '+filename)
  with zipfile.ZipFile(io.BytesIO(archive)) as z:jar=z.read('classes.jar')
  if hashlib.sha256(jar).hexdigest()!=row['jar_sha256']:raise ValueError('jar checksum mismatch: '+filename)
  temporary=target.with_suffix('.tmp');temporary.write_bytes(jar);temporary.replace(target)
if __name__=='__main__':
 import argparse
 parser=argparse.ArgumentParser(description='Verify vendored Media3 JARs offline; --download explicitly repairs missing files.')
 parser.add_argument('--download',action='store_true')
 fetch(parser.parse_args().download)
