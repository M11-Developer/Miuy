import {build} from 'esbuild';
import fs from 'node:fs/promises';
import path from 'node:path';

const root=path.resolve(import.meta.dirname,'..');
process.chdir(root);
await fs.mkdir('build',{recursive:true});
await fs.mkdir('deliverables',{recursive:true});
await build({entryPoints:['app/main.js'],bundle:true,format:'iife',minify:true,target:'es2020',outfile:'build/portable.js',external:['/assets/*'],legalComments:'eof'});
let js=await fs.readFile('build/portable.js','utf8');
let css=await fs.readFile('build/portable.css','utf8');
let html=await fs.readFile('index.html','utf8');
const mime={webp:'image/webp',png:'image/png',jpg:'image/jpeg',mp3:'audio/mpeg',ttf:'font/ttf',woff2:'font/woff2'};
let count=0;
const references=new Set([...`${js}\n${css}\n${html}`.matchAll(/\/assets\/([a-zA-Z0-9_.-]+)/g)].map(m=>m[1]));
for(const name of references){
  const raw=await fs.readFile(path.join('public/assets',name));
  const data=`data:${mime[name.split('.').at(-1)]||'application/octet-stream'};base64,${raw.toString('base64')}`;
  const from='/assets/'+name;
  js=js.split(from).join(data);css=css.split(from).join(data);html=html.split(from).join(data);count++;
}
html=html.replace(/<script type="module" src="\/app\/main\.js"><\/script>/,'<script>'+js.replace(/<\/script/gi,'<\\/script')+'</script>');
html=html.replace('</head>','<style>'+css+'</style>\n</head>');
html=html.replace('<title>Miyu — a little company, a little magic</title>','<title>Miyu — your little desktop companion</title>');
html='<!-- Miyu 1.0 · self-contained portable edition · artwork and studio reactions are AI-generated. See the supplied README and THIRD-PARTY-NOTICES. -->\n'+html;
await fs.writeFile('deliverables/Miyu-Portable.html',html);
await fs.writeFile('desktop/ui.html',html);
console.log(`Portable edition: ${(Buffer.byteLength(html)/1024/1024).toFixed(2)} MB; ${count} local assets embedded; no CDN dependencies.`);
