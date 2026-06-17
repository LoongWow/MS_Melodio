import { ref, nextTick } from 'vue'
import axios from 'axios'

export function useChat(options) {
  const {
    apiBase, messages, chatInput, isAiThinking, queue, currentTrack,
    currentTrackIndex, showPlayerView, playerTab,
    playNext, playPrev, loadAndPlayTrack, loadAndPlayById, stopAudio
  } = options

  const chatScrollRef = ref(null)

  const scrollChatToBottom = () => {
    nextTick(() => {
      if (chatScrollRef.value) {
        chatScrollRef.value.scrollTop = chatScrollRef.value.scrollHeight
      }
    })
  }

  const loadGreeting = (userId) => {
    axios.get(`${apiBase}/greeting`, { params: { userId: Number(userId) } })
      .then(res => {
        const reply = res.data?.reply || ''
        const songs = res.data?.songs || []

        const greetingMsg = {
          id: Date.now(),
          role: 'assistant',
          author: 'CLAUDIO',
          content: reply
        }

        messages.value = [greetingMsg]

        if (songs.length) {
          queue.value = songs.map(s => ({
            id: s.id,
            title: s.name,
            artist: s.artist || 'Unknown'
          }))

          currentTrackIndex.value = 0
          currentTrack.value = queue.value[0].title + ' - ' + queue.value[0].artist
          showPlayerView.value = false
        }
        scrollChatToBottom()
      })
      .catch(() => {
        console.log('加载问候失败')
      })
  }

  const sendChat = () => {
    const message = (chatInput.value || '').trim()
    if (!message) return
    const userId = localStorage.getItem('music_userId')
    if (!userId) {
      // Need a router instance to redirect, or emit event
      if (options.goToLogin) options.goToLogin()
      return
    }

    const userMsg = { id: Date.now(), role: 'user', author: 'YOU', content: message }
    
    messages.value.push(userMsg)
    chatInput.value = ''
    isAiThinking.value = true
    scrollChatToBottom()

    axios.post(`${apiBase}/chat`, { message, userId: Number(userId) })
      .then(res => {
        isAiThinking.value = false
        
        const reply = res.data?.reply || ''
        const songs = res.data?.songs || []
        const songId = res.data?.songId
        const action = res.data?.action
        const newUserId = res.data?.newUserId
        // const newNickname = res.data?.newNickname

        const addAssistantMsg = (content) => {
          messages.value.push({
            id: Date.now() + 1,
            role: 'assistant',
            author: 'CLAUDIO',
            content
          })
          scrollChatToBottom()
        }

        if (action === 'pause_music') {
          if (options.pauseAudio) options.pauseAudio()
          addAssistantMsg(reply)
          return
        }

        if (action === 'resume_music') {
          if (options.resumeAudio) {
            const success = options.resumeAudio()
            if (!success) {
              addAssistantMsg('当前没有正在播放的歌曲，请先搜索或选择歌曲。')
              return
            }
          }
          addAssistantMsg(reply)
          return
        }

        if (action === 'play_next') {
          playNext()
          addAssistantMsg(reply)
          return
        }

        if (action === 'play_previous') {
          playPrev()
          addAssistantMsg(reply)
          return
        }

        if (action && action.startsWith('set_volume:')) {
          const vol = parseInt(action.substring(11))
          if (!isNaN(vol) && vol >= 0 && vol <= 100) {
            if (options.setVolume) options.setVolume(vol)
          }
          addAssistantMsg(reply)
          return
        }

        if (action === 'logout') {
          addAssistantMsg(reply)
          stopAudio()
          
          queue.value = []
          currentTrack.value = ''
          currentTrackIndex.value = 0
          showPlayerView.value = false
          
          localStorage.removeItem('music_userId')
          alert('已退出登录')
          setTimeout(() => {
            if (options.goToLogin) options.goToLogin()
          }, 1500)
          return
        }

        if (action === 'switch_account' && newUserId) {
          addAssistantMsg(reply)
          localStorage.setItem('music_userId', newUserId)
          alert('切换成功')
          setTimeout(() => {
            messages.value = []
            queue.value = []
            currentTrack.value = ''
            showPlayerView.value = false
            loadGreeting(newUserId)
          }, 1500)
          return
        }

        const nextMessages = [...messages.value, { id: Date.now() + 1, role: 'assistant', author: 'CLAUDIO', content: reply }]

        if (songs.length) {
          queue.value = songs.map(s => ({ id: s.id, title: s.name, artist: s.artist || 'Unknown' }))
          currentTrackIndex.value = 0
          currentTrack.value = queue.value[0].title + ' - ' + queue.value[0].artist
          showPlayerView.value = true
          playerTab.value = 0
          messages.value = nextMessages
          scrollChatToBottom()
          loadAndPlayTrack(queue.value[0], 0)
        } else {
          messages.value = nextMessages
          scrollChatToBottom()
          if (songId) {
            loadAndPlayById(songId)
          }
        }
      })
      .catch(() => {
        isAiThinking.value = false
        alert('对话失败')
      })
  }

  return {
    chatScrollRef,
    loadGreeting,
    sendChat,
    scrollChatToBottom
  }
}
