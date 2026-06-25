<template>
  <div class="page-wrapper" :class="theme">
    <div class="glow-wrapper">
      <div :class="containerClass">
        <div class="content" :class="{'typing-mode': isTyping}" :style="{ paddingBottom: keyboardHeight ? (keyboardHeight + 15) + 'px' : '15px' }">
          <!-- 顶部 -->
          <div class="header">
            <div class="logo-section">
              <img class="avatar" src="https://img.yzcdn.cn/vant/cat.jpeg" />
              <span class="logo-text">Melodio</span>
            </div>
            <div class="header-right">
              <span class="login-btn" @click="goToLogin">{{ isLoggedIn ? 'My' : 'LOGIN' }}</span>
              <div class="theme-toggle">
                <div class="toggle-btn" :class="{ active: theme === 'dark' }" @click="toggleTheme('dark')">DARK</div>
                <div class="toggle-btn" :class="{ active: theme === 'light' }" @click="toggleTheme('light')">LIGHT</div>
              </div>
            </div>
          </div>

          <!-- 时钟区域 -->
          <div class="middle-section" :class="{ collapsed: isTyping }">
            <div class="clock-section">
              <canvas id="clockCanvas" class="clock-canvas"></canvas>
              <div class="date">{{ dayOfWeek }}</div>
              <div class="full-date">{{ fullDate }}</div>
              <div class="on-air">
                <div class="dot green-dot"></div>
                <span>ON AIR</span>
              </div>
            </div>

            <div class="divider"></div>

            <!-- Player Section -->
            <div class="player-section">
              <div class="track-info-row">
                <div class="equalizer" :class="{ playing: isPlaying }">
                  <div class="bar bar1"></div>
                  <div class="bar bar2"></div>
                  <div class="bar bar3"></div>
                  <div class="bar bar4"></div>
                </div>

                <div class="track-details" @click="openPlayerView">
                  <div class="marquee-wrap">
                    <span class="track-name marquee-text">{{ currentTrack }}</span>
                  </div>
                  <span class="playing-status">{{ isPlaying ? 'PLAYING' : 'PAUSED' }}</span>
                </div>

                <div class="controls">
                  <div class="icon-btn" @click="playPrev">|◁</div>
                  <div class="icon-btn" @click="togglePlay">{{ isPlaying ? '||' : '▷' }}</div>
                  <div class="icon-btn" @click="playNext">▷|</div>
                  <div class="volume-slider">
                    <span class="vol-text">VOL</span>
                    <input type="range" class="vol-slider" min="0" max="100" v-model="volume" @input="onVolumeChange" />
                  </div>
                </div>
              </div>
              <div class="progress-row">
                <span class="time-current">{{ currentTimeStr }}</span>
                <input type="range" class="progress-slider" min="0" :max="duration" v-model="currentTime" @input="onSeek" />
                <span class="time-total">{{ durationStr }}</span>
              </div>
            </div>

            <div class="divider"></div>

            <!-- Queue Info -->
            <div class="queue-section">
              <span>QUEUE</span>
              <div style="display:flex;align-items:center;">
                <span>{{ queue.length }} TRACKS</span>
                <span class="queue-toggle-btn" @click="toggleQueue">{{ showQueue ? '▲' : '▼' }}</span>
              </div>
            </div>

            <!-- Queue List -->
            <div class="scroll-view queue-list" :style="{ height: queueHeight + 'px', overflowY: 'auto', transition: 'height 0.4s ease' }">
              <div
                class="queue-item"
                :class="{ active: index === currentTrackIndex }"
                v-for="(item, index) in queue"
                :key="item.id"
                @click="tapQueueItem(index)"
              >
                <div class="queue-left">
                  <span class="queue-index">{{ index === currentTrackIndex ? '▶' : index + 1 }}</span>
                  <span class="queue-title">{{ item.title }}</span>
                </div>
                <span class="queue-artist">{{ item.artist }}</span>
                <span class="queue-delete-btn" @click.stop="deleteQueueItem(index)">✕</span>
              </div>
            </div>

            <div class="divider"></div>

            <!-- Live Status -->
            <div class="live-status">
              <div class="live-left">
                <div class="dot green-dot"></div>
                <span>Melodio</span>
              </div>
              <span class="live-right">LIVE</span>
            </div>

            <div class="connection-status">
              <span class="dashed-line">---</span>
              <span class="dashed-line">---</span>
            </div>
          </div>

          <!-- Chat Section -->
          <div class="scroll-view chat-section" ref="chatScrollRef">
            <div v-for="item in messages" :key="item.id" :id="'msg-' + item.id" class="message-group" :class="item.role === 'user' ? 'user-message' : 'agent-message'">
              <span class="msg-author">{{ item.author }}</span>
              <div class="message">
                <img v-if="item.role === 'assistant'" class="msg-avatar ai-avatar" src="https://img.yzcdn.cn/vant/cat.jpeg" />
                <div class="msg-content">
                  <span class="msg-text">{{ item.content }}</span>
                </div>
                <img v-if="item.role === 'user'" class="msg-avatar user-avatar" src="https://img.yzcdn.cn/vant/cat.jpeg" />
              </div>
              <div v-if="item.song" class="now-playing-hint">
                 <span class="dashed-line">-</span> {{ item.content }} <span class="dashed-line">-</span>
              </div>
            </div>

            <div v-if="isAiThinking" id="msg-thinking" class="message-group agent-message">
              <span class="msg-author">MELODIO</span>
              <div class="message">
                <img class="msg-avatar ai-avatar" src="https://img.yzcdn.cn/vant/cat.jpeg" />
                <div class="msg-content thinking-content">
                  <div class="typing-indicator">
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Input Section -->
          <div class="input-section">
            <div class="input-box">
              <input 
                placeholder="Say something to the DJ..." 
                class="placeholder-style" 
                v-model="chatInput"
                @keyup.enter="sendChat"
                @focus="onInputFocus"
                @blur="onInputBlur"
              />
            </div>
            <div class="icon-btn send-btn" @click="sendChat">↑</div>
          </div>
          
          <div class="footer">
            <span>MELODIO FM.</span>
            <span>CONNECTED.</span>
          </div>
          
          <!-- ====== LYRICS DRAWER ====== -->
          <div class="drawer-mask" :class="{ open: showPlayerView }" @click="closePlayerView"></div>
          <div class="lyrics-drawer" :class="{ open: showPlayerView, playing: isPlaying }" @click.stop>
            
            <div class="drawer-header">
              <div class="player-tabs">
                <span class="tab" :class="{ active: playerTab === 0 }" @click="switchPlayerTab(0)">LYRICS</span>
                <span class="tab" :class="{ active: playerTab === 1 }" @click="switchPlayerTab(1)">HISTORY</span>
                <span class="tab" :class="{ active: playerTab === 2 }" @click="switchPlayerTab(2)">RANK</span>
              </div>
              <span v-show="playerTab === 1 && playHistory.length > 0" class="clear-history-btn" @click="clearPlayHistory">清空历史</span>
              <div class="drawer-close-btn" @click="closePlayerView">▼</div>
            </div>

            <div class="player-swiper-container" style="flex:1; overflow:hidden; position:relative;">
              
              <div v-show="playerTab === 0" class="scroll-view lyrics-view" ref="lyricScrollRef">
                <div
                  v-for="(item, index) in lyricLines"
                  :key="item.time"
                  :id="'lyric-' + index"
                  class="lyric-line" :class="index < currentLyricIndex ? 'past' : (index === currentLyricIndex ? 'current' : 'future')"
                  @click="seekToLyric(item.time, index)"
                >
                  <span class="lyric-text">{{ item.text }}</span>
                </div>
                <div v-if="!lyricLines.length" class="lyric-line future">
                  <span class="lyric-text">暂无歌词</span>
                </div>
              </div>

              <div v-show="playerTab === 1" class="scroll-view history-view">
                <div v-if="playHistory.length === 0" class="empty-hint">暂无播放记录</div>
                <div
                  class="queue-item"
                  v-for="(item, index) in playHistory"
                  :key="item.id"
                  @click="playHistoryItem(item)"
                >
                  <div class="queue-left">
                    <span class="queue-index">{{ index + 1 }}</span>
                    <span class="queue-title">{{ item.songName }}</span>
                  </div>
                  <span class="queue-artist">{{ item.artist }}</span>
                </div>
              </div>

              <div v-show="playerTab === 2" class="scroll-view rank-view">
                <div class="rank-hint">
                  <div class="rank-hint-icon">★</div>
                  <div class="rank-hint-title">热门榜单</div>
                  <div class="rank-hint-desc">查看全站用户最喜爱的歌曲</div>
                  <div class="rank-go-btn" @click="goToRank">进入榜单 →</div>
                </div>
              </div>

            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { useCanvasClock } from '../../composables/useCanvasClock'
