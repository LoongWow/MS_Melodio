<template>
  <div class="page-wrapper">
    <div class="glow-wrapper">
      <div class="container" :class="{'register-mode': mode === 'register'}">
        <div class="content login-content">
          <!-- 返回主页按钮 -->
          <div style="position: absolute; top: 15px; left: 20px; z-index: 10;">
            <span class="sys-status" style="font-size:28px;color:#00d4ff;font-family:monospace;cursor:pointer;letter-spacing:1px;" @click="$router.push('/')">←</span>
          </div>
          
          <!-- 头部标识 -->
          <div class="header">
            <div class="logo-section">
              <div class="avatar" >
                <img class="avatar" src="https://img.yzcdn.cn/vant/cat.jpeg">
              </div>
              <span class="logo-text">Melodio</span>
            </div>
            <div class="header-right">
              <span class="sys-status">状态: {{mode === 'register' ? '注册中' : '认证登录中'}}</span>
            </div>
          </div>

          <div class="divider"></div>

          <!-- 登录方式切换 -->
          <div class="method-tabs">
            <div class="tab" :class="{active: loginMethod === 'qrcode'}" 
                  @click="switchLoginMethod('qrcode')">
              [ 扫码登录 ]
            </div>
            <div class="tab" :class="{active: loginMethod === 'manual'}" 
                  @click="switchLoginMethod('manual')">
              [ 账号密码 ]
            </div>
          </div>

          <div class="divider"></div>

          <!-- 手动输入表单 -->
          <div v-if="loginMethod === 'manual'" class="auth-form">
            <div class="input-group">
              <span class="label">账号</span>
              <div class="input-wrapper">
                <input class="sys-input" placeholder="输入账号..." v-model="account" />
              </div>
            </div>

            <div class="input-group">
              <span class="label">密码</span>
              <div class="input-wrapper">
                <input class="sys-input" placeholder="输入密码..." type="password" v-model="password" />
              </div>
            </div>

            <div class="input-group">
              <span class="label">COOKIE 数据</span>
              <div class="input-wrapper">
                <textarea class="sys-textarea" placeholder="MUSIC_U=..." v-model="cookie"></textarea>
              </div>
            </div>

            <div class="action-group">
              <button class="sys-btn primary" @click="submit" :disabled="loading">
                {{mode === 'register' ? (loading ? '注册中...' : '注册') : (loading ? '登录中...' : '登录')}}
              </button>
            </div>
          </div>

          <!-- 二维码登录表单 -->
          <div v-if="loginMethod === 'qrcode'" class="auth-form">
            <div class="qr-header">
              <span class="label">等待扫码操作</span>
            </div>

            <div class="qr-container">
              <div v-if="!qrCodeImg" class="qr-loading">
                <span class="loading-text">{{ loading ? '正在生成二维码...' : '等待生成二维码' }}</span>
              </div>
              
              <img v-if="qrCodeImg" class="qr-image" :src="qrCodeImg" />
              
              <div v-if="qrCodeImg && qrCodeExpired" class="qr-overlay">
                <span class="expired-text">二维码已过期</span>
                <button class="sys-btn small" @click="generateQrCode">刷新</button>
              </div>
            </div>

            <div class="status-panel">
              <span class="status-indicator">♪♪</span>
              <span class="status-msg">{{qrCodeStatus || '等待初始化...'}}</span>
              <span class="status-indicator">♪♪</span>
            </div>

            <div class="action-group">
              <button class="sys-btn primary" @click="generateQrCode" :disabled="loading">
                {{qrCodeImg ? '刷新二维码' : '获取二维码'}}
              </button>
            </div>
          </div>

          <!-- 底部模式切换 -->
          <div class="footer-action" v-if="loginMethod === 'manual'">
            <button class="sys-btn secondary" @click="switchMode">
              {{mode === 'register' ? '我要登录' : '没有账号？点击注册'}}
            </button>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()

const loginMethod = ref('qrcode')
const account = ref('')
const password = ref('')
const cookie = ref('')
const qrCodeKey = ref('')
const qrCodeImg = ref('')
const qrCodeStatus = ref('请生成二维码开始登录')
const qrCodeExpired = ref(false)
let qrCodePolling = null

const loading = ref(false)
const mode = ref('login')

let cookieCheckInterval = null
const cookieCheckIntervalMs = 30 * 60 * 1000

const switchLoginMethod = (method) => {
  loginMethod.value = method
  loading.value = false
  if (method === 'qrcode') {
    mode.value = 'login'
  }
}

const switchMode = () => {
  mode.value = mode.value === 'login' ? 'register' : 'login'
}

