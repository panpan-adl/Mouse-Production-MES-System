/* Static miniapp contract checks, without claiming a real-device launch. */
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert');
const root=path.resolve(__dirname,'../03-miniprogram');
const config=JSON.parse(fs.readFileSync(path.join(root,'app.json'),'utf8'));
const context={module:{exports:{}},wx:{getStorageSync:()=>''},console};
vm.runInNewContext(fs.readFileSync(path.join(root,'utils/api.js'),'utf8'),context);
const api=context.module.exports;
let pages=0,methods=0;
for(const page of config.pages){
  for(const ext of ['.js','.json','.wxml'])assert(fs.existsSync(path.join(root,page+ext)),`Missing ${page+ext}`);
  const source=fs.readFileSync(path.join(root,page+'.js'),'utf8');new vm.Script(source,{filename:page+'.js'});pages++;
  for(const match of source.matchAll(/\b([a-zA-Z]+Api)\.([a-zA-Z]+)\s*\(/g)){assert(api[match[1]]&&typeof api[match[1]][match[2]]==='function',`Missing API contract ${match[1]}.${match[2]} in ${page}`);methods++;}
}
for(const item of config.tabBar.list)assert(config.pages.includes(item.pagePath),'Tab page is not registered');
assert(config.pages.includes('pages/mes/index'),'MES field operations page is not registered');
console.log(`Miniapp static checks passed: ${pages} registered pages and ${methods} API method references. Real-device testing remains separate.`);
