<template>
  <div class="page-wrapper dark">
    <div class="glow-wrapper">
      <div class="container">
        <div class="content">
           <!-- 返回主页按钮 -->
           <div style="position: absolute; top: 15px; left: 20px; z-index: 10;">
             <span class="sys-status" style="font-size:28px;color:#00d4ff;font-family:monospace;cursor:pointer;letter-spacing:1px;" @click="$router.push('/')">←</span>
           </div>

           <div class="profile-section">
              <img class="avatar-large" src="https://img.yzcdn.cn/vant/cat.jpeg">
              <span class="nickname">{{nickname}}</span>
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

const router = useRouter()
const nickname = ref('加载中...')
const userId = ref('---')

onMounted(() => {
  const storedUserId = localStorage.getItem('music_userId')
  const storedNickname = localStorage.getItem('user_nickname')
  
  userId.value = storedUserId || '---'
  nickname.value = storedNickname || '网易云用户'
})

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