const submit = async () => {
  if (!cookie.value.trim()) {
    alert('请先填写 Cookie')
    return
  }
  loading.value = true

  try {
    const res = await request.post(`/api/user/${mode.value === 'register' ? 'register' : 'login'}`, {
      account: account.value,
      password: password.value,
      cookie: cookie.value
    })
    loading.value = false
    const userId = res.data?.userId
    const nickname = res.data?.nickname
    if (userId) {
      localStorage.setItem('music_userId', userId)
      if (nickname) {
        localStorage.setItem('user_nickname', nickname)
      }
      localStorage.setItem('netease_cookie', cookie.value)
      localStorage.setItem('netease_cookie_timestamp', Date.now())

      alert(mode.value === 'register' ? '注册成功' : '登录成功')
      setTimeout(() => {
        router.back()
      }, 500)
    } else {
      alert((mode.value === 'register' ? '注册失败: ' : '登录失败: ') + (res.data?.message || 'Cookie 验证失败'))
    }
  } catch (err) {
    loading.value = false
    // interceptor 已经 alert 了具体信息，这里不强求，但可以保留
  }
}

const checkCookieStatus = async () => {
  const storedCookie = localStorage.getItem('netease_cookie')
  if (!storedCookie) return

  try {
    const res = await request.get(`/api/user/cookie-status`, { params: { cookie: storedCookie } })
    if (res.data.valid) {
      localStorage.setItem('netease_cookie_last_check', Date.now())
    } else {
      handleCookieExpired()
    }
  } catch (error) {
    console.error('Cookie 状态检查失败:', error)
  }
}

const handleCookieExpired = () => {
  localStorage.removeItem('netease_cookie')
  localStorage.removeItem('netease_cookie_timestamp')
  if (confirm('Cookie 已失效，请重新登录网易云音乐获取新的 Cookie。是否去登录？')) {
    loginMethod.value = 'qrcode'
  }
}

const startCookieAutoCheck = () => {
  stopCookieAutoCheck()
  checkCookieStatus()
  cookieCheckInterval = setInterval(checkCookieStatus, cookieCheckIntervalMs)
}

const stopCookieAutoCheck = () => {
  if (cookieCheckInterval) {
    clearInterval(cookieCheckInterval)
    cookieCheckInterval = null
  }
}

const stopQrCodePolling = () => {
  if (qrCodePolling) {
    clearInterval(qrCodePolling)
    qrCodePolling = null
  }
}

const handleQrLoginSuccess = async (scannedCookie) => {
  if (!scannedCookie) {
    alert('登录失败: 未获取到 Cookie')
    return
  }

  try {
    const res = await request.post(`/api/user/qr-login`, null, { 
      params: { cookie: scannedCookie }
    })
    
    if (res.status === 200 && res.data.userId) {
      const { userId, nickname } = res.data
      localStorage.setItem('netease_cookie', scannedCookie)
      localStorage.setItem('netease_cookie_timestamp', Date.now())
      localStorage.setItem('music_userId', userId)
      localStorage.setItem('user_nickname', nickname)
      
      qrCodeStatus.value = '✅ 登录成功'
      alert('登录成功')
      setTimeout(() => {
        router.back()
      }, 1000)
    } else {
      alert('登录失败: ' + (res.data.message || '请重试'))
    }
  } catch (error) {
    console.error('二维码登录失败:', error)
    alert('登录失败: 网络错误，请重试')
  }
}

const startQrCodePolling = () => {
  stopQrCodePolling()

  const checkStatus = async () => {
    try {
      const res = await request.get(`/api/user/qr-check`, { params: { key: qrCodeKey.value } })
      const { code, message, cookie: resCookie } = res.data

      switch (code) {
        case 800:
          stopQrCodePolling()
          qrCodeStatus.value = '二维码已过期'
          qrCodeExpired.value = true
          break
        case 801:
          qrCodeStatus.value = '等待扫码中...'
          break
        case 802:
          qrCodeStatus.value = '✅ 已扫码，请在手机上确认'
          break
        case 803:
          stopQrCodePolling()
          handleQrLoginSuccess(resCookie)
          break
        default:
          console.warn('未知状态码:', code, message)
      }
    } catch (error) {
      console.error('检查二维码状态失败:', error)
    }
  }

  checkStatus()
  qrCodePolling = setInterval(checkStatus, 2000)
}

const generateQrCode = async () => {
  stopQrCodePolling()
  loading.value = true
  qrCodeImg.value = ''
  qrCodeExpired.value = false
  qrCodeStatus.value = '正在生成二维码...'

  try {
    const keyRes = await request.get(`/api/user/qr-key`, { params: { timestamp: Date.now() } })
    if (!keyRes.data.success) throw new Error(keyRes.data.errorMessage || '获取二维码 Key 失败')
    
    const qrKey = keyRes.data.unikey
    const imgRes = await request.get(`/api/user/qr-create`, { params: { key: qrKey, timestamp: Date.now() } })
    if (!imgRes.data.success) throw new Error(imgRes.data.errorMessage || '生成二维码失败')

    qrCodeKey.value = qrKey
    qrCodeImg.value = imgRes.data.qrimg
    qrCodeStatus.value = '请使用网易云音乐 APP 扫码'

    startQrCodePolling()
  } catch (error) {
    console.error('生成二维码失败:', error)
    alert('生成失败: ' + (error.message || '网络错误'))
    qrCodeStatus.value = '生成失败，请重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  startCookieAutoCheck()
})

onUnmounted(() => {
  stopCookieAutoCheck()
  stopQrCodePolling()
})
</script>

<style scoped src="./Login.css"></style>
