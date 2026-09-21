import {defineConfig} from 'vite';
export default defineConfig({base:'./',server:{host:'0.0.0.0',allowedHosts:true},build:{target:'es2020',outDir:'dist',assetsInlineLimit:0}});
