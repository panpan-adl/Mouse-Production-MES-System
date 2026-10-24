<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ArrowRight, Eye, EyeOff, LockKeyhole, UserRound, Factory, ShieldCheck, Layers3, Workflow, ScanLine } from 'lucide-vue-next'
const auth=useAuthStore(),router=useRouter()
const userCode=ref(''),password=ref(''),showPassword=ref(false),loading=ref(false),error=ref('')
const demo=import.meta.env.VITE_DEMO_MODE==='1'
async function login(){
  if(loading.value)return
  error.value=''
  if(!/^\d{6}$/.test(userCode.value.trim())){error.value='请输入6位数字工号';return}
  if(!password.value){error.value='请输入登录密码';return}
  loading.value=true
  try{if(await auth.login(userCode.value.trim(),password.value))await router.push(auth.redirectPath);else error.value='工号或密码错误，请重新输入'}catch{error.value='暂时无法登录，请稍后重试'}finally{loading.value=false}
}
</script>
<template>
  <div class="login-page">
    <section class="login-story">
      <a href="/login" class="login-brand"><span class="login-brand-icon"><Factory :size="23" /></span><span>MOUSE MES<small>制造执行管理平台</small></span></a>
      <div class="story-copy"><div class="story-eyebrow"><span></span> CONNECT YOUR FACTORY</div><h1>让生产的每一步，<br/>都清晰可见<span>。</span></h1><p>从一份生产计划，到每一道工序的执行。<br/>连接人、设备与物料，构建有序、高效的生产现场。</p></div>
      <div class="factory-visual" aria-hidden="true">
        <div class="visual-topline"><span>MANUFACTURING WORKFLOW</span><span>01 — 03</span></div>
        <svg class="factory-lines" viewBox="0 0 600 150" fill="none"><path d="M80 75H520" stroke="#3d6288" stroke-dasharray="5 7"/><path d="M80 75V30H520V75M80 75V120H520V75" stroke="#294563"/><circle cx="300" cy="30" r="4" fill="#4496df"/><circle cx="460" cy="120" r="4" fill="#4aafab"/></svg>
        <div class="workflow-stages"><div class="workflow-stage"><span class="stage-number">01</span><Layers3/><strong>计划排产</strong><small>PLAN & SCHEDULE</small></div><div class="workflow-stage stage-focus"><span class="stage-number">02</span><Workflow/><strong>生产执行</strong><small>EXECUTE & CONTROL</small></div><div class="workflow-stage"><span class="stage-number">03</span><ScanLine/><strong>质量追溯</strong><small>QUALITY & TRACE</small></div></div>
        <div class="visual-bottomline"><span class="visual-dot"></span><span>全流程协同 · 全要素连接 · 全生命周期追溯</span></div>
      </div>
      <div class="story-footer"><span>为每一次制造，建立可靠连接。</span><span>MOUSE / MES</span></div>
    </section>
    <section class="login-access">
      <div class="login-top-note"><ShieldCheck :size="15"/><span>企业工作空间</span><span v-if="demo" class="login-demo">演示环境</span></div>
      <div class="login-card"><div class="login-greeting">WELCOME BACK</div><h2>欢迎回来</h2><p class="login-description">登录您的账号，开始今天的生产协作。</p>
        <form @submit.prevent="login" class="login-fields">
          <label for="login-user-code">员工工号</label><div class="login-input"><UserRound :size="18"/><input id="login-user-code" v-model="userCode" autocomplete="username" inputmode="numeric" maxlength="6" placeholder="请输入6位员工工号" required /></div>
          <div class="password-label"><label for="login-password">登录密码</label><span>使用您的企业账号密码</span></div><div class="login-input"><LockKeyhole :size="18"/><input id="login-password" v-model="password" :type="showPassword?'text':'password'" autocomplete="current-password" placeholder="请输入登录密码" required /><button type="button" :aria-label="showPassword?'隐藏密码':'显示密码'" @click="showPassword=!showPassword"><EyeOff v-if="showPassword" :size="17"/><Eye v-else :size="17"/></button></div>
          <p v-if="error" class="login-error" role="alert">{{error}}</p>
          <button class="login-submit" :disabled="loading" type="submit"><span>{{loading?'正在登录…':'登录工作台'}}</span><ArrowRight v-if="!loading" :size="18"/><span v-else class="login-spinner"></span></button>
        </form>
        <p class="login-help"><ShieldCheck :size="14"/>账号由管理员统一分配，如需帮助请联系管理员。</p>
      </div>
      <footer class="login-access-footer"><span>Mouse MES · 制造执行管理系统</span><span>连接生产，协同未来</span></footer>
    </section>
  </div>
