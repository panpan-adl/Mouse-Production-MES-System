<script setup lang="ts">
import FaceSuccessNotice from "@/components/FaceSuccessNotice.vue"
import FaceCameraGuide from "@/components/FaceCameraGuide.vue"
import { nextTick, onBeforeUnmount, ref } from 'vue'
import { captureFaceFrames } from '@/utils/faceCapture'
import request, { RequestError } from '@/utils/request'
import LoginParticleCloud from '@/components/LoginParticleCloud.vue'
import LoginPromo from '@/components/LoginPromo.vue'
import { motionEnabled } from '@/stores/layout'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ArrowRight, Eye, EyeOff, LockKeyhole, UserRound, Factory, ScanFace, ShieldCheck } from 'lucide-vue-next'
const auth=useAuthStore(),router=useRouter(),route=useRoute()
const userCode=ref(''),password=ref(''),showPassword=ref(false),loading=ref(false),error=ref('')
const demo=import.meta.env.VITE_DEMO_MODE==='1'
const resetMode = ref(false)
const credential = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const resetNotice = ref('')

function toggleReset() {
  resetMode.value = !resetMode.value
  error.value = ''
  resetNotice.value = ''
  password.value = credential.value = newPassword.value = confirmPassword.value = ''
}

async function resetPassword() {
  if (loading.value) return
  error.value = ''
  if (newPassword.value !== confirmPassword.value) {
    error.value = '两次输入的新密码不一致'
    return
  }
  loading.value = true
  try {
    await request.post('/auth/password-reset', {
      userCode: userCode.value.trim(),
      credential: credential.value.trim(),
      newPassword: newPassword.value,
    })
    toggleReset()
    resetNotice.value = '密码已修改，请使用新密码登录。'
  } catch (cause) {
    error.value = loginErrorMessage(cause, 'reset')
  } finally {
    loading.value = false
  }
}

const faceEnabled = import.meta.env.VITE_FACE_LOGIN === '1'
const camera = ref<HTMLVideoElement>()
const cameraOpen = ref(false)
const faceVerified = ref(false)
const faceDialog = ref<HTMLDialogElement>()
const faceStatus = ref('')
const faceFailure = ref('')
let stream: MediaStream | undefined
let faceAttempt: AbortController | undefined

function closeCamera() {
  stream?.getTracks().forEach(track => track.stop())
  stream = undefined
  cameraOpen.value = false
}

function closeFaceDialog() {
  faceAttempt?.abort()
  closeCamera()
  faceDialog.value?.close()
}

onBeforeUnmount(closeFaceDialog)

async function faceLogin() {
  if (loading.value) return
  loading.value = true
  error.value = ''
  faceFailure.value = ''
  faceStatus.value = '正在准备人脸登录…'
  const attempt = new AbortController()
  faceAttempt = attempt
  try {
    const challenge = await request.post<string>('/auth/face/challenge', undefined, { signal: attempt.signal })
    if (attempt.signal.aborted) return
    if (!navigator.mediaDevices?.getUserMedia) {
      throw new Error('当前浏览器无法访问摄像头，请使用密码登录。')
    }
    if (faceDialog.value && !faceDialog.value.open) faceDialog.value.showModal()
    faceStatus.value = '正在打开摄像头，请允许访问。'
    const media = await navigator.mediaDevices.getUserMedia({ video: { width: 640, height: 480 }, audio: false })
    if (attempt.signal.aborted) {
      media.getTracks().forEach(track => track.stop())
      return
    }
    stream = media
    cameraOpen.value = true
    await nextTick()
    if (attempt.signal.aborted || !camera.value) return
    const video = camera.value
    video.srcObject = media
    await video.play()
    if (attempt.signal.aborted) return
    faceStatus.value = '请保持正对镜头，正在采集…'
    const frames = await captureFaceFrames(video, () => attempt.signal.aborted)
    if (attempt.signal.aborted) return
    faceStatus.value = '正在验证人脸…'
    await auth.loginWithFace(challenge, frames, attempt.signal)
    if (attempt.signal.aborted) return
    faceDialog.value?.close()
    faceVerified.value = true
  } catch (cause) {
    if (attempt.signal.aborted) return
    const message = loginErrorMessage(cause, 'face')
    if (faceDialog.value?.open) faceFailure.value = message
    else error.value = message
  } finally {
    closeCamera()
    faceAttempt = undefined
    loading.value = false
  }
}

