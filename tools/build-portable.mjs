import { build } from 'esbuild';
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, '..');
process.chdir(root);

await fs.mkdir('build', {recursive:true});
await fs.mkdir('deliverables', {recursive:true});
await fs.mkdir('desktop', {recursive:true});

// Bundle JS via esbuild
await build({
  entryPoints: ['app/main.js'],
  bundle: true,
  format: 'iife',
  minify: true,
  target: 'es2020',
  outfile: 'build/portable.js',
  external: ['/assets/*'],
  legalComments: 'eof',
  loader: { '.css': 'empty' } // CSS handled separately
});

let js = await fs.readFile('build/portable.js','utf8');
let css = '';
try {
  css = await fs.readFile('app/style.css','utf8');
} catch {}
let html = await fs.readFile('index.html','utf8');

const mime={webp:'image/webp',png:'image/png',jpg:'image/jpeg',mp3:'audio/mpeg',ttf:'font/ttf',woff2:'font/woff2',svg:'image/svg+xml'};
let count=0;
const references=new Set([...`${js}\n${css}\n${html}`.matchAll(/\/assets\/([a-zA-Z0-9_.\-]+)/g)].map(m=>m[1]));

for(const name of references){
  try {
    const raw=await fs.readFile(path.join('public/assets',name));
    const ext = name.split('.').pop().toLowerCase();
    const data=`data:${mime[ext]||'application/octet-stream'};base64,${raw.toString('base64')}`;
    const from='/assets/'+name;
    js=js.split(from).join(data);
    css=css.split(from).join(data);
    html=html.split(from).join(data);
    count++;
  } catch (e) {
    console.warn(`Missing asset: ${name}`);
  }
}

// Also embed assets referenced in public/assets themselves? Already covered

html=html.replace(/<script type="module" src="\/app\/main\.js"><\/script>/,'<script>'+js.replace(/<\/script/gi,'<\\/script')+'</script>');
// Remove any other module scripts
html=html.replace(/<script type="module"[^>]*><\/script>/g,'');
html=html.replace('</head>','<style>'+css+'</style>\n</head>');
html=html.replace('<title>Miyu — a little company, a little magic</title>','<title>Miyu — your little desktop companion</title>');
// Ensure viewport and meta
if (!html.includes('<!-- Miyu')) {
  html='<!-- Miyu 1.2 · self-contained portable edition · artwork and studio reactions are AI-generated. See README and THIRD-PARTY-NOTICES. -->\n'+html;
}

await fs.writeFile('deliverables/Miyu-Portable.html',html);
await fs.writeFile('desktop/ui.html',html);
await fs.writeFile('Miyu-Portable.html',html).catch(()=>{});
console.log(`Portable edition: ${(Buffer.byteLength(html)/1024/1024).toFixed(2)} MB; ${count} local assets embedded; no CDN dependencies.`);
console.log(`Written to deliverables/Miyu-Portable.html (${(html.length/1024).toFixed(0)} KB) and desktop/ui.html`);