</template>
<style scoped>
.login-page{min-height:100svh;display:grid;grid-template-columns:1.05fr 1fr;background:#fff;font-family:'Inter','Segoe UI','Microsoft YaHei',sans-serif}
.login-story{background:#132439;position:relative;overflow:hidden;display:flex;flex-direction:column;min-height:100svh;padding:44px 8%;color:white;background-image:radial-gradient(ellipse at 85% 70%,#21436a50,transparent 65%)}
.login-story::before{content:'';position:absolute;inset:0;background-image:linear-gradient(#ffffff04 1px,transparent 1px);pointer-events:none}
.login-brand{display:flex;align-items:center;gap:12px;font-size:17px;font-weight:650;letter-spacing:.09em;z-index:1;width:fit-content}
.login-brand-icon{height:43px;width:43px;display:grid;place-items:center;border:1px solid #5473945c;border-radius:12px;background:#253c55}
.login-brand small{display:block;font-size:10px;font-weight:400;color:#7890a8;letter-spacing:.18em;margin-top:5px}
.story-copy{margin:auto 0 28px;padding-top:70px;position:relative;z-index:1}
.story-eyebrow{font-size:9px;letter-spacing:.22em;color:#80a8ce;display:flex;align-items:center;gap:10px;margin-bottom:24px}
.story-eyebrow span{width:22px;height:1px;background:#4d9be4}
.story-copy h1{font-size:clamp(30px,3.15vw,53px);line-height:1.55;letter-spacing:-.055em;font-weight:600;color:#edf3fb}
.story-copy h1>span{color:#5caaed}
.story-copy p{font-size:12px;line-height:2.15;color:#8ea3bb;margin-top:23px;letter-spacing:.025em}
.factory-visual{position:relative;margin:24px 0 auto;padding:20px 0 0;z-index:1}
.visual-topline{display:flex;justify-content:space-between;color:#587590;font-size:8px;letter-spacing:.18em;border-top:1px solid #2d4056;padding-top:20px;margin-bottom:30px}
.factory-lines{position:absolute;width:100%;height:170px;top:45px;left:0}
.workflow-stages{display:flex;justify-content:space-around;gap:20px;position:relative}
.workflow-stage{width:122px;padding:15px;border:1px solid #354e68;border-radius:12px;background:#192d43;display:flex;flex-direction:column;align-items:flex-start;gap:9px;position:relative;box-shadow:0 8px 20px #07142220}
.workflow-stage svg{color:#719abb;width:23px;height:23px;margin:6px 0}
.workflow-stage strong{font-size:12px;font-weight:500;color:#b9cde0}
.workflow-stage small{font-size:6px;color:#69829d;letter-spacing:.08em;white-space:nowrap}
.stage-number{position:absolute;right:11px;top:10px;font-size:8px;color:#526d87}
.stage-focus{border-color:#467aac;background:#203c59;transform:translateY(-9px)}
.stage-focus svg{color:#6cbbff}.stage-focus strong{color:#e0eeff}
.visual-bottomline{display:flex;align-items:center;gap:8px;margin:30px 0;font-size:8px;letter-spacing:.05em;color:#657d96}
.visual-dot{width:4px;height:4px;border-radius:50%;background:#4b959c}
.story-footer{display:flex;justify-content:space-between;font-size:9px;color:#567089;margin-top:40px;letter-spacing:.05em}
.login-access{display:flex;flex-direction:column;padding:43px 9% 28px;background:#fbfcfe;position:relative}
.login-top-note{display:flex;align-items:center;gap:7px;justify-content:flex-end;color:#9aa8b8;font-size:10px}
.login-demo{font-size:9px;margin-left:10px;padding:4px 9px;border:1px solid #f0dfbd;background:#fff9eb;color:#af8950;border-radius:5px}
.login-card{width:100%;max-width:370px;margin:auto;align-self:center;padding:65px 0}
.login-greeting{font-size:9px;letter-spacing:.2em;font-weight:600;color:#7a9bbd;margin-bottom:15px}
.login-card h2{font-size:32px;letter-spacing:-.045em;font-weight:650;color:#20344c;margin-bottom:12px}
.login-description{font-size:12px;color:#8a99ac;line-height:1.8}
.login-fields{margin-top:39px}
.login-fields label{display:block;font-size:11px;font-weight:600;color:#435770;margin-bottom:10px}
.login-input{display:flex;align-items:center;gap:12px;background:white;border:1px solid #dee5ef;border-radius:10px;height:49px;padding:0 15px;transition:border .15s,box-shadow .15s;color:#a1b1c3}
.login-input:focus-within{border-color:#6da8e9;box-shadow:0 0 0 4px #2376df0b}
.login-input input{border:0;outline:0;min-width:0;width:100%;height:100%;font-size:12px;background:transparent;color:#283d56}
.login-input input::placeholder{color:#abb7c5}.login-input button{padding:5px;color:#95a6bb}.login-input button:hover{color:#347bd1}
.password-label{display:flex;justify-content:space-between;align-items:center;margin-top:24px}.password-label span{font-size:9px;color:#a1adbc;margin-bottom:10px}
.login-error{color:#bf4456;background:#fff0f2;border:1px solid #f5d8df;padding:10px;border-radius:8px;font-size:11px;margin-top:15px}
.login-submit{margin-top:29px;display:flex;align-items:center;justify-content:space-between;width:100%;padding:0 18px;height:49px;border-radius:9px;color:#fff;background:#2878da;font-size:12px;box-shadow:0 5px 12px #1f6bcb20}
.login-submit:hover{background:#206bca;box-shadow:0 6px 16px #1f6bcb30}.login-submit:disabled{opacity:.6}
.login-help{display:flex;gap:6px;align-items:center;color:#a3aebc;font-size:9px;margin-top:24px;line-height:1.8}
.login-access-footer{display:flex;justify-content:space-between;color:#a8b2c0;font-size:9px;gap:15px}
.login-spinner{width:15px;height:15px;border:2px solid #ffffff50;border-top-color:white;border-radius:50%;animation:rotate .7s linear infinite}
@keyframes rotate{to{transform:rotate(360deg)}}
@media(min-width:1700px){.login-story{padding:55px 10%}.story-copy{padding-top:90px}}
@media(max-width:1000px){.login-story{padding:32px 7%}.login-access{padding:32px 8%}.workflow-stages{gap:12px}.workflow-stage{padding:12px;width:106px}.story-copy h1{font-size:32px}.workflow-stage small{font-size:5px}}
@media(max-width:760px){.login-page{grid-template-columns:1fr}.login-story{min-height:auto;padding:24px;background:#14263c}.story-copy{padding:0;margin:30px 0 12px}.story-copy h1{font-size:28px;line-height:1.5}.story-copy p,.factory-visual,.story-footer,.story-eyebrow{display:none}.login-brand{font-size:14px}.login-access{min-height:62svh;padding:25px}.login-card{padding:34px 0;max-width:390px}.login-top-note{position:absolute;right:25px;top:25px}.login-card h2{font-size:26px}.login-greeting{margin-top:15px}.login-fields{margin-top:25px}.login-help{font-size:9px}.login-access-footer{margin-top:12px}}
</style>