import { useAudioPlayer } from '../../composables/useAudioPlayer'
import { useChat } from '../../composables/useChat'
import { useLyrics } from '../../composables/useLyrics'
import { useTheme } from '../../composables/useTheme'

const router = useRouter()
const { theme, toggleTheme } = useTheme()
const containerClass = ref('container genre-default')
const isTyping = ref(false)
const keyboardHeight = ref(0)
const isLoggedIn = ref(false)

const timeDigits = ref(['0', '0', '0', '0'])
const dayOfWeek = ref('Monday')
const fullDate = ref('01 JAN 2026')
let timeInterval = null

const isPlaying = ref(false)
const currentTrack = ref('Ready to play')
const volume = ref(80)
const currentTime = ref(0)
const duration = ref(100)
const currentTimeStr = ref('0:00')
const durationStr = ref('0:00')
const queue = ref([])
const showQueue = ref(false)
const queueHeight = ref(0)
const currentTrackIndex = ref(0)
const messages = ref([])
const isAiThinking = ref(false)
const chatInput = ref('')
const showPlayerView = ref(false)
const playerTab = ref(0)
const lyricLines = ref([])
const currentLyricIndex = ref(-1)
const playHistory = ref([])
const lyricScrollRef = ref(null)

const { triggerBeat } = useCanvasClock({ isPlaying, theme, timeDigits })