function loginErrorMessage(cause: unknown, action: 'face' | 'reset') {
  const label = action === 'face' ? '人脸登录' : '密码重置'
  const alternative = action === 'face' ? '请使用密码登录。' : '请稍后重试或联系管理员。'
  if (cause instanceof RequestError) {
    if (cause.status === 404) return label + '暂不可用，' + alternative
    if (cause.transportCode === 'ECONNABORTED' || cause.transportCode === 'ETIMEDOUT') {
      return label + '请求超时，请稍后重试。'
    }
    if (!cause.status || cause.status >= 500) return label + '暂时无法连接服务，' + alternative
  }
  if (action === 'face' && cause instanceof DOMException) {
    if (cause.name === 'NotAllowedError' || cause.name === 'SecurityError') {
      return '未获得摄像头权限，请在浏览器中允许访问摄像头后重试。'
    }
    if (cause.name === 'NotFoundError') return '未找到摄像头，请连接摄像头或使用密码登录。'
    if (cause.name === 'NotReadableError' || cause.name === 'AbortError') {
      return '无法打开摄像头，请关闭其他正在使用摄像头的程序后重试。'
    }
  }
  if (cause instanceof Error && /^[\u3400-\u9fff]/.test(cause.message)) return cause.message
  return label + '失败，' + alternative
}

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
      <div class="story-copy"><div class="story-eyebrow"><span></span> YOUR EVERYDAY WORKSPACE</div><h1 class="story-art-title"><span class="title-line">让生产<span class="title-emphasis">井然有序</span>，</span><span class="title-line">让协作<span class="title-emphasis">轻松发生</span><span class="title-period">。</span></span></h1><p>从一份生产计划，到每一道工序的执行。<br/>连接人、设备与物料，构建有序、高效的生产现场。</p></div>
      <LoginPromo :motion-enabled="motionEnabled" />
      <div class="login-workflow"><span>01 · 生产计划</span><span>02 · 工序执行</span><span>03 · 质量追溯</span></div>
      <div class="story-footer"><span>为每一次制造，建立可靠连接。</span><span>MOUSE / MES</span></div>
    </section>
    <section class="login-access">
      <div class="login-top-note"><ShieldCheck :size="15"/><span>企业工作空间</span><span v-if="demo" class="login-demo">演示环境</span></div>
      <div class="login-card"><div class="login-greeting"><span class="login-status-light"></span> ENTER YOUR WORKSPACE</div><h2>欢迎回来</h2><p class="login-description">登录您的账号，开始今天的生产协作。</p>
        <p v-if="route.query.passwordChanged==='1'" role="status" class="text-sm text-green-700 bg-green-50 rounded-lg p-3 mb-4">密码已修改，请使用新密码重新登录。</p>
        <p v-if="resetNotice" role="status">{{ resetNotice }}</p>
        <form v-if="!resetMode" @submit.prevent="login" class="login-fields">
          <label for="login-user-code">员工工号</label><div class="login-input"><UserRound :size="18"/><input id="login-user-code" v-model="userCode" autocomplete="username" inputmode="numeric" maxlength="6" placeholder="请输入6位员工工号" required /></div>
          <div class="password-label"><label for="login-password">登录密码</label><span>使用您的企业账号密码</span></div><div class="login-input"><LockKeyhole :size="18"/><input id="login-password" v-model="password" :type="showPassword?'text':'password'" autocomplete="current-password" placeholder="请输入登录密码" required /><button type="button" :aria-label="showPassword?'隐藏密码':'显示密码'" @click="showPassword=!showPassword"><EyeOff v-if="showPassword" :size="17"/><Eye v-else :size="17"/></button></div>
          <p v-if="error" class="login-error" role="alert">{{error}}</p>
          <button class="login-submit" :disabled="loading" type="submit"><span>{{loading?'正在登录…':'登录工作台'}}</span><ArrowRight v-if="!loading" :size="18"/><span v-else class="login-spinner"></span></button>
        </form>
        <form v-else class="login-fields" @submit.prevent="resetPassword">
          <label for="reset-user">员工工号</label>
          <div class="login-input"><input id="reset-user" v-model="userCode" autocomplete="username" pattern="[0-9]{6}" required /></div>
          <label for="reset-credential">管理员发放的临时凭据</label>
          <div class="login-input"><input id="reset-credential" v-model="credential" type="password" autocomplete="off" required /></div>
          <label for="reset-password">新密码</label>
          <div class="login-input"><input id="reset-password" v-model="newPassword" type="password" autocomplete="new-password" minlength="6" required /></div>
          <label for="reset-confirm">确认新密码</label>
          <div class="login-input"><input id="reset-confirm" v-model="confirmPassword" type="password" autocomplete="new-password" minlength="6" required /></div>
          <p v-if="error" class="login-error" role="alert">{{ error }}</p>
          <button class="login-submit" :disabled="loading" type="submit">设置新密码</button>
        </form>
        <div class="login-alternatives">
          <button v-if="faceEnabled && !resetMode" type="button" class="login-face-button"
            :disabled="loading" @click="faceLogin"><ScanFace :size="18" aria-hidden="true" />人脸登录</button>
          <button type="button" :disabled="loading" class="login-reset-link" @click="toggleReset">
            {{ resetMode ? '返回登录' : '使用临时凭据重置密码' }}
          </button>
        </div>
        <Teleport to="body">
          <dialog ref="faceDialog" class="face-login-dialog" aria-labelledby="face-login-title"
            @cancel.prevent="closeFaceDialog">
            <h2 id="face-login-title">人脸登录</h2>
            <FaceCameraGuide v-if="cameraOpen">
              <video ref="camera" autoplay muted playsinline aria-label="摄像头预览" />
            </FaceCameraGuide>
            <p v-if="faceFailure" class="face-login-error" role="alert">{{ faceFailure }}</p>
            <p v-else role="status">{{ faceStatus }}</p>
            <div class="face-login-actions">
              <button type="button" autofocus @click="closeFaceDialog">关闭</button>
              <button v-if="faceFailure" type="button" :disabled="loading" @click="faceLogin">重新采集</button>
            </div>
          </dialog>
        </Teleport>
        <FaceSuccessNotice v-if="faceVerified" title="人脸验证通过" @done="router.replace(auth.redirectPath)">
          <p>{{ auth.currentUser?.realName }} · 工号 {{ auth.currentUser?.userCode }}</p>
          <p>三秒后进入工作台</p>
        </FaceSuccessNotice>
        <p class="login-help"><ShieldCheck :size="14"/>账号由管理员统一分配，如需帮助请联系管理员。</p>
      </div>
      <button class="login-motion-control" :aria-pressed="motionEnabled" @click="motionEnabled=!motionEnabled">{{motionEnabled?'暂停背景动效':'开启背景动效'}}</button>
      <footer class="login-access-footer"><span>Mouse MES · 制造执行管理系统</span><span>连接生产，协同未来</span></footer>
    </section>
  </div>
