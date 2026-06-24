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
          <!-- 头部 -->
          <div class="header">
            <div class="logo-section">
              <span class="back-btn" @click="$router.push('/')">←</span>
              <span class="logo-text">RANK</span>
            </div>
            <div class="header-right">
              <div class="theme-toggle">
                <div class="toggle-btn" :class="{ active: theme === 'dark' }" @click="toggleTheme('dark')">DARK</div>
                <div class="toggle-btn" :class="{ active: theme === 'light' }" @click="toggleTheme('light')">LIGHT</div>
              </div>
            </div>
          </div>

          <div class="divider"></div>

          <!-- 榜单切换 -->
          <div class="rank-tabs">
            <div class="tab-item" :class="{ active: currentTab === 'hot' }" @click="switchTab('hot')">
              <span class="tab-text">热歌榜</span>
            </div>
            <div class="tab-item" :class="{ active: currentTab === 'new' }" @click="switchTab('new')">
              <span class="tab-text">新歌榜</span>
            </div>
            <div class="tab-item" :class="{ active: currentTab === 'rising' }" @click="switchTab('rising')">
              <span class="tab-text">飙升榜</span>
            </div>
          </div>

          <div class="divider"></div>

          <!-- 榜单说明 -->
          <div class="rank-info">
            <div class="info-left">
              <div class="dot green-dot"></div>
              <span>{{ tabDescriptions[currentTab] }}</span>
            </div>
            <span class="info-right">REAL-TIME</span>
          </div>

          <div class="divider"></div>

          <!-- 加载状态 -->
          <div v-if="isLoading" class="loading-section">
            <div class="typing-indicator">
              <div class="typing-dot"></div>
              <div class="typing-dot"></div>
              <div class="typing-dot"></div>
            </div>
            <span class="loading-text">LOADING RANK DATA...</span>
          </div>

          <!-- 榜单列表 -->
          <div v-else class="scroll-view rank-list">
            <div v-if="rankList.length === 0" class="empty-hint">
              暂无排行数据
            </div>
            <div
              v-for="(item, index) in rankList"
              :key="item.songId"
              class="rank-item"
              @click="playSong(item)"
            >
              <div class="rank-number" :class="getRankClass(index + 1)">
                <span v-if="index < 3" class="rank-icon">★</span>
                <span v-else>{{ index + 1 }}</span>
              </div>
              <div class="song-info">
                <div class="song-name">{{ item.songName }}</div>
                <div class="song-artist">{{ item.artist }}</div>
              </div>
              <div class="play-count">
                <span class="count-number">{{ formatPlayCount(item.playCount) }}</span>
                <span class="count-label">PLAYS</span>
              </div>
              <div class="play-btn">▷</div>
            </div>
          </div>

          <!-- 底部状态 -->
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
import { useTheme } from '@/composables/useTheme'
import request from '@/utils/request'

const router = useRouter()
const { theme, toggleTheme } = useTheme()

const currentTab = ref('hot')
const rankList = ref([])
const isLoading = ref(false)
const toastMessage = ref('')

const tabDescriptions = {
  hot: '全站用户最喜爱歌曲',
  new: '近期热门新歌',
  rising: '播放量快速增长'
}

let toastTimer = null

const showToast = (msg) => {
  toastMessage.value = msg
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastMessage.value = ''
  }, 2500)
}

const getRankClass = (rank) => {
  if (rank === 1) return 'rank-1'
  if (rank === 2) return 'rank-2'
  if (rank === 3) return 'rank-3'
  return ''
}

const formatPlayCount = (count) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + 'W'
  }
  return count
}

const loadRankData = async (tab) => {
  isLoading.value = true
  try {
    const response = await request.get('/api/rank/list', {
      params: { type: tab }
    })
    if (response.data && Array.isArray(response.data)) {
      rankList.value = response.data
    } else {
      rankList.value = []
    }
  } catch (error) {
    console.error('加载排行榜失败:', error)
    showToast('加载排行榜失败')
    rankList.value = []
  } finally {
    isLoading.value = false
  }
}

const switchTab = (tab) => {
  currentTab.value = tab
  loadRankData(tab)
}

const playSong = (item) => {
  // 将歌曲信息存储到 sessionStorage，供主页播放器读取
  const songData = {
    id: item.songId,
    title: item.songName,
    artist: item.artist
  }
  sessionStorage.setItem('play_song_from_rank', JSON.stringify(songData))
  showToast('已添加到播放队列')

  // 跳转回主页
  setTimeout(() => {
    router.push('/')
  }, 800)
}

onMounted(() => {
  loadRankData(currentTab.value)
})
</script>

<style scoped src="./Rank.css"></style>
