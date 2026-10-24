<script setup lang="ts">
import { ref } from 'vue'
import LoginParticleCloud from '@/components/LoginParticleCloud.vue'
import { motionEnabled } from '@/stores/layout'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ArrowRight, Eye, EyeOff, LockKeyhole, UserRound, Factory, ShieldCheck } from 'lucide-vue-next'
const auth=useAuthStore(),router=useRouter()
const userCode=ref(''),password=ref(''),showPassword=ref(false),loading=ref(false),error=ref('')
const demo=import.meta.env.VITE_DEMO_MODE==='1'
async function login(){
  if(loading.value)return
  error.value=''
  if(!/^\d{6}$/.test(userCode.value.trim())){error.value='请输入6位数字工号';return}
  if(!password.value){error.value='请输入登录密码';return}
  loading.value=true
  try {
    await auth.login(userCode.value.trim(), password.value)
    await router.push(auth.redirectPath)
  } catch (cause) {
    const message = cause instanceof Error ? cause.message : ''
    if (/用户编码或密码错误/.test(message)) error.value = '工号或密码错误，请重新输入'
    else if (/timeout/i.test(message)) error.value = '登录请求超时，请稍后重试'
    else if (/Network Error|status code 50[234]|ECONNREFUSED/i.test(message)) error.value = '暂时无法连接登录服务，请稍后重试'
    else if (/禁用|停用|联系管理员/.test(message)) error.value = message
    else error.value = '暂时无法登录，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>
<template>
  <div class="login-page notion-login" :class="{'motion-paused':!motionEnabled}">
    <section class="login-story">
      <div class="login-particle-stage" aria-hidden="true"><LoginParticleCloud :active="motionEnabled"/></div>
      <a href="/login" class="login-brand"><span class="login-brand-icon"><Factory :size="23" /></span><span>MOUSE MES<small>制造执行管理平台</small></span></a>
      <div class="story-copy"><div class="story-eyebrow"><span></span> YOUR EVERYDAY WORKSPACE</div><h1>让生产井然有序，<br/>让协作轻松发生<span>。</span></h1><p>从一份生产计划，到每一道工序的执行。<br/>连接人、设备与物料，构建有序、高效的生产现场。</p></div>
      <div class="login-workflow"><span>01 · 生产计划</span><span>02 · 工序执行</span><span>03 · 质量追溯</span></div>
      <div class="story-footer"><span>为每一次制造，建立可靠连接。</span><span>MOUSE / MES</span></div>
    </section>
    <section class="login-access">
      <div class="login-top-note"><ShieldCheck :size="15"/><span>企业工作空间</span><span v-if="demo" class="login-demo">演示环境</span></div>
      <div class="login-card"><div class="login-greeting"><span class="login-status-light"></span> ENTER YOUR WORKSPACE</div><h2>欢迎回来</h2><p class="login-description">登录您的账号，开始今天的生产协作。</p>
        <form @submit.prevent="login" class="login-fields">
          <label for="login-user-code">员工工号</label><div class="login-input"><UserRound :size="18"/><input id="login-user-code" v-model="userCode" autocomplete="username" inputmode="numeric" maxlength="6" placeholder="请输入6位员工工号" required /></div>
          <div class="password-label"><label for="login-password">登录密码</label><span>使用您的企业账号密码</span></div><div class="login-input"><LockKeyhole :size="18"/><input id="login-password" v-model="password" :type="showPassword?'text':'password'" autocomplete="current-password" placeholder="请输入登录密码" required /><button type="button" :aria-label="showPassword?'隐藏密码':'显示密码'" @click="showPassword=!showPassword"><EyeOff v-if="showPassword" :size="17"/><Eye v-else :size="17"/></button></div>
          <p v-if="error" class="login-error" role="alert">{{error}}</p>
          <button class="login-submit" :disabled="loading" type="submit"><span>{{loading?'正在登录…':'登录工作台'}}</span><ArrowRight v-if="!loading" :size="18"/><span v-else class="login-spinner"></span></button>
        </form>
        <p class="login-help"><ShieldCheck :size="14"/>账号由管理员统一分配，如需帮助请联系管理员。</p>
      </div>
      <button class="login-motion-control" :aria-pressed="motionEnabled" @click="motionEnabled=!motionEnabled">{{motionEnabled?'暂停背景动效':'开启背景动效'}}</button>
      <footer class="login-access-footer"><span>Mouse MES · 制造执行管理系统</span><span>连接生产，协同未来</span></footer>
    </section>
  </div>
</template>
<style scoped>
.login-page{min-height:100svh;display:grid;grid-template-columns:1.12fr 1fr;background:#faf9f6;color:#37352f;font-family:'Inter','Segoe UI','Microsoft YaHei',sans-serif;color-scheme:light}
.login-story{position:relative;isolation:isolate;overflow:hidden;min-height:100svh;padding:40px 9%;display:flex;flex-direction:column;background:#f1efe8;border-right:1px solid #e7e3da}
.login-particle-stage{position:absolute;inset:0;z-index:-1;pointer-events:none}
.login-brand{display:flex;align-items:center;gap:12px;width:fit-content;font-size:16px;font-weight:600;letter-spacing:.04em}
.login-brand-icon{display:grid;place-items:center;width:38px;height:38px;background:#b77945;color:#fff;border-radius:7px}
.login-brand small{display:block;font-size:10px;font-weight:400;color:#91897d;margin-top:4px;letter-spacing:.08em}
.story-copy{position:relative;margin:65px 0 0;max-width:480px}
.story-eyebrow{display:flex;align-items:center;gap:10px;font-size:9px;color:#9c8c72;letter-spacing:.16em;margin-bottom:20px}
.story-eyebrow span{width:20px;height:1px;background:#b59a72}
.story-copy h1{font-size:clamp(28px,3vw,45px);line-height:1.6;font-weight:550;letter-spacing:-.04em;color:#454139}
.story-copy h1>span{color:#b77945}
.story-copy p{font-size:12px;line-height:2.1;color:#898174;margin-top:20px}
.login-workflow{display:flex;gap:9px;flex-wrap:wrap;position:relative;margin:auto 0 26px;padding-top:250px}
.login-workflow span{padding:9px 12px;border:1px solid #ded8cb;border-radius:6px;background:#faf9f1e8;font-size:10px;color:#837763}
.story-footer{display:flex;justify-content:space-between;gap:16px;font-size:9px;color:#a49b8d;position:relative}
.login-access{position:relative;display:flex;flex-direction:column;padding:40px 10% 28px;min-width:0}
.login-top-note{display:flex;justify-content:flex-end;align-items:center;gap:7px;color:#979187;font-size:10px}
.login-demo{padding:4px 8px;background:#faf0d9;color:#9d8047;border-radius:4px;margin-left:8px}
.login-card{width:100%;max-width:380px;margin:auto;align-self:center;padding:42px 0}
.login-greeting{display:flex;align-items:center;gap:7px;font-size:9px;letter-spacing:.14em;color:#9b9487;margin-bottom:18px}
.login-status-light{width:5px;height:5px;border-radius:50%;background:#94a38b}
.login-card h2{font-size:32px;font-weight:600;letter-spacing:-.04em;margin-bottom:12px;color:#37352f}
.login-description{font-size:12px;color:#969086;line-height:1.8}
.login-fields{margin-top:34px}
.login-fields label{display:block;font-size:12px;font-weight:500;color:#625c52;margin-bottom:10px}
.login-input{display:flex;align-items:center;gap:11px;border:1px solid #e1ddd4;border-radius:6px;background:#fff;height:48px;padding:0 14px;color:#aaa194;transition:border-color .2s,box-shadow .2s}
.login-input:focus-within{border-color:#a89a82;box-shadow:0 0 0 3px #a89a8214}
.login-input input{width:100%;min-width:0;height:100%;background:transparent;border:0;outline:none;box-shadow:none;font-size:12px;color:#49433a}
.login-input input::placeholder{color:#b1aa9f}
.login-input button{padding:5px;color:#a3998b;border-radius:4px}
.login-input button:hover{color:#625544;background:#f5f2eb}
.password-label{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-top:24px}
.password-label span{font-size:9px;color:#aaa194;margin-bottom:10px}
.login-submit{margin-top:28px;display:flex;align-items:center;justify-content:space-between;width:100%;height:48px;padding:0 16px;border:1px solid #45433e;border-radius:6px;background:#45433e;color:#fff;font-size:12px}
.login-submit:hover{background:#302e29}.login-submit:disabled{opacity:.6}
.login-help{display:flex;align-items:flex-start;gap:6px;margin-top:24px;padding-top:20px;border-top:1px solid #e9e5dc;font-size:10px;line-height:1.8;color:#a39a8c}
.login-help svg{flex-shrink:0;margin-top:2px}
.login-error{margin-top:15px;padding:10px;border:1px solid #efd5cf;background:#f8eae7;color:#a96057;border-radius:5px;font-size:11px}
.login-motion-control{align-self:flex-end;padding:6px 10px;margin:0 0 24px;border:1px solid #e4ded2;border-radius:5px;background:transparent;font-size:10px;color:#a09685}
.login-motion-control:hover{background:#efebe2;color:#736753}
.login-access-footer{display:flex;justify-content:space-between;gap:12px;font-size:9px;color:#b0a89c}
.login-spinner{width:15px;height:15px;border:2px solid #ffffff50;border-top-color:#fff;border-radius:50%;animation:rotate .7s linear infinite}
.notion-login :focus-visible{outline:2px solid #a89a82;outline-offset:3px}
@keyframes rotate{to{transform:rotate(360deg)}}
@media(min-width:1700px){.login-story{padding:50px 11%}.story-copy{margin-top:85px}}
@media(max-width:1000px){.login-story{padding:32px 7%}.login-access{padding:32px 8%}.story-copy h1{font-size:31px}.story-copy{margin-top:65px}.login-workflow{gap:6px}.login-workflow span{padding:8px;font-size:9px}}
@media(max-width:760px){.login-page{grid-template-columns:1fr}.login-story{min-height:430px;padding:24px;border-right:0;border-bottom:1px solid #e7e3da}.story-copy{margin-top:26px}.story-copy h1{font-size:27px}.story-copy p{font-size:11px;margin-top:12px}.story-eyebrow{font-size:8px;margin-bottom:12px}.story-footer{display:none}.login-workflow{padding-top:110px;margin-bottom:0;gap:7px}.login-access{padding:24px;min-height:560px}.login-card{padding:30px 0}.login-card h2{font-size:27px}.login-fields{margin-top:26px}.login-access-footer{font-size:8px}.login-motion-control{margin-bottom:20px}}
</style>