const { loadLyric, syncLyric } = useLyrics({
  lyricLines, currentLyricIndex, triggerBeat, lyricScrollRef
})

const {
  playNext, playPrev, togglePlay, onVolumeChange, onSeek,
  loadAndPlayTrack, loadAndPlayById, tapQueueItem, deleteQueueItem,
  stop: stopAudio, seekTo: seekToAudio
} = useAudioPlayer({
  isPlaying, currentTrack, volume, currentTime, duration,
  currentTimeStr, durationStr, queue, currentTrackIndex,
  triggerBeat, syncLyric, loadLyric
})

const goToLogin = () => {
  if (isLoggedIn.value) {
    router.push('/my')
  } else {
    router.push('/login')
  }
}

const { chatScrollRef, loadGreeting, sendChat, scrollChatToBottom } = useChat({
  messages, chatInput, isAiThinking, queue, currentTrack,
  currentTrackIndex, showPlayerView, playerTab,
  playNext, playPrev, loadAndPlayTrack, loadAndPlayById, stopAudio,
  goToLogin,
  pauseAudio: () => { isPlaying.value = false; stopAudio() }, // Simplification
  resumeAudio: () => { togglePlay(); return queue.value.length > 0 },
  setVolume: (v) => { volume.value = v; onVolumeChange({target:{value:v}}) }
})

const updateTime = () => { 
  const now = new Date(); 
  const hours = now.getHours().toString().padStart(2, '0'); 
  const minutes = now.getMinutes().toString().padStart(2, '0'); 
  const timeString = hours + minutes; 
  const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']; 
  const dOfWeek = days[now.getDay()]; 
  const months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']; 
  const day = now.getDate().toString().padStart(2, '0'); 
  const month = months[now.getMonth()]; 
  const year = now.getFullYear(); 
  const fDate = `${day} ${month} ${year}`; 
  if (timeDigits.value.join('') !== timeString || fullDate.value !== fDate) {
    timeDigits.value = timeString.split('');
    dayOfWeek.value = dOfWeek;
    fullDate.value = fDate;
  }
}

