// utils/charts.js - 小程序 Canvas 2D 图表工具（数据总览四图）
// 用法：initCanvas(this, '#donutCanvas').then(({ctx,width,height}) => drawDonut(ctx,width,height,{...}))

function initCanvas(page, selector) {
  return new Promise((resolve, reject) => {
    wx.createSelectorQuery()
      .in(page)
      .select(selector)
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res || !res[0] || !res[0].node) {
          reject(new Error('canvas not found: ' + selector))
          return
        }
        const node = res[0].node
        const width = res[0].width
        const height = res[0].height
        const dpr = (wx.getWindowInfo ? wx.getWindowInfo().pixelRatio : 2) || 2
        node.width = width * dpr
        node.height = height * dpr
        const ctx = node.getContext('2d')
        ctx.scale(dpr, dpr)
        resolve({ ctx, width, height })
      })
  })
}

// ---------- 1. 消费分类占比环形图 ----------
// data: [{name, value, color}]，中心展示 totalText / subText
function drawDonut(ctx, w, h, opts) {
  const { data, totalText, subText } = opts
  ctx.clearRect(0, 0, w, h)

  const legendW = Math.min(150, w * 0.38)
  const cx = (w - legendW) / 2
  const cy = h / 2
  const outerR = Math.min((w - legendW) / 2, h / 2) - 6
  const innerR = outerR * 0.64

  const total = data.reduce((s, d) => s + d.value, 0) || 1
  let angle = -Math.PI / 2
  data.forEach((d) => {
    const sweep = (d.value / total) * Math.PI * 2
    ctx.beginPath()
    ctx.arc(cx, cy, outerR, angle, angle + sweep)
    ctx.arc(cx, cy, innerR, angle + sweep, angle, true)
    ctx.closePath()
    ctx.fillStyle = d.color
    ctx.fill()
    angle += sweep
  })

  // 中心文字
  ctx.fillStyle = '#0f172a'
  ctx.font = 'bold 17px sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(totalText, cx, cy - 8)
  ctx.fillStyle = '#64748b'
  ctx.font = 'bold 10px sans-serif'
  ctx.fillText(subText, cx, cy + 10)

  // 右侧图例
  const lx = w - legendW + 8
  const rowH = 22
  const ly0 = cy - ((data.length - 1) * rowH) / 2
  data.forEach((d, i) => {
    const y = ly0 + i * rowH
    ctx.beginPath()
    ctx.arc(lx, y, 4, 0, Math.PI * 2)
    ctx.fillStyle = d.color
    ctx.fill()
    ctx.fillStyle = '#475569'
    ctx.font = 'bold 11px sans-serif'
    ctx.textAlign = 'left'
    ctx.textBaseline = 'middle'
    ctx.fillText(d.name + ' ' + d.value + '%', lx + 10, y)
  })
}

