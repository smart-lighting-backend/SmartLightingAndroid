<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

// ── 右侧面板数据 ──────────────────────────────────────
const energyToday = ref(2450)
const avgLux      = ref(25)

const strategyLogs = ref([
  {
    id: 1,
    title: '日出降光策略触发',
    desc: '全区照度调至 20%',
    time: '06:30 AM',
    status: 'active',
  },
  {
    id: 2,
    title: '深夜节能模式',
    desc: '间隔亮灯启用',
    time: '00:00 AM',
    status: 'normal',
  },
])

const timestamps = ref(['10:42', '10:15', '09:58', '09:30'])

// ── Canvas 等距城市绘制 ────────────────────────────────
const canvasRef = ref(null)
let animFrame  = null

function drawIsometricCity(ctx, w, h) {
  ctx.clearRect(0, 0, w, h)

  const cx = w / 2
  const cy = h / 2 - 40

  // 背景网格（等距格）
  ctx.strokeStyle = 'rgba(0, 180, 255, 0.06)'
  ctx.lineWidth = 0.5

  // 等距地面网格
  const cols = 12, rows = 10
  const tw = 60, th = 30  // tile size

  for (let r = -rows; r <= rows; r++) {
    for (let c = -cols; c <= cols; c++) {
      const px = cx + (c - r) * (tw / 2)
      const py = cy + (c + r) * (th / 2)
      ctx.beginPath()
      ctx.moveTo(px, py - th / 2)
      ctx.lineTo(px + tw / 2, py)
      ctx.lineTo(px, py + th / 2)
      ctx.lineTo(px - tw / 2, py)
      ctx.closePath()
      ctx.stroke()
    }
  }

  // 建筑定义（列, 行, 宽格, 高px, 颜色主调）
  const buildings = [
    { c:  0, r:  0, cw: 2, rw: 2, h: 80,  hue: 200 },
    { c: -2, r:  1, cw: 1, rw: 1, h: 50,  hue: 210 },
    { c:  2, r: -1, cw: 1, rw: 2, h: 70,  hue: 195 },
    { c: -3, r: -1, cw: 2, rw: 1, h: 60,  hue: 205 },
    { c:  1, r:  2, cw: 1, rw: 1, h: 40,  hue: 220 },
    { c: -1, r: -2, cw: 1, rw: 1, h: 55,  hue: 200 },
    { c:  3, r:  1, cw: 1, rw: 1, h: 45,  hue: 215 },
    { c: -4, r:  2, cw: 1, rw: 1, h: 35,  hue: 200 },
    { c:  0, r: -3, cw: 2, rw: 1, h: 65,  hue: 190 },
    { c:  4, r: -2, cw: 1, rw: 1, h: 50,  hue: 210 },
    { c: -2, r:  3, cw: 1, rw: 2, h: 55,  hue: 205 },
    { c:  2, r:  3, cw: 2, rw: 1, h: 40,  hue: 220 },
    { c: -5, r:  0, cw: 1, rw: 1, h: 45,  hue: 200 },
    { c:  5, r:  0, cw: 1, rw: 1, h: 50,  hue: 210 },
    { c:  0, r:  4, cw: 3, rw: 2, h: 30,  hue: 180 },
    { c: -3, r: -3, cw: 1, rw: 1, h: 70,  hue: 195 },
    { c:  3, r:  4, cw: 1, rw: 1, h: 35,  hue: 215 },
  ]

  buildings.forEach(b => {
    const bx = cx + (b.c - b.r) * (tw / 2)
    const by = cy + (b.c + b.r) * (th / 2)
    const bw = (b.cw + b.rw) * (tw / 2)
    const bd = (b.cw + b.rw) * (th / 2)
    const bh = b.h

    // 顶面
    ctx.beginPath()
    ctx.moveTo(bx, by - bd / 2 - bh)
    ctx.lineTo(bx + bw / 2, by - bh)
    ctx.lineTo(bx, by + bd / 2 - bh)
    ctx.lineTo(bx - bw / 2, by - bh)
    ctx.closePath()
    ctx.fillStyle = `rgba(0, ${b.hue}, 255, 0.15)`
    ctx.fill()
    ctx.strokeStyle = `rgba(0, ${b.hue + 50}, 255, 0.4)`
    ctx.lineWidth = 0.8
    ctx.stroke()

    // 右侧面
    ctx.beginPath()
    ctx.moveTo(bx, by + bd / 2 - bh)
    ctx.lineTo(bx + bw / 2, by - bh)
    ctx.lineTo(bx + bw / 2, by)
    ctx.lineTo(bx, by + bd / 2)
    ctx.closePath()
    ctx.fillStyle = `rgba(0, ${b.hue - 30}, 200, 0.2)`
    ctx.fill()
    ctx.strokeStyle = `rgba(0, ${b.hue + 30}, 255, 0.3)`
    ctx.stroke()

    // 左侧面
    ctx.beginPath()
    ctx.moveTo(bx, by + bd / 2 - bh)
    ctx.lineTo(bx - bw / 2, by - bh)
    ctx.lineTo(bx - bw / 2, by)
    ctx.lineTo(bx, by + bd / 2)
    ctx.closePath()
    ctx.fillStyle = `rgba(0, ${b.hue - 60}, 180, 0.12)`
    ctx.fill()
    ctx.strokeStyle = `rgba(0, ${b.hue}, 220, 0.25)`
    ctx.stroke()

    // 顶部发光线条（科技感）
    if (Math.random() > 0.4) {
      ctx.beginPath()
      ctx.moveTo(bx, by - bd / 2 - bh)
      ctx.lineTo(bx + bw / 2, by - bh)
      ctx.strokeStyle = `rgba(0, 220, 255, 0.5)`
      ctx.lineWidth = 1.2
      ctx.stroke()
    }
  })

  // 故障节点 L-1024（固定位置）
  const fx = cx + (1 - 1) * (tw / 2)
  const fy = cy + (1 + 1) * (th / 2) - 60

  // 脉冲圆
  const t = Date.now() / 1000
  const pulse = Math.sin(t * 3) * 0.5 + 0.5
  ctx.beginPath()
  ctx.arc(fx, fy + 18, 12 + pulse * 6, 0, Math.PI * 2)
  ctx.strokeStyle = `rgba(255, 80, 80, ${0.3 + pulse * 0.3})`
  ctx.lineWidth = 1.5
  ctx.stroke()

  ctx.beginPath()
  ctx.arc(fx, fy + 18, 6, 0, Math.PI * 2)
  ctx.fillStyle = `rgba(255, 100, 80, ${0.8 + pulse * 0.2})`
  ctx.fill()

  // 标注文字
  ctx.fillStyle = '#ff6060'
  ctx.font = '11px system-ui, sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('节点故障', fx, fy - 2)
  ctx.fillStyle = '#ffaaaa'
  ctx.fillText('L-1024', fx, fy + 12)

  // 正常节点
  const greenDots = [
    { c: -3, r: -2 }, { c: 2, r: 2 }, { c: -1, r: 3 }
  ]
  greenDots.forEach(d => {
    const gx = cx + (d.c - d.r) * (tw / 2)
    const gy = cy + (d.c + d.r) * (th / 2)
    ctx.beginPath()
    ctx.arc(gx, gy, 5, 0, Math.PI * 2)
    ctx.fillStyle = 'rgba(0, 255, 150, 0.85)'
    ctx.fill()
    ctx.beginPath()
    ctx.arc(gx, gy, 9 + pulse * 3, 0, Math.PI * 2)
    ctx.strokeStyle = `rgba(0, 255, 150, ${0.2 + pulse * 0.15})`
    ctx.lineWidth = 1
    ctx.stroke()
  })
}

