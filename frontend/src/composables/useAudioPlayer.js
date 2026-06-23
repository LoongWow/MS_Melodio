import { onMounted, onUnmounted, ref } from 'vue'
import request from '@/utils/request'

export function useAudioPlayer(options) {
  const {
    isPlaying, currentTrack, volume, currentTime, duration,
    currentTimeStr, durationStr, queue, currentTrackIndex,
    triggerBeat, syncLyric
  } = options

  let audio = null
  let playStartTime = null
  let currentSong = null
  let seeking = false

  const formatTime = (sec) => {
    sec = Math.max(0, Math.floor(sec || 0))
    const m = Math.floor(sec / 60)
    const s = sec % 60
    return m + ':' + (s < 10 ? '0' + s : s)
  }

  const recordPlayStart = () => {
    playStartTime = Date.now()
    console.log('播放开始记录', currentSong)
  }

  const recordPlayCompletion = (completed) => {
    if (!playStartTime || !currentSong) return

    const dur = Math.floor((Date.now() - playStartTime) / 1000)
    const userId = localStorage.getItem('music_userId')

    if (dur < 5) {
      console.log('播放时长不足5秒，不记录')
      playStartTime = null
      return
    }

    if (userId) {
      request.post(`/api/music/play-history`, {
        userId: Number(userId),
        songId: currentSong.id,
        songName: currentSong.name,
        artist: currentSong.artist,
        duration: dur,
        completed: completed
      }).then(res => {
        console.log('播放记录已保存', res.data)
        // Note: history refresh logic should be handled appropriately
      }).catch(err => {
        console.error('播放记录保存失败', err)
      })
    }

    playStartTime = null
  }

  const playNext = () => {
    if (!queue.value.length) return
    const next = (currentTrackIndex.value + 1) % queue.value.length
    const track = queue.value[next]
    if (audio) {
      audio.pause()
    }
    currentTrackIndex.value = next
    currentTrack.value = track.title + ' - ' + track.artist
    currentTime.value = 0
    duration.value = 0
    currentTimeStr.value = '0:00'
    durationStr.value = '0:00'
    loadAndPlayTrack(track, next)
  }

  const playPrev = () => {
    if (!queue.value.length) return
    const prev = (currentTrackIndex.value - 1 + queue.value.length) % queue.value.length
    const track = queue.value[prev]
    if (audio) {
      audio.pause()
    }
    currentTrackIndex.value = prev
    currentTrack.value = track.title + ' - ' + track.artist
    currentTime.value = 0
    duration.value = 0
    currentTimeStr.value = '0:00'
    durationStr.value = '0:00'
    loadAndPlayTrack(track, prev)
  }

  const togglePlay = () => {
    if (!queue.value.length) return
    if (isPlaying.value) {
      audio.pause()
    } else {
      if (!audio.src || audio.src === window.location.href) {
        const track = queue.value[currentTrackIndex.value]
        loadAndPlayTrack(track, currentTrackIndex.value)
      } else {
        audio.play()
      }
    }
  }

  const loadAndPlayTrack = (track, index) => {
    if (!track) return
    if (audio) audio.pause()
    const userId = localStorage.getItem('music_userId')
    console.log('准备播放队列歌曲', track)

    request.get(`/api/music/play-url`, { params: { songId: track.id, userId } })
      .then(res => {
        const url = res.data && res.data.url
        if (!url) {
          alert('无版权或无法播放')
          return
        }

        currentTrackIndex.value = index
        currentTrack.value = track.title + ' - ' + track.artist
        currentSong = {
          id: track.id,
          name: track.title,
          artist: track.artist
        }

        audio.src = url
        audio.play()
        
        // syncLyric equivalent here
        if (options.loadLyric) {
          options.loadLyric(track.id)
        }
      })
      .catch(err => {
        console.error('获取播放 URL 失败:', err)
        alert('获取播放链接失败，请重试')
      })
  }

  const loadAndPlayById = (songId) => {
    let track = queue.value.find(item => item.id === songId)
    let index = queue.value.findIndex(item => item.id === songId)
    if (!track) {
      track = { id: songId, title: '未知歌曲', artist: '未知艺术家' }
      index = currentTrackIndex.value
    }
    loadAndPlayTrack(track, index >= 0 ? index : currentTrackIndex.value)
  }

  const onVolumeChange = (e) => {
    const v = e.target.value
    volume.value = v
    if (audio) {
      audio.volume = v / 100
    }
  }

  const onSeek = (e) => {
    const v = e.target.value
    if (audio && audio.src) {
      audio.currentTime = v
    }
    currentTime.value = v
    currentTimeStr.value = formatTime(v)
  }

  const tapQueueItem = (index) => {
    if (index === currentTrackIndex.value) return
    const above = queue.value.slice(0, index)
    const clicked = queue.value[index]
    const below = queue.value.slice(index + 1)
    queue.value = [clicked, ...below, ...above]
    
    if (audio) {
      audio.pause()
    }
    currentTrackIndex.value = 0
    currentTrack.value = clicked.title + ' - ' + clicked.artist
    loadAndPlayTrack(clicked, 0)
  }

  const deleteQueueItem = (index) => {
    const newQueue = queue.value.filter((_, i) => i !== index)
    const newIndex = index < currentTrackIndex.value ? currentTrackIndex.value - 1 : currentTrackIndex.value
    queue.value = newQueue
    currentTrackIndex.value = Math.min(newIndex, newQueue.length - 1)
  }

  const stop = () => {
    if (audio) {
      audio.pause()
    }
  }

  const seekTo = (sec) => {
    if (!audio || !audio.src) return
    audio.currentTime = sec
    if (audio.paused) audio.play()
    currentTime.value = sec
    currentTimeStr.value = formatTime(sec)
  }

  onMounted(() => {
    audio = new Audio()
    audio.volume = volume.value / 100

    audio.addEventListener('play', () => {
      isPlaying.value = true
      recordPlayStart()
      console.log('音频开始播放')
    })

    audio.addEventListener('pause', () => {
      isPlaying.value = false
      recordPlayCompletion(false)
    })

    audio.addEventListener('ended', () => {
      isPlaying.value = false
      recordPlayCompletion(true)
      playNext()
    })

    audio.addEventListener('error', (e) => {
      console.error('audio error', e)
      alert('播放失败，可能由于跨域或格式不支持')
      isPlaying.value = false
      recordPlayCompletion(false)
    })

    audio.addEventListener('timeupdate', () => {
      if (syncLyric) syncLyric(audio.currentTime * 1000)
      if (seeking) return
      const ct = audio.currentTime || 0
      const du = audio.duration || 0
      currentTime.value = ct
      duration.value = du
      currentTimeStr.value = formatTime(ct)
      durationStr.value = formatTime(du)
    })

    audio.addEventListener('loadedmetadata', () => {
      const du = audio.duration || 0
      if (du) {
        duration.value = du
        durationStr.value = formatTime(du)
      }
    })
  })

  onUnmounted(() => {
    if (audio) {
      audio.pause()
      audio = null
    }
  })

  return {
    playNext,
    playPrev,
    togglePlay,
    onVolumeChange,
    onSeek,
    loadAndPlayTrack,
    loadAndPlayById,
    tapQueueItem,
    deleteQueueItem,
    stop,
    seekTo
  }
}
