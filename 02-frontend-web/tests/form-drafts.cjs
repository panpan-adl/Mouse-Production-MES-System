const assert=require('node:assert/strict')
const fs=require('node:fs'),path=require('node:path'),Module=require('node:module')
const ts=require('typescript'),vue=require('vue')
const values=new Map()
global.localStorage={getItem:k=>values.get(k)||null,setItem:(k,v)=>values.set(k,v),removeItem:k=>values.delete(k)}
global.window={addEventListener(){},removeEventListener(){}}
const route={path:'/purchase/order'},auth={currentUser:{id:9}}
let source=fs.readFileSync(path.join(__dirname,'../src/composables/useFormDrafts.ts'),'utf8')
source=source.replace(/import \{ useRoute \} from 'vue-router'/,"const useRoute=()=>global.__draftRoute").replace(/import \{ useAuthStore \} from '@\/stores\/auth'/,"const useAuthStore=()=>global.__draftAuth")
global.__draftRoute=route;global.__draftAuth=auth
const compiled=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
const mod=new Module(path.join(__dirname,'draft-test.cjs'));mod.paths=module.paths;mod.filename=path.join(__dirname,'draft-test.cjs');mod._compile(compiled,mod.filename)
const {useFormDrafts}=mod.exports
const renderer=vue.createRenderer({createElement:()=>({}),insert(){},remove(){},setElementText(){},createText:()=>({}),createComment:()=>({}),setText(){},setComment(){},parentNode:()=>null,nextSibling:()=>null,patchProp(){}})
function mount(){let state;const app=renderer.createApp({setup(){const open=vue.ref(false),form=vue.reactive({id:null,name:'',lines:[],password:''}),selection=vue.ref(''),record=vue.ref(null);const complete=useFormDrafts({order:{open,fields:{form,selection},record:()=>record.value}});state={open,form,selection,record,complete};return()=>null}});app.mount({});return {...state,unmount:()=>app.unmount()}}
async function settle(){await vue.nextTick();await vue.nextTick();await vue.nextTick()}
async function run(){
 let a=mount();a.open.value=true;a.form.name='半成品采购';a.form.lines.push({quantity:12});a.form.password='never-save';a.selection.value='supplier-2';a.open.value=false
 assert(![...values.values()].join('').includes('never-save'),'credentials must not be saved')
 a.form.name='';a.form.lines=[];a.selection.value='';a.open.value=true;await settle()
 assert.equal(a.form.name,'半成品采购');assert.equal(a.form.lines[0].quantity,12);assert.equal(a.selection.value,'supplier-2')
 console.log('PASS close and reopen restores nested form data and selected references')
 a.unmount();a=mount();a.open.value=true;await settle();assert.equal(a.form.name,'半成品采购')
 console.log('PASS leaving and remounting page restores saved progress')
 a.complete();assert.equal(a.open.value,false);assert.equal(values.size,0);a.form.name='';a.open.value=true;await settle();assert.equal(a.form.name,'')
 console.log('PASS successful save clears draft and closes dialog without stale restoration')
 a.form.name='failed-save';await settle();assert.equal(a.open.value,true);a.open.value=false;a.form.name='';a.open.value=true;await settle();assert.equal(a.form.name,'failed-save')
 console.log('PASS unsuccessful save retains draft and keeps the form available')
 a.open.value=false;a.record.value=17;a.form.name='record-17';a.open.value=true;await settle();a.form.name='record-17-draft';a.open.value=false;a.record.value=18;a.form.name='record-18';a.open.value=true;await settle();assert.equal(a.form.name,'record-18')
 console.log('PASS editing different documents never restores another record draft')
 a.open.value=false;a.unmount();auth.currentUser.id=11;const b=mount();b.open.value=true;await settle();assert.equal(b.form.name,'')
 console.log('PASS drafts are isolated between accounts')
 b.open.value=false;b.unmount();auth.currentUser.id=9;route.path='/inventory/material-requests';const c=mount();c.open.value=true;await settle();assert.equal(c.form.name,'');c.unmount()
 console.log('PASS drafts are isolated between business pages')
 console.log('Form draft regression: 7 checks passed')
}
run().catch(error=>{console.error(error);process.exitCode=1})
