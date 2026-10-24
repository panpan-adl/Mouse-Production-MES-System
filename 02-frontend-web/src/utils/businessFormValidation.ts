type Control = HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement
interface ValueRule { label: string; value: string; required: boolean; type?: string; min?: string; max?: string; placeholderSelection?: boolean }
export function fieldValueError(rule: ValueRule): string {
  const value=rule.value.trim(), label=rule.label||'此项'
  if(rule.required&&(!value||rule.placeholderSelection))return `请填写或选择${label}`
  if(!value)return ''
  if(rule.type==='number'){
    const number=Number(value)
    if(!Number.isFinite(number))return `${label}必须填写有效数字`
    if(rule.min!==undefined&&rule.min!==''&&number<Number(rule.min))return `${label}不能小于${rule.min}`
    if(rule.max!==undefined&&rule.max!==''&&number>Number(rule.max))return `${label}不能大于${rule.max}`
  }
  return ''
}
const selector='[data-business-form]'
function labelFor(field: Control): string {
  const label=field.labels?.[0]
  return (field.getAttribute('aria-label')||label?.childNodes&&Array.from(label.childNodes).filter(n=>n.nodeType===Node.TEXT_NODE||n.nodeType===Node.ELEMENT_NODE&&(n as Element).tagName==='SPAN').map(n=>n.textContent).join('')||field.getAttribute('placeholder')||field.name||'此项').replace(/\s+/g,' ').replace(/\*/g,'').trim()
}
function controls(container: Element): Control[]{return Array.from(container.querySelectorAll<Control>('input,select,textarea')).filter(f=>!f.disabled&&f.type!=='hidden'&&f.getClientRects().length>0)}
function activeForm(): HTMLElement | undefined {return Array.from(document.querySelectorAll<HTMLElement>(selector)).filter(d=>d.getClientRects().length>0).at(-1)}
export function showBusinessFormError(message: string, container=activeForm()) {
  if(!container)return
  let alert=container.querySelector<HTMLElement>('[data-form-validation-error]')
  if(!alert){alert=document.createElement('div');alert.dataset.formValidationError='';alert.setAttribute('role','alert');alert.setAttribute('aria-live','assertive');alert.className='mb-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700 whitespace-pre-line';(container.querySelector('.modal-card')||container.querySelector('form')||container.firstElementChild||container).prepend(alert)}
  alert.textContent=message
}
export function validateBusinessForm(container: HTMLElement): boolean {
  const fields=controls(container),errors:string[]=[];let first:Control|undefined
  for(const field of fields){
    field.removeAttribute('aria-invalid')
    const label=labelFor(field)
    let error=fieldValueError({label,value:field.value,required:field.required,type:field.type,min:field.getAttribute('min')??undefined,max:field.getAttribute('max')??undefined,placeholderSelection:field instanceof HTMLSelectElement&&['0',''].includes(field.value)&&/请选择|选择.*(?:设备|人员|来源|物料|工单|仓库)/.test(field.selectedOptions[0]?.textContent||'')})
    if(!error&&!field.validity.valid){
      if(field.validity.stepMismatch)error=`${label}的数值步长不正确${field.getAttribute('step')==='1'?'，请填写整数':''}`
      else if(field.validity.tooLong)error=`${label}超过允许的长度`
      else error=`${label}：${field.validationMessage||'填写格式不正确'}`
    }
    if(error){field.setAttribute('aria-invalid','true');errors.push(error);first??=field}
  }
  for(const end of fields.filter(f=>f.dataset.dateRole==='end'&&f.value)){
    const start=fields.find(f=>f.dataset.dateRole==='start'&&f.dataset.datePair===end.dataset.datePair)
    if(start?.value&&new Date(end.value).getTime()<=new Date(start.value).getTime()){
      errors.push(`${labelFor(end)}必须晚于${labelFor(start)}`);end.setAttribute('aria-invalid','true');first??=end
    }
  }
  if(errors.length){showBusinessFormError('尚未保存，请修改以下内容：\n'+[...new Set(errors)].map(e=>'• '+e).join('\n'),container);first?.focus();return false}
  container.querySelector('[data-form-validation-error]')?.remove()
  return true
}
export function installBusinessFormValidation() {
  document.addEventListener('click',event=>{
    const button=(event.target as Element|null)?.closest('button')
    const dialog=button?.closest<HTMLElement>(selector)
    if(!button||!dialog||button.disabled)return
    const text=button.textContent?.trim()||''
    if(!/^(?:保存|确认|提交|新增并|创建并|冻结并|登记)/.test(text)||/取消|关闭|返回|删除|移除/.test(text))return
    if(!validateBusinessForm(dialog)){event.preventDefault();event.stopImmediatePropagation()}
  },true)
  document.addEventListener('submit',event=>{
    const dialog=(event.target as Element).closest<HTMLElement>(selector)
    if(dialog&&!validateBusinessForm(dialog)){event.preventDefault();event.stopImmediatePropagation()}
  },true)
  document.addEventListener('invalid',event=>{
    const dialog=(event.target as Element).closest<HTMLElement>(selector)
    if(dialog){event.preventDefault();validateBusinessForm(dialog)}
  },true)
}
