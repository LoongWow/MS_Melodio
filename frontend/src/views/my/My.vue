<template>
  <div class="page-wrapper" :class="theme">
    <transition name="toast-fade">
      <div v-if="toastMessage" class="toast-popup">
        {{ toastMessage }}
      </div>
    </transition>
    <div class="glow-wrapper">
      <div class="container">
        <div class="content">
           <!-- 返回主页按钮 -->
           <div style="position: absolute; top: 15px; left: 20px; z-index: 10;">
             <span class="sys-status" style="font-size:28px;color:#00d4ff;font-family:monospace;cursor:pointer;letter-spacing:1px;" @click="$router.push('/')">←</span>
           </div>

           <div class="profile-section">
              <img class="avatar-large" src="https://img.yzcdn.cn/vant/cat.jpeg">
              
              <div v-if="!isEditingNickname" class="nickname-container">
                <span class="nickname">{{nickname}}</span>
                <span class="edit-icon" @click="startEditNickname" title="修改名称">✎</span>
              </div>
              <div v-else class="nickname-edit-container">
                <input type="text" v-model="editNicknameValue" class="nickname-input" placeholder="输入新昵称" @keyup.enter="confirmEditNickname" />
                <span class="confirm-icon" @click="confirmEditNickname" title="确认">✔</span>
                <span class="cancel-icon" @click="cancelEditNickname" title="取消">✖</span>
              </div>

              <span class="user-id">用户 ID: {{userId}}</span>
           </div>

           <div class="action-section">
              <div class="action-btn" @click="handleUpdateCookies">
                 <span>更新 COOKIES</span>
              </div>
              <div class="action-btn logout-btn" @click="handleLogout">
                 <span>退出登录</span>
              </div>
           </div>

           <div class="footer">
             <span>MELODIO FM.</span>
             <span>CONNECTED.</span>
           </div>
           
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { useTheme } from '@/composables/useTheme'

const router = useRouter()
const nickname = ref('加载中...')
const userId = ref('---')
const { theme } = useTheme()

const isEditingNickname = ref(false)
const editNicknameValue = ref('')

const toastMessage = ref('')
let toastTimer = null

const showToast = (msg) => {
  toastMessage.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastMessage.value = ''
  }, 2500)
}

onMounted(() => {
  const storedUserId = localStorage.getItem('music_userId')
  const storedNickname = localStorage.getItem('user_nickname')
  
  userId.value = storedUserId || '---'
  nickname.value = storedNickname || '网易云用户'
})

const startEditNickname = () => {
  editNicknameValue.value = nickname.value
  isEditingNickname.value = true
}

const cancelEditNickname = () => {
  isEditingNickname.value = false
}

const confirmEditNickname = async () => {
  const newNickname = editNicknameValue.value.trim()
  if (!newNickname) {
    showToast('昵称不能为空')
    return
  }
  try {
    const res = await request.post('/api/user/update-nickname', null, {
      params: {
        userId: Number(userId.value),
        nickname: newNickname
      }
    })
    if (res.status === 200) {
      nickname.value = newNickname
      localStorage.setItem('user_nickname', newNickname)
      isEditingNickname.value = false
      showToast('修改昵称成功！')
    }
  } catch (error) {
    console.error('更新昵称失败:', error)
    showToast('更新昵称失败')
  }
}

const handleUpdateCookies = () => {
  if (confirm('是否前往登录页重新扫码或账号登录以更新 Cookies？')) {
    router.push('/login')
  }
}

const handleLogout = () => {
  if (confirm('确定要退出当前账号吗？')) {
    localStorage.removeItem('music_userId')
    localStorage.removeItem('user_nickname')
    localStorage.removeItem('netease_cookie')
    localStorage.removeItem('netease_cookie_timestamp')
    localStorage.removeItem('genre_cache')
    
    alert('已退出登录')
    
    setTimeout(() => {
      window.location.href = '/'
    }, 1000)
  }
}
</script>

<style scoped src="./My.css"></style>