</template>
<style scoped>
.face-login-dialog {
  box-sizing: border-box;
  position: fixed;
  inset: 0;
  width: min(640px, calc(100% - 32px));
  max-height: calc(100dvh - 32px);
  margin: auto;
  overflow: auto;
  padding: clamp(16px, 4vw, 28px);
  border: 1px solid #e5dfd3;
  border-radius: 16px;
  background: #fffefa;
  color: #3e382f;
  box-shadow: 0 16px 48px #332d2533;
}
.face-login-dialog::backdrop { background: #201b1666; }
.face-login-dialog h2 { font-size: 22px; font-weight: 650; margin: 0 0 12px; }
.face-login-dialog p { font-size: 14px; margin: 8px 0; }
.face-login-error { color: #a0352d; }
.face-login-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 16px; }
.face-login-actions button { padding: 8px 14px; border: 1px solid #d8d0c2; border-radius: 7px; }
.face-login-actions button:last-child:not(:only-child) { background: #766044; color: white; }

.login-page{min-height:100svh;display:grid;grid-template-columns:1.12fr 1fr;background:#faf9f6;color:#37352f;font-family:'Inter','Segoe UI','Microsoft YaHei',sans-serif;color-scheme:light}
.login-story{position:relative;isolation:isolate;overflow:hidden;min-height:100svh;padding:40px 9%;display:flex;flex-direction:column;background:#f1efe8;border-right:1px solid #e7e3da}
.login-particle-stage{position:absolute;inset:0;z-index:-1;pointer-events:none}
.login-brand{display:flex;align-items:center;gap:12px;width:fit-content;font-size:16px;font-weight:600;letter-spacing:.04em}
.login-brand-icon{display:grid;place-items:center;width:38px;height:38px;background:#b77945;color:#fff;border-radius:7px}
.login-brand small{display:block;font-size:10px;font-weight:400;color:#91897d;margin-top:4px;letter-spacing:.08em}
.story-copy{position:relative;margin:65px 0 0;max-width:100%}
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
.login-alternatives{display:grid;gap:10px;margin-top:12px}
.login-face-button{display:flex;align-items:center;justify-content:center;gap:8px;min-height:44px;border:1px solid #d8d0c2;border-radius:7px;background:#fffefa;color:#554b3e;font-size:14px;font-weight:600}
.login-face-button:hover{background:#f5f0e7}
.login-reset-link{justify-self:center;min-height:36px;padding:4px 8px;color:#675740;font-size:12px;text-decoration:underline;text-underline-offset:3px}
.login-alternatives button:disabled{opacity:.6}
.login-motion-control{align-self:flex-end;padding:6px 10px;margin:0 0 24px;border:1px solid #e4ded2;border-radius:5px;background:transparent;font-size:10px;color:#a09685}
.login-motion-control:hover{background:#efebe2;color:#736753}
.login-access-footer{display:flex;justify-content:space-between;gap:12px;font-size:9px;color:#b0a89c}
.login-spinner{width:15px;height:15px;border:2px solid #ffffff50;border-top-color:#fff;border-radius:50%;animation:rotate .7s linear infinite}
.notion-login :focus-visible{outline:2px solid #a89a82;outline-offset:3px}
@keyframes rotate{to{transform:rotate(360deg)}}
/* A stronger focal point for the sign-in form. */
.notion-login .login-card{max-width:450px;padding:38px 32px 30px;background:#fffefa;border:1px solid #e5dfd3;border-radius:14px;box-shadow:0 12px 44px #6351390a;position:relative}
.notion-login .login-card::before{content:'';position:absolute;top:-1px;left:32px;width:48px;height:3px;background:#b77945;border-radius:0 0 3px 3px}
.notion-login .login-greeting{font-size:10px;color:#9b805b;font-weight:600;letter-spacing:.12em;margin-bottom:20px}
.notion-login .login-status-light{width:6px;height:6px;background:#b77945}
.notion-login .login-card h2{font-size:42px;line-height:1.3;font-weight:650;color:#3e382f;margin-bottom:14px;letter-spacing:-.035em}
.notion-login .login-description{font-size:13px;color:#857969}
.notion-login .login-fields label{font-size:13px;color:#554b3e;font-weight:600}
.notion-login .login-input{height:54px;border-color:#d8d0c2;border-radius:7px;color:#9b8669}
.notion-login .login-input input{font-size:14px}
.notion-login .login-input input:focus,.notion-login .login-input input:focus-visible{outline:none;outline-offset:0;border:0;box-shadow:none}
.notion-login .login-input input::placeholder{color:#a69b8b}
.notion-login .login-input:focus-within{border-color:#b77945;box-shadow:0 0 0 3px #b7794512}
.notion-login .login-submit{height:54px;font-size:14px;font-weight:600;background:#494136;border-color:#494136;border-radius:7px;box-shadow:0 4px 10px #49413612}
.notion-login .login-submit:hover{background:#332d25;border-color:#332d25}
.notion-login .login-help{color:#9b8f7d;font-size:10px}
@media(max-width:1000px){.notion-login .login-card{padding:32px 22px}.notion-login .login-card h2{font-size:36px}}
@media(max-width:760px){.notion-login .login-card{max-width:440px;padding:30px 22px}.notion-login .login-card h2{font-size:34px}}
/* Editorial lettering with local Chinese serif fallbacks; no font download needed. */
.story-copy .story-art-title{font-size:clamp(36px,4vw,68px);font-family:'STZhongsong','华文中宋','Source Han Serif SC','Noto Serif CJK SC','Songti SC','SimSun',serif;font-weight:600;line-height:1.65;letter-spacing:.015em;color:#494235;text-shadow:0 1px 0 #fff9}
.story-art-title .title-line{display:block;white-space:nowrap}
.story-art-title .title-emphasis{position:relative;display:inline;color:#8c6944;background:linear-gradient(115deg,#66513b 5%,#a47c4d 55%,#755b40 95%);background-clip:text;-webkit-background-clip:text;-webkit-text-fill-color:transparent}
.story-art-title .title-emphasis::after{content:'';position:absolute;left:2%;right:1%;bottom:-.12em;height:3px;border-top:1px solid #b794635c;border-bottom:1px solid #b794632e;transform:rotate(-1.5deg);pointer-events:none}
.story-art-title .title-period{color:#b77945;-webkit-text-fill-color:#b77945}
@media(max-width:760px){.story-copy .story-art-title{font-size:clamp(25px,6.8vw,44px);letter-spacing:0;line-height:1.75}}
@media(min-width:1700px){.login-story{padding:50px 11%}.story-copy{margin-top:85px}}
@media(min-width:761px) and (max-width:1100px){.story-copy .story-art-title{font-size:clamp(28px,3.6vw,40px)}}
@media(max-width:1000px){.login-story{padding:32px 7%}.login-access{padding:32px 8%}.story-copy h1{font-size:31px}.story-copy{margin-top:65px}.login-workflow{gap:6px}.login-workflow span{padding:8px;font-size:9px}}
@media(max-width:760px){.login-page{grid-template-columns:1fr}.login-story{min-height:430px;padding:24px;border-right:0;border-bottom:1px solid #e7e3da}.story-copy{margin-top:26px}.story-copy h1{font-size:27px}.story-copy p{font-size:11px;margin-top:12px}.story-eyebrow{font-size:8px;margin-bottom:12px}.story-footer{display:none}.login-workflow{padding-top:110px;margin-bottom:0;gap:7px}.login-access{padding:24px;min-height:560px}.login-card{padding:30px 0}.login-card h2{font-size:27px}.login-fields{margin-top:26px}.login-access-footer{font-size:8px}.login-motion-control{margin-bottom:20px}}
</style>
<style scoped>
/* The film takes the place of the former empty particle stage. */
.notion-login{grid-template-columns:minmax(0,1.12fr) minmax(0,1fr)}
.notion-login .login-story{min-width:0}
.notion-login .story-copy{margin-top:32px}
.notion-login .story-copy .story-art-title{font-size:clamp(28px,3vw,48px);line-height:1.55}
.notion-login .story-copy p{margin-top:12px}
.notion-login .story-eyebrow{margin-bottom:12px}
.notion-login .login-workflow{padding-top:0;margin:18px 0}
.notion-login .story-footer{margin-top:auto}
@media(max-width:760px){.notion-login{grid-template-columns:minmax(0,1fr)}.notion-login .story-copy{margin-top:24px}.notion-login .story-copy .story-art-title{font-size:clamp(25px,6.8vw,40px)}.notion-login .login-story{min-height:auto;padding-bottom:24px}}
</style>
