"""Bundle reviewed source and a small portable Windows release (no runtime installer)."""
from pathlib import Path
import zipfile, hashlib, shutil
ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'deliverables';DEST.mkdir(exist_ok=True)
source_files=[]
for name in ['index.html','package.json','package-lock.json','vite.config.js','README.md','TESTING.md','THIRD-PARTY-NOTICES.txt','.gitignore']:
 source_files.append(ROOT/name)
for directory in ['app','tools','licenses','public/assets']:
 source_files.extend(p for p in (ROOT/directory).rglob('*') if p.is_file() and '__pycache__' not in p.parts)
for p in (ROOT/'desktop').rglob('*'):
 if p.is_file() and (p.suffix in ['.go','.ico'] or p.name in ['go.mod','go.sum']):source_files.append(p)
source_files.extend((ROOT/'tests').glob('*.mjs'))
with zipfile.ZipFile(DEST/'Miyu-Source.zip','w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for p in sorted(set(source_files)):
  z.write(p,'Miyu-Source/'+p.relative_to(ROOT).as_posix())
for name in ['README.md','TESTING.md','THIRD-PARTY-NOTICES.txt']:
 shutil.copy2(ROOT/name,DEST/name)
checks=[]
for name in ['Miyu.exe','Miyu-Portable.html','Miyu-Source.zip']:
 data=(DEST/name).read_bytes();checks.append(hashlib.sha256(data).hexdigest()+'  '+name)
(DEST/'SHA256SUMS.txt').write_text('\n'.join(checks)+'\n')
files=['Miyu.exe','Miyu-Portable.html','Miyu-Source.zip','START HERE.txt','README.md','TESTING.md','THIRD-PARTY-NOTICES.txt','SHA256SUMS.txt']
with zipfile.ZipFile(DEST/'Miyu-Windows.zip','w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for name in files:z.write(DEST/name,'Miyu/'+name)
 for p in (ROOT/'licenses').glob('*'):z.write(p,'Miyu/licenses/'+p.name)
 # Validate integrity before surfacing the package.
assert zipfile.ZipFile(DEST/'Miyu-Windows.zip').testzip() is None
public=ROOT/'public/downloads';public.mkdir(parents=True,exist_ok=True)
for name in ['Miyu-Windows.zip','Miyu-Portable.html']:
 shutil.copy2(DEST/name,public/name)
for name in ['Miyu-Windows.zip','Miyu-Portable.html','Miyu-Source.zip','Miyu.exe']:
 print(name,round((DEST/name).stat().st_size/1024/1024,2),'MB')
print('Source files:',len(set(source_files)))
