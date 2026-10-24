const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),Module=require('node:module'),ts=require('typescript')
const source=fs.readFileSync(path.join(__dirname,'../src/utils/businessFormValidation.ts'),'utf8')
const compiled=ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText
const mod=new Module(__filename);mod.filename=__filename;mod.paths=module.paths;mod._compile(compiled,__filename)
const {fieldValueError,validateBusinessForm,showBusinessFormError,installBusinessFormValidation}=mod.exports
assert.match(fieldValueError({label:'故障描述',value:'   ',required:true}),/故障描述/)
assert.match(fieldValueError({label:'负责人',value:'0',required:true,placeholderSelection:true}),/负责人/)
assert.match(fieldValueError({label:'每隔',value:'0',required:true,type:'number',min:'1'}),/不能小于1/)
assert.match(fieldValueError({label:'费用',value:'-1',required:true,type:'number',min:'0'}),/不能小于0/)
assert.match(fieldValueError({label:'天数',value:'366',required:true,type:'number',max:'365'}),/不能大于365/)
assert.equal(fieldValueError({label:'备注',value:'',required:false}),'')
assert.equal(fieldValueError({label:'每隔',value:'1',required:true,type:'number',min:'1'}),'')
console.log('PASS required whitespace, placeholder selection, numeric bounds and optional fields')
global.Node={TEXT_NODE:3,ELEMENT_NODE:1};global.HTMLSelectElement=class {}
const handlers={}
global.document={addEventListener:(name,fn)=>handlers[name]=fn,querySelectorAll:()=>[],createElement:()=>({dataset:{},setAttribute(){},textContent:''})}
function field(label,value,extra={}){const attrs={'aria-label':label,...extra.attrs};return {value,required:false,type:'text',disabled:false,dataset:{},validity:{valid:true},getAttribute:k=>attrs[k]??null,setAttribute:(k,v)=>attrs[k]=v,removeAttribute:k=>delete attrs[k],getClientRects:()=>[1],focus(){this.focused=true},...extra}}
function container(fields){let alert;return {querySelectorAll:()=>fields,querySelector:s=>s==='[data-form-validation-error]'?alert:s==='.modal-card'?{prepend:a=>{alert=a;a.remove=()=>alert=undefined}}:null,getClientRects:()=>[1],get alert(){return alert}}}
const title=field('故障描述','   ',{required:true}),quantity=field('需求数量','-2',{type:'number',attrs:{min:'1'}}),c=container([title,quantity])
assert.equal(validateBusinessForm(c),false);assert.match(c.alert.textContent,/故障描述/);assert.match(c.alert.textContent,/需求数量/);assert(title.focused)
console.log('PASS invalid forms report all errors inside the dialog and focus the first invalid field')
title.value='皮带断裂';quantity.value='2';assert.equal(validateBusinessForm(c),true);assert.equal(c.alert,undefined)
console.log('PASS correcting errors clears alert and permits the save handler')
const start=field('开始时间','2026-11-02T08:00',{dataset:{dateRole:'start',datePair:'time'}}),end=field('结束时间','2026-11-01T08:00',{dataset:{dateRole:'end',datePair:'time'}}),dates=container([start,end])
assert.equal(validateBusinessForm(dates),false);assert.match(dates.alert.textContent,/结束时间必须晚于开始时间/)
console.log('PASS impossible start/end time range is rejected before saving')
showBusinessFormError('该设备已有未完成维修单，不能重复创建',c);assert.match(c.alert.textContent,/已有未完成维修单/)
console.log('PASS server feasibility errors are rendered inside the active form')
installBusinessFormValidation();title.value='';let stopped=false,prevented=false
const button={disabled:false,textContent:'保存',closest:()=>c}
handlers.click({target:{closest:()=>button},preventDefault(){prevented=true},stopImmediatePropagation(){stopped=true}})
assert(prevented&&stopped,'invalid click must never reach the persistence handler')
console.log('PASS invalid save click is intercepted before any save request')
console.log('Business form validation: 6 checks passed')
