const {request,getBaseUrl}=require('../../utils/api.js')
Page({
  data:{tab:'tasks',role:0,tasks:[],checks:[],calendar:[],reports:[],templates:[],items:[],lines:[],selected:null,templateIndex:0,templateOptions:[],results:[],quantity:'',badQuantity:'0',actual:'',checkResult:'PASS',error:'',busy:false,reportId:null},
  onShow(){const user=wx.getStorageSync('userInfo');if(!user){wx.redirectTo({url:'/pages/login/login'});return}this.user=user;this.setData({role:Number(user.roleId)});this.load()},
  async onPullDownRefresh(){try{await this.load()}finally{wx.stopPullDownRefresh()}},
  async load(){this.setData({error:''});try{
    const role=this.data.role
    const paths=['/api/mes/calendar','/api/foundation/qc-templates','/api/foundation/qc-items','/api/foundation/qc-lines']
    const [calendar,templates,items,lines]=await Promise.all(paths.map(url=>request({url})))
    const tasks=[1,2,3,4,8,9].includes(role)?await request({url:'/api/mes/planning'}):[]
    const checks=[1,2,8,9].includes(role)?await request({url:'/api/mes/checks'}):[]
    const reports=[1,2,3,4,5,9].includes(role)?await request({url:'/api/mes/reports'}):[]
    this.setData({calendar,reports,templates,items,lines,tasks:tasks.map(t=>({...t,state:['待领取','已派发','生产中','待质检','已完成','已取消'][t.task_status],canReport:Number(t.assigned_user_id)===Number(this.user.id)&&[1,2].includes(t.task_status)})),checks})
  }catch(e){this.fail(e)}},
  fail(e){this.setData({error:e.msg||e.message||'请求失败，请检查连接或岗位权限'})},
  switchTab(e){this.setData({tab:e.currentTarget.dataset.tab,selected:null,reportId:null})},
  scan(){wx.scanCode({onlyFromCamera:false,success:async r=>{try{const resolved=await request({url:'/api/mes/resolve-barcode',data:{content:r.result}});if(resolved.type==='STATION'){this.setData({tasks:this.data.tasks.filter(t=>Number(t.station_id)===Number(resolved.id)),tab:'tasks'});return}if(resolved.type==='PACKAGE'&&[1,2,5,7,9].includes(this.data.role)){const contents=await request({url:`/api/mes/packages/${resolved.id}/contents`});wx.showModal({title:'包装箱追溯',content:contents.map(i=>`${i.material_name} · ${i.lot_no} · ${i.quantity}`).join('\n')||'此箱没有装入物料',showCancel:false});return}wx.showModal({title:'识别成功',content:resolved.content,showCancel:false})}catch(e){this.fail(e)}}})},
  input(e){this.setData({[e.currentTarget.dataset.field]:e.detail.value})},
  async start(e){await this.act(()=>request({url:`/api/production-tasks/${e.currentTarget.dataset.id}/start`,method:'PUT',data:{}}))},
  report(e){this.setData({reportId:e.currentTarget.dataset.id,quantity:'',badQuantity:'0'})},
  async submitReport(){const good=Number(this.data.quantity),bad=Number(this.data.badQuantity);if(!Number.isInteger(good)||good<0||!Number.isInteger(bad)||bad<0||good+bad<=0){this.setData({error:'合格品与不良品须为非负整数且总量大于零'});return}if(await this.act(()=>request({url:'/api/mes/reports',method:'POST',data:{task_id:this.data.reportId,good_quantity:good,bad_quantity:bad}})))this.setData({reportId:null})},
  async sop(e){try{const records=await request({url:`/api/mes/task-sops/${e.currentTarget.dataset.id}`});if(!records.length){wx.showToast({title:'此工序尚未配置SOP',icon:'none'});return}const images=[];for(const record of records){const file=await new Promise((resolve,reject)=>wx.downloadFile({url:getBaseUrl()+record.image_path,header:{Authorization:wx.getStorageSync('token')||''},success:r=>r.statusCode===200?resolve(r.tempFilePath):reject(new Error('SOP读取失败')),fail:reject}));images.push(file)}wx.previewImage({urls:images})}catch(e){this.fail(e)}},
  inspect(e){const task=this.data.tasks.find(t=>t.id===Number(e.currentTarget.dataset.id));const options=this.data.templates.filter(t=>t.kind==='PROCESS'&&Number(t.operation_id)===Number(task.operation_id)&&Number(t.product_id)===Number(task.product_id));this.setData({selected:task,templateOptions:options.map(t=>({...t,display:t.name+' · '+({FIRST:'首检',LAST:'末检',PATROL:'巡检',FINAL:'成品/最终检验'}[t.phase]||t.phase)})),templateIndex:0,quantity:task.quantity});this.templateChanged({detail:{value:0}})},
  templateChanged(e){const index=Number(e.detail.value),template=this.data.templateOptions[index];if(!template){this.setData({results:[]});return}const ids=this.data.lines.filter(l=>Number(l.template_id)===Number(template.id)).map(l=>Number(l.item_id));this.setData({templateIndex:index,results:this.data.items.filter(i=>ids.includes(Number(i.id))).map(i=>({...i,actual:'',result:'PASS'}))})},
  actualChanged(e){const rows=this.data.results.slice();rows[Number(e.currentTarget.dataset.index)].actual=e.detail.value;this.setData({results:rows})},
  verdictChanged(e){const rows=this.data.results.slice();rows[Number(e.currentTarget.dataset.index)].result=Number(e.detail.value)===0?'PASS':'FAIL';this.setData({results:rows})},
  async submitInspection(){const t=this.data.templateOptions[this.data.templateIndex];if(!t||!this.data.results.length){this.setData({error:'请配置适用于本工序的检测模板'});return}const results={};for(const r of this.data.results)results[String(r.id)]={actual:r.actual,result:r.result};if(await this.act(()=>request({url:'/api/foundation/inspections',method:'POST',data:{template_id:t.id,source_type:'TASK',source_id:this.data.selected.id,quantity:Number(this.data.quantity),qualified_quantity:Number(this.data.quantity),results}})))this.setData({selected:null})},
  openCheck(e){this.setData({checkId:Number(e.currentTarget.dataset.id),planId:Number(e.currentTarget.dataset.plan),actual:'',tab:'checks'})},
  checkVerdict(e){this.setData({checkResult:Number(e.detail.value)===0?'PASS':'FAIL'})},
  async recordCheck(){const now=new Date(),local=new Date(now.getTime()-now.getTimezoneOffset()*60000).toISOString().slice(0,19);if(await this.act(()=>request({url:'/api/foundation/check-records',method:'POST',data:{plan_id:this.data.planId,checked_at:local,actual_value:this.data.actual,result:this.data.checkResult}})))this.setData({checkId:null})},
  async act(action){if(this.data.busy)return false;this.setData({busy:true,error:''});try{await action();wx.showToast({title:'操作成功'});await this.load();return true}catch(e){this.fail(e);return false}finally{this.setData({busy:false})}}
})