// ---------- 2. 资产总值与净值月度演进趋势（面积图） ----------
function drawArea(ctx, w, h, opts) {
  const { months, total, net } = opts
  ctx.clearRect(0, 0, w, h)

  const padL = 42
  const padR = 10
  const padT = 26
  const padB = 24
  const cw = w - padL - padR
  const ch = h - padT - padB
  const maxV = Math.max.apply(null, total) * 1.15
  const stepX = months.length > 1 ? cw / (months.length - 1) : cw

  const px = (i) => padL + i * stepX
  const py = (v) => padT + ch - (v / maxV) * ch

  // 网格线 + y轴
  ctx.strokeStyle = '#cbd5e1'
  ctx.setLineDash([4, 4])
  ctx.lineWidth = 1
  ctx.font = 'bold 9px monospace'
  ctx.fillStyle = '#64748b'
  ctx.textAlign = 'right'
  ctx.textBaseline = 'middle'
  for (let g = 0; g <= 3; g++) {
    const v = (maxV / 3) * g
    const y = py(v)
    ctx.beginPath()
    ctx.moveTo(padL, y)
    ctx.lineTo(w - padR, y)
    ctx.stroke()
    ctx.fillText(Math.round(v) + 'k', padL - 5, y)
  }
  ctx.setLineDash([])

  function seriesPath(values) {
    ctx.beginPath()
    values.forEach((v, i) => {
      if (i === 0) ctx.moveTo(px(0), py(v))
      else {
        // 平滑曲线
        const x0 = px(i - 1)
        const y0 = py(values[i - 1])
        const x1 = px(i)
        const y1 = py(v)
        const mx = (x0 + x1) / 2
        ctx.bezierCurveTo(mx, y0, mx, y1, x1, y1)
      }
    })
  }

  function fillArea(values, colorTop) {
    seriesPath(values)
    ctx.lineTo(px(values.length - 1), padT + ch)
    ctx.lineTo(px(0), padT + ch)
    ctx.closePath()
    const grad = ctx.createLinearGradient(0, padT, 0, padT + ch)
    grad.addColorStop(0, colorTop)
    grad.addColorStop(1, 'rgba(255,255,255,0.01)')
    ctx.fillStyle = grad
    ctx.fill()
  }

  function strokeLine(values, color) {
    seriesPath(values)
    ctx.strokeStyle = color
    ctx.lineWidth = 2.2
    ctx.stroke()
    // 数据点
    values.forEach((v, i) => {
      ctx.beginPath()
      ctx.arc(px(i), py(v), 2.6, 0, Math.PI * 2)
      ctx.fillStyle = '#ffffff'
      ctx.fill()
      ctx.lineWidth = 1.6
      ctx.strokeStyle = color
      ctx.stroke()
    })
  }

  fillArea(total, 'rgba(59,130,246,0.32)')
  fillArea(net, 'rgba(139,92,246,0.26)')
  strokeLine(total, '#3b82f6')
  strokeLine(net, '#8b5cf6')

  // 峰值标注（总值最高点）
  let peakIdx = 0
  total.forEach((v, i) => { if (v > total[peakIdx]) peakIdx = i })
  const peakX = Math.min(Math.max(px(peakIdx), padL + 30), w - padR - 34)
  const peakY = py(total[peakIdx])
  ctx.beginPath()
  ctx.fillStyle = '#0891b2'
  ctx.arc(peakX, peakY - 16, 15, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = '#ffffff'
  ctx.font = 'bold 8px sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('￥' + total[peakIdx].toFixed(1) + 'K', peakX, peakY - 16)

  // x轴标签
  ctx.fillStyle = '#64748b'
  ctx.font = 'bold 9px monospace'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'top'
  const labelEvery = months.length > 8 ? 2 : 1
  months.forEach((m, i) => {
    if (i % labelEvery !== 0 && i !== months.length - 1) return
    ctx.fillText(m, px(i), padT + ch + 7)
  })

  // 图例
  ctx.textAlign = 'right'
  ctx.textBaseline = 'middle'
  ctx.font = 'bold 10px sans-serif'
  ctx.fillStyle = '#8b5cf6'
  ctx.fillText('● 资产净值', w - padR, 10)
  ctx.fillStyle = '#3b82f6'
  ctx.fillText('● 资产总值', w - padR - 74, 10)
}

// ---------- 3. AI多维账户风控雷达图（六维） ----------
function drawRadarChart(ctx, w, h, opts) {
  const { indicators, values, max } = opts
  ctx.clearRect(0, 0, w, h)

  const cx = w / 2
  const cy = h / 2 + 4
  const R = Math.min(w, h) / 2 - 30
  const n = indicators.length
  const pt = (i, r) => {
    const a = -Math.PI / 2 + (i * 2 * Math.PI) / n
    return [cx + r * Math.cos(a), cy + r * Math.sin(a)]
  }

  // 网格（3 层）
  for (let ring = 1; ring <= 3; ring++) {
    ctx.beginPath()
    for (let i = 0; i <= n; i++) {
      const [x, y] = pt(i % n, (R / 3) * ring)
      if (i === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    }
    ctx.setLineDash([4, 4])
    ctx.strokeStyle = '#cbd5e1'
    ctx.lineWidth = 1
    ctx.stroke()
  }
  ctx.setLineDash([])

  // 轴线 + 标签
  ctx.font = 'bold 10px sans-serif'
  indicators.forEach((name, i) => {
    const [x, y] = pt(i, R)
    ctx.beginPath()
    ctx.moveTo(cx, cy)
    ctx.lineTo(x, y)
    ctx.strokeStyle = '#e2e8f0'
    ctx.stroke()
    const [lx, ly] = pt(i, R + 14)
    ctx.fillStyle = '#334155'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(name, lx, ly)
  })

  // 数据多边形
  ctx.beginPath()
  for (let i = 0; i <= n; i++) {
    const idx = i % n
    const [x, y] = pt(idx, (values[idx] / max) * R)
    if (i === 0) ctx.moveTo(x, y)
    else ctx.lineTo(x, y)
  }
  ctx.closePath()
  ctx.fillStyle = 'rgba(16,185,129,0.28)'
  ctx.fill()
  ctx.strokeStyle = '#10b981'
  ctx.lineWidth = 2
  ctx.stroke()

  // 顶点
  const dotColors = ['#06b6d4', '#3b82f6', '#10b981', '#6366f1', '#f59e0b', '#8b5cf6']
  values.forEach((v, i) => {
    const [x, y] = pt(i, (v / max) * R)
    ctx.beginPath()
    ctx.arc(x, y, 3, 0, Math.PI * 2)
    ctx.fillStyle = dotColors[i % dotColors.length]
    ctx.fill()
  })
}

// ---------- 4. 近四季度预约业务分类办结量堆叠柱状图 ----------
// series: [{name, color, data:[q1..q4]}]（按从下到上顺序传入）
function drawStackedBar(ctx, w, h, opts) {
  const { labels, series, yMax } = opts
  ctx.clearRect(0, 0, w, h)

  const padL = 34
  const padR = 10
  const padT = 14
  const padB = 46
  const cw = w - padL - padR
  const ch = h - padT - padB
  const groupW = cw / labels.length
  const barW = Math.min(34, groupW * 0.4)

  const py = (v) => padT + ch - (v / yMax) * ch

  // y轴网格
  ctx.font = 'bold 9px monospace'
  ctx.fillStyle = '#64748b'
  ctx.textAlign = 'right'
  ctx.textBaseline = 'middle'
  for (let v = 0; v <= yMax; v += yMax / 2) {
    const y = py(v)
    ctx.setLineDash([4, 4])
    ctx.strokeStyle = '#cbd5e1'
    ctx.beginPath()
    ctx.moveTo(padL, y)
    ctx.lineTo(w - padR, y)
    ctx.stroke()
    ctx.setLineDash([])
    ctx.fillText(String(v), padL - 5, y)
  }

  // 堆叠柱
  labels.forEach((lb, gi) => {
    const x = padL + gi * groupW + (groupW - barW) / 2
    let acc = 0
    series.forEach((s) => {
      const v = s.data[gi] || 0
      const y1 = py(acc)
      const y2 = py(acc + v)
      ctx.fillStyle = s.color
      ctx.fillRect(x, y2, barW, y1 - y2)
      acc += v
    })
    // x轴标签
    ctx.fillStyle = '#64748b'
    ctx.font = 'bold 10px monospace'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'top'
    ctx.fillText(lb, padL + gi * groupW + groupW / 2, padT + ch + 8)
  })

  // 图例（底部）
  ctx.font = 'bold 9px sans-serif'
  ctx.textAlign = 'left'
  ctx.textBaseline = 'middle'
  let lx = padL
  const ly = h - 14
  series.forEach((s) => {
    ctx.fillStyle = s.color
    ctx.fillRect(lx, ly - 4, 8, 8)
    ctx.fillStyle = '#64748b'
    ctx.fillText(s.name, lx + 12, ly)
    lx += 12 + ctx.measureText(s.name).width + 14
  })
}

module.exports = {
  initCanvas,
  drawDonut,
  drawArea,
  drawRadarChart,
  drawStackedBar,
}