onMounted(() => {
  updateTime();
  timeInterval = setInterval(updateTime, 1000);

  const userId = localStorage.getItem('music_userId')
  if (userId) {
    isLoggedIn.value = true
    loadGreeting(userId)
  }

  // 监听来自排行榜的添加队列事件
  window.addEventListener('addToQueue', handleAddToQueue)

  // 检查 localStorage 中是否有待添加的歌曲（页面刷新或重新进入）
  checkPendingSongs()
})

onActivated(() => {
  const userId = localStorage.getItem('music_userId')
  if (userId && !isLoggedIn.value) {
    isLoggedIn.value = true
    loadGreeting(userId)
  }
})

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval);
  window.removeEventListener('addToQueue', handleAddToQueue)
})

// 处理添加到队列的事件
const handleAddToQueue = (event) => {
  const songData = event.detail
  addSongToQueue(songData)
}

// 添加歌曲到队列
const addSongToQueue = (songData) => {
  const exists = queue.value.findIndex(q => q.id === songData.id)
  if (exists === -1) {
    queue.value.push({
      id: songData.id,
      title: songData.title,
      artist: songData.artist
    })
  }
}

// 检查并处理待添加的歌曲
const checkPendingSongs = () => {
  const keys = Object.keys(localStorage)
  const addKeys = keys.filter(key => key.startsWith('add_to_queue_'))

  addKeys.forEach(key => {
    try {
      const songData = JSON.parse(localStorage.getItem(key))
      // 只处理5分钟内的请求
      if (songData && Date.now() - songData.timestamp < 5 * 60 * 1000) {
        addSongToQueue(songData)
      }
      // 清除已处理的项
      localStorage.removeItem(key)
    } catch (e) {
      console.error('处理待添加歌曲失败', e)
      localStorage.removeItem(key)
    }
  })
}

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval);
})

const openPlayerView = () => { showPlayerView.value = true; setTimeout(scrollChatToBottom, 400) }
const closePlayerView = () => { showPlayerView.value = false; setTimeout(() => playerTab.value = 0, 410) }

watch(isPlaying, (newVal) => {
  if (newVal && !showPlayerView.value) {
    openPlayerView()
  }
})
const toggleQueue = () => { showQueue.value = !showQueue.value; queueHeight.value = showQueue.value ? 120 : 0 }
const scrollToBottomDuringAnimation = () => {
  const start = performance.now()
  const animate = (time) => {
    scrollChatToBottom()
    if (time - start < 150) {
      requestAnimationFrame(animate)
    }
  }
  requestAnimationFrame(animate)
}

const onInputFocus = () => { isTyping.value = true; scrollToBottomDuringAnimation() }
const onInputBlur = () => { isTyping.value = false; scrollToBottomDuringAnimation() }
const loadPlayHistory = () => {
  const userId = localStorage.getItem('music_userId')
  if (userId) {
    request.get('/api/music/play-history', { params: { userId } })
      .then(res => {
        if (Array.isArray(res.data)) {
          playHistory.value = res.data
        }
      })
      .catch(err => console.error('加载历史失败', err))
  }
}

const clearPlayHistory = () => {
  const userId = localStorage.getItem('music_userId')
  if (!userId) return
  
  if (confirm('确定要清空所有播放记录吗？一旦清空将无法恢复。')) {
    request.delete(`/api/music/play-history?userId=${userId}`)
      .then(() => {
        playHistory.value = []
      })
      .catch(err => {
        console.error('清空历史失败', err)
        alert('清空历史失败')
      })
  }
}

const switchPlayerTab = (t) => {
  playerTab.value = t
  if (t === 1) {
    loadPlayHistory()
  }
}

const goToRank = () => {
  router.push('/rank')
}
const seekToLyric = (timeMs, idx) => { seekToAudio(timeMs / 1000); currentLyricIndex.value = idx }
const playHistoryItem = (item) => {
  let idx = queue.value.findIndex(q => q.id === item.songId)
  if (idx === -1) {
    queue.value.push({ id: item.songId, title: item.songName, artist: item.artist })
    idx = queue.value.length - 1
  }
  loadAndPlayTrack(queue.value[idx], idx)
}

</script>

<style scoped src="./Index.css"></style>
