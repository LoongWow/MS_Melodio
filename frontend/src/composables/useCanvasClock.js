import { ref, onMounted, onUnmounted } from 'vue'

export function useCanvasClock(options) {
  const { isPlaying, theme, timeDigits } = options
  const canvasRef = ref(null)
  
  let canvas = null
  let ctx = null
  let animationId = null
  let timer = null
  let particles = []
  const maxParticles = 224
  let particlesInitialized = false
  let beat = 0
  const pixelSize = 4
  const gap = 2
  const digitWidth = 5
  const digitHeight = 7

  const calculateTargets = () => {
    if (!canvas) return
    const dpr = window.devicePixelRatio || 1
    const canvasWidth = canvas.width / dpr
    const canvasHeight = canvas.height / dpr
    particles.forEach(p => p.active = false)
    let pIndex = 0

    if (!isPlaying.value) {
      const charPatterns = { 
        '0': [0x3E, 0x41, 0x41, 0x41, 0x3E], '1': [0x00, 0x42, 0x7F, 0x40, 0x00], 
        '2': [0x42, 0x61, 0x51, 0x49, 0x46], '3': [0x22, 0x41, 0x49, 0x49, 0x36], 
        '4': [0x18, 0x14, 0x12, 0x7F, 0x10], '5': [0x27, 0x45, 0x45, 0x45, 0x39], 
        '6': [0x3E, 0x49, 0x49, 0x49, 0x32], '7': [0x01, 0x01, 0x71, 0x09, 0x07], 
        '8': [0x36, 0x49, 0x49, 0x49, 0x36], '9': [0x26, 0x49, 0x49, 0x49, 0x3E], 
        ':': [0x00, 0x24, 0x00] 
      }
      const chars = [timeDigits.value[0], timeDigits.value[1], ':', timeDigits.value[2], timeDigits.value[3]]
      let totalUnits = 0
      chars.forEach((char, index) => { 
        totalUnits += (char === ':' ? 3 : 5)
        if (index < chars.length - 1) totalUnits += 1 
      })
      const totalWidth = totalUnits * (pixelSize + gap)
      let startX = (canvasWidth - totalWidth) / 2
      const startY = (canvasHeight - digitHeight * (pixelSize + gap)) / 2

      chars.forEach((char) => { 
        const pattern = charPatterns[char]
        const w = char === ':' ? 3 : 5
        for (let x = 0; x < w; x++) { 
          for (let y = 0; y < 7; y++) { 
            if ((pattern[x] >> y) & 1) { 
              if (pIndex < maxParticles) { 
                const p = particles[pIndex++]
                p.targetX = startX + x * (pixelSize + gap)
                p.targetY = startY + y * (pixelSize + gap)
                p.active = true
              } 
            } 
          } 
        } 
        startX += (w + 1) * (pixelSize + gap)
      })
    } else {
      const numBars = 32
      const maxBarHeight = 7
      const totalWidth = numBars * (pixelSize + gap) - gap
      const startX = (canvasWidth - totalWidth) / 2
      const startY = (canvasHeight - maxBarHeight * (pixelSize + gap)) / 2
      const now = Date.now()
      beat = (beat || 0) * 0.92
      const currentBeat = beat
      
      for (let i = 0; i < numBars; i++) { 
        const center = (numBars - 1) / 2
        const distFromCenter = Math.abs(i - center) / center
        const envelope = 1 - distFromCenter * 0.3
        const noise = Math.sin(now * 0.008 + i * 0.5) * 0.5 + 0.5
        const noise2 = Math.cos(now * 0.004 + i * 1.2) * 0.5 + 0.5
        const base = (noise * 0.6 + noise2 * 0.4) * envelope
        const beatBoost = currentBeat * (0.6 + Math.sin(i * 1.7 + now * 0.02) * 0.4)
        let barHeight = Math.floor((base + beatBoost) * maxBarHeight) + 1
        
        if (barHeight > maxBarHeight) barHeight = maxBarHeight
        if (barHeight < 1) barHeight = 1
        
        for (let y = 0; y < maxBarHeight; y++) { 
          if (pIndex < maxParticles) { 
            const p = particles[pIndex++]
            p.targetX = startX + i * (pixelSize + gap)
            if (y < barHeight) { 
              p.targetY = startY + (maxBarHeight - 1 - y) * (pixelSize + gap)
              p.active = true 
            } else { 
              p.targetY = startY + (maxBarHeight - barHeight) * (pixelSize + gap)
              p.active = false 
            } 
          } 
        } 
      }
    }
  }

  const drawPixelClock = () => { 
    if (!ctx) return
    if (particles.length === 0) {
      particles = Array.from({ length: maxParticles }).map(() => ({ 
        x: 0, y: 0, targetX: 0, targetY: 0, active: false, alpha: 0 
      }))
    }
    calculateTargets()
    const dpr = window.devicePixelRatio || 1
    const canvasWidth = canvas.width / dpr
    const canvasHeight = canvas.height / dpr
    
    ctx.clearRect(0, 0, canvasWidth, canvasHeight)
    ctx.fillStyle = theme.value === 'dark' ? '#FFFFFF' : '#000000'
    
    particles.forEach(p => { 
      if (!particlesInitialized) { 
        p.x = p.targetX
        p.y = p.targetY
        p.alpha = p.active ? 1 : 0
      } else { 
        p.x += (p.targetX - p.x) * 0.15
        p.y += (p.targetY - p.y) * 0.15
        p.alpha += ((p.active ? 1 : 0) - p.alpha) * 0.15
      } 
      if (p.alpha > 0.01) { 
        ctx.globalAlpha = p.alpha
        ctx.fillRect(p.x, p.y, pixelSize, pixelSize)
      } 
    })
    ctx.globalAlpha = 1.0
    particlesInitialized = true
  }

  const startClockAnimation = () => { 
    const render = () => { 
      drawPixelClock()
      animationId = requestAnimationFrame(render)
    }
    render()
  }

  const triggerBeat = () => {
    beat = 1
  }

  const initCanvas = () => {
    const el = document.getElementById('clockCanvas')
    if (!el) return
    canvas = el
    ctx = canvas.getContext('2d')
    const dpr = window.devicePixelRatio || 1
    // Read CSS width/height
    const rect = canvas.getBoundingClientRect()
    canvas.width = rect.width * dpr
    canvas.height = rect.height * dpr
    ctx.scale(dpr, dpr)
    startClockAnimation()
  }

  onMounted(() => {
    // Delay slightly to ensure DOM is ready and styled
    setTimeout(initCanvas, 100)
  })

  onUnmounted(() => {
    if (animationId) cancelAnimationFrame(animationId)
  })

  return {
    canvasRef,
    triggerBeat
  }
}
