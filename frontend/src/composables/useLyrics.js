import { nextTick } from 'vue'
import request from '@/utils/request'

export function useLyrics(options) {
  const { lyricLines, currentLyricIndex, triggerBeat, lyricScrollRef } = options

  const parseLyric = (yrc) => {
    if (!yrc) return null
    const lines = []
    const rawLines = yrc.split('\n')
    const headRe = /^\[(\d+),(\d+)\](.*)$/
    const wordRe = /\((\d+),(\d+),\d+\)([^\(]*)/g
    
    rawLines.forEach(raw => {
      if (raw.startsWith('{')) return
      const m = raw.match(headRe)
      if (!m) return
      const time = parseInt(m[1], 10)
      const rest = m[3]
      let text = ''
      let wm
      while ((wm = wordRe.exec(rest)) !== null) text += wm[3]
      if (!text) text = rest
      if (text.trim()) lines.push({ time, text })
    })
    return lines.length ? lines : null
  }

  const parseLrc = (lrc) => {
    if (!lrc) return null
    const lines = []
    const re = /\[(\d+):(\d+)(?:\.(\d+))?\]/g
    lrc.split('\n').forEach(line => {
      let m
      const matches = []
      while ((m = re.exec(line)) !== null) matches.push(m)
      if (!matches.length) return
      const text = line.replace(/\[[^\]]+\]/g, '').trim()
      if (!text) return
      matches.forEach(mm => {
        const min = parseInt(mm[1], 10)
        const sec = parseInt(mm[2], 10)
        const ms = mm[3] ? parseInt((mm[3] + '00').slice(0, 3), 10) : 0
        lines.push({ time: (min * 60 + sec) * 1000 + ms, text })
      })
    })
    lines.sort((a, b) => a.time - b.time)
    return lines.length ? lines : null
  }

  const loadLyric = (songId) => {
    request.get(`/api/music/lyric/new`, { params: { id: songId } })
      .then(res => {
        const yrc = res.data?.yrc?.lyric
        const lrc = res.data?.lrc?.lyric
        const lines = parseLrc(lrc) || parseLyric(yrc) || []
        lyricLines.value = lines
        currentLyricIndex.value = -1
      })
      .catch(() => {
        lyricLines.value = []
        currentLyricIndex.value = -1
      })
  }

  const syncLyric = (currentTimeMs) => {
    const lines = lyricLines.value
    if (!lines || !lines.length) return
    let idx = -1
    for (let i = 0; i < lines.length; i++) {
      if (lines[i].time <= currentTimeMs) idx = i
      else break
    }
    if (idx !== currentLyricIndex.value) {
      currentLyricIndex.value = idx
      if (triggerBeat) triggerBeat()
      
      // Auto scroll
      nextTick(() => {
        if (lyricScrollRef.value && idx >= 0) {
          const el = document.getElementById('lyric-' + idx)
          if (el) {
            // Scroll to center
            const container = lyricScrollRef.value
            const offset = el.offsetTop - container.offsetTop - container.clientHeight / 2 + el.clientHeight / 2
            container.scrollTo({ top: offset, behavior: 'smooth' })
          }
        }
      })
    }
  }

  return {
    loadLyric,
    syncLyric
  }
}