function startAnimation() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')

  const resize = () => {
    canvas.width  = canvas.parentElement.clientWidth
    canvas.height = canvas.parentElement.clientHeight
  }

  resize()
  window.addEventListener('resize', resize)

  const loop = () => {
    drawIsometricCity(ctx, canvas.width, canvas.height)
    animFrame = requestAnimationFrame(loop)
  }
  loop()

  return () => {
    window.removeEventListener('resize', resize)
    cancelAnimationFrame(animFrame)
  }
}

let cleanup = null
onMounted(() => { cleanup = startAnimation() })
onUnmounted(() => { cleanup?.() })
</script>

<template>
  <div class="twin-page">
    <!-- 左侧时间戳列 -->
    <div class="time-panel">
      <div
        v-for="(t, i) in timestamps"
        :key="t"
        class="time-item"
        :class="{ active: i === 0 }"
      >{{ t }}</div>
    </div>

    <!-- 中心地图 Canvas -->
    <div class="map-area">
      <div class="map-top-icon">
        <svg viewBox="0 0 24 24" fill="none"><rect x="5" y="2" width="14" height="14" rx="2" stroke="#4dd0e1" stroke-width="1.5"/><path d="M8 16v4M12 16v4M16 16v4" stroke="#4dd0e1" stroke-width="1.5" stroke-linecap="round"/></svg>
      </div>
      <canvas ref="canvasRef" class="city-canvas" />
    </div>

    <!-- 右侧信息面板 -->
    <div class="info-panel">
      <!-- 今日能耗 -->
      <div class="panel-card energy-card">
        <div class="card-header">
          <span class="card-title">今日能耗</span>
          <span class="card-value">{{ energyToday.toLocaleString() }} kWh</span>
        </div>
        <div class="mini-chart">
          <div v-for="(v, i) in [3,4,3,5,7,6,4]" :key="i" class="mini-bar" :style="{ height: v*12+'px', opacity: i===4?1:0.5 }" />
        </div>
        <div class="chart-labels">
          <span>00:00</span>
          <span>08:00</span>
          <span>16:00</span>
        </div>
      </div>

      <!-- 区域照度热力 -->
      <div class="panel-card lux-card">
        <div class="card-title">区域照度热力</div>
        <div class="lux-bar">
          <div class="lux-gradient" />
          <span class="lux-label">平均 {{ avgLux }} lx</span>
        </div>
      </div>

      <!-- 策略执行轨迹 -->
      <div class="panel-card strategy-card">
        <div class="card-header">
          <span class="card-title">策略执行轨迹</span>
          <button class="refresh-btn">
            <svg viewBox="0 0 24 24" fill="none"><path d="M1 4v6h6M23 20v-6h-6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M20.49 9A9 9 0 005.64 5.64L1 10M23 14l-4.64 4.36A9 9 0 013.51 15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
        </div>
        <div class="strategy-list">
          <div v-for="log in strategyLogs" :key="log.id" class="strategy-item">
            <span class="dot" :class="log.status" />
            <div class="strategy-content">
              <div class="strategy-title">{{ log.title }}</div>
              <div class="strategy-desc">{{ log.desc }}</div>
              <div class="strategy-time">{{ log.time }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.twin-page {
  display: flex;
  width: 100%;
  height: 100%;
  background: #060f1e;
  overflow: hidden;
  position: relative;
}

/* 左侧时间戳 */
.time-panel {
  width: 72px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 16px 0;
  background: rgba(5, 15, 30, 0.8);
  border-right: 1px solid rgba(0, 150, 220, 0.1);
  gap: 2px;
  align-items: center;
}

.time-item {
  padding: 10px 8px;
  font-size: 11px;
  color: rgba(100, 160, 210, 0.5);
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  text-align: center;
  width: 90%;
}
.time-item.active {
  background: rgba(0, 100, 200, 0.25);
  color: #4dd0e1;
  font-weight: 600;
  border: 1px solid rgba(0, 150, 220, 0.3);
}
.time-item:hover:not(.active) {
  background: rgba(0, 80, 150, 0.15);
  color: rgba(160, 200, 240, 0.8);
}

/* 地图区域 */
.map-area {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.map-top-icon {
  position: absolute;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  width: 36px;
  height: 36px;
  background: rgba(0, 80, 150, 0.5);
  border: 1px solid rgba(77, 208, 225, 0.3);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}
.map-top-icon svg { width: 20px; height: 20px; }

.city-canvas {
  width: 100%;
  height: 100%;
  display: block;
}

/* 右侧面板 */
.info-panel {
  width: 240px;
  flex-shrink: 0;
  background: rgba(5, 14, 28, 0.9);
  border-left: 1px solid rgba(0, 150, 220, 0.12);
  display: flex;
  flex-direction: column;
  padding: 16px 12px;
  gap: 14px;
  overflow-y: auto;
}

.panel-card {
  background: rgba(0, 25, 55, 0.6);
  border: 1px solid rgba(0, 150, 220, 0.15);
  border-radius: 10px;
  padding: 14px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-title {
  font-size: 12px;
  color: rgba(150, 200, 230, 0.85);
  font-weight: 600;
}

.card-value {
  font-size: 11px;
  color: #4dd0e1;
}

/* 迷你柱状图 */
.mini-chart {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 60px;
  margin-bottom: 6px;
}

.mini-bar {
  flex: 1;
  background: rgba(0, 150, 255, 0.6);
  border-radius: 3px 3px 0 0;
  transition: height 0.3s;
}

.chart-labels {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: rgba(100, 160, 200, 0.5);
}

/* 照度热力条 */
.lux-card .card-title {
  margin-bottom: 10px;
  display: block;
}

.lux-bar {
  position: relative;
  height: 44px;
  border-radius: 8px;
  overflow: hidden;
}

.lux-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, #0033aa, #0066cc, #00aaff, #ffaa00, #ff6600);
  opacity: 0.75;
}

.lux-label {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  text-shadow: 0 1px 4px rgba(0,0,0,0.7);
}

/* 策略执行轨迹 */
.refresh-btn {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: rgba(77, 208, 225, 0.6);
  cursor: pointer;
  transition: color 0.2s;
}
.refresh-btn:hover { color: #4dd0e1; }
.refresh-btn svg { width: 14px; height: 14px; }

.strategy-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.strategy-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 3px;
}
.dot.active  { background: #4dd0e1; box-shadow: 0 0 6px #4dd0e1; }
.dot.normal  { background: rgba(77, 208, 225, 0.4); }

.strategy-content { flex: 1; }
.strategy-title {
  font-size: 11px;
  color: rgba(180, 220, 250, 0.9);
  font-weight: 500;
  margin-bottom: 2px;
}
.strategy-desc {
  font-size: 11px;
  color: rgba(120, 170, 210, 0.7);
  margin-bottom: 3px;
}
.strategy-time {
  font-size: 10px;
  color: #4dd0e1;
  font-weight: 600;
}
</style>
