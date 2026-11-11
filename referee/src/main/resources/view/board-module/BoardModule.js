import { api as entityModule } from '../entity-module/GraphicEntityModule.js'
import { ToggleModule } from '../toggle-module/ToggleModule.js'

/* global PIXI */

// UX 15.2: mirrors, beams and streak chips, drawn with PixiJS from compact referee strings (BoardModule.java).
// Pixi objects live in pools inside this module, so they are not GEM entities (budget <= 200).
// Phases follow UX 3: placement grows over 0-0.3, mirror turns over 0-0.3, beams and chips snap at 0.3,
// pending, wear and lock rings change at 0.6 (a broken mirror disappears at 0.6).

const COLOUR = [0x56b4e9, 0xe69f00, 0x009e73, 0xcc79a7, 0xf4f6ff]
const WHITE = 0xf4f6ff
const LOCK_OFF = 0x55617a
const BEAM = ['beam_solid_glow.png', 'beam_dash_glow.png', 'beam_dots_glow.png', 'beam_double_glow.png']
const CHIP_W = 36
const CHIP_H = 40
const CHIP_FONT = 36

export function parseRecords (s) {
  return s ? s.split(';').map(r => r.split(',').map(Number)) : []
}

export function parsePoints (s) {
  const v = s ? s.split(',').map(Number) : []
  const pts = []
  for (let i = 0; i + 1 < v.length; i += 2) pts.push([v[i], v[i + 1]])
  return pts
}

function easeOut (t) {
  return 1 - (1 - t) * (1 - t)
}

function clamp01 (t) {
  return Math.max(0, Math.min(1, t))
}

function toggle (name) {
  return ToggleModule.toggles[name] !== false
}

function sprite (image, w, h) {
  const s = new PIXI.Sprite(PIXI.Texture.from(image))
  s.anchor.set(0.5)
  s.width = w
  s.height = h
  return s
}

export class BoardModule {
  constructor (assets) {
    this.empty = { m: '', c: '', b: ['', '', '', ''] }
    this.state = this.empty
    BoardModule.instance = this
    const refresh = ToggleModule.refreshContent
    if (!refresh.board) {
      ToggleModule.refreshContent = () => {
        refresh()
        if (BoardModule.instance) BoardModule.instance.redraw()
      }
      ToggleModule.refreshContent.board = true
    }
  }

  static get moduleName () {
    return 'board'
  }

  static get dependencies () {
    return ['entitymodule']
  }

  handleGlobalData (players, data) {
    if (!data) return
    const [n, cell, x0, y0, beamWidth, playerCount] = data.l
    Object.assign(this, { n, cell, x0, y0, beamWidth, playerCount, layerIds: data.z })
    this.state = { m: data.m0 || '', c: '', b: ['', '', '', ''] }
  }

  handleFrameData (frameInfo, data) {
    const prev = this.state
    const cur = { m: prev.m, c: prev.c, b: prev.b.slice() }
    if (data) {
      if (data.m !== undefined) cur.m = data.m
      if (data.c !== undefined) cur.c = data.c
      for (let k = 0; k < 4; k++) if (data['b' + k] !== undefined) cur.b[k] = data['b' + k]
    }
    this.state = cur
    return { number: frameInfo.number, prev, cur }
  }

  reinitScene (container, canvasData) {
    this.layers = [new PIXI.Container(), new PIXI.Container(), new PIXI.Container()]
    this.beams = [[], [], [], []]
    this.mirrors = []
    this.chips = []
  }

  updateScene (previousData, currentData, progress) {
    this.frame = currentData
    this.progress = progress
    this.redraw()
  }

  redraw () {
    // the toggle module redraws during its own reinitScene, before ours replaced the layers the old stage destroyed
    if (!this.frame || !this.layers || !this.layerIds || this.layers.some(l => l.destroyed)) return
    if (!this.attach()) return
    const p = this.progress
    const f = this.frame
    this.drawBeams(p < 0.3 ? f.prev.b : f.cur.b)
    this.drawMirrors(f.prev.m, f.cur.m, p)
    this.drawChips(p < 0.3 ? f.prev.c : f.cur.c)
  }

  /** The three layers sit in empty GEM groups (z 41, 60, 62); a GEM re-init makes new containers, so re-attach. */
  attach () {
    for (let k = 0; k < 3; k++) {
      const e = entityModule.entities && entityModule.entities.get(this.layerIds[k])
      if (!e || !e.container) return false
      const layer = this.layers[k]
      if (layer.parent !== e.container) e.container.addChild(layer)
      layer.scale.set(entityModule.toWorldUnits || 1)
    }
    return true
  }

  drawBeams (strings) {
    const h = 3 * this.beamWidth
    for (let id = 0; id < 4; id++) {
      const pool = this.beams[id]
      const pts = id < this.playerCount && toggle('beam' + id) ? parsePoints(strings[id]) : []
      const segs = Math.max(0, pts.length - 1)
      while (pool.length < segs) {
        const t = new PIXI.TilingSprite(PIXI.Texture.from(BEAM[id]), 1, h)
        t.anchor.set(0, 0.5)
        t.tint = COLOUR[id]
        t.tileScale.set(h / 48)
        this.layers[0].addChild(t)
        pool.push(t)
      }
      pool.forEach((t, k) => {
        t.visible = k < segs
        if (!t.visible) return
        const [ax, ay] = pts[k]
        const [bx, by] = pts[k + 1]
        t.position.set(ax, ay)
        t.width = Math.max(1, Math.round(Math.hypot(bx - ax, by - ay)))
        t.rotation = Math.atan2(by - ay, bx - ax)
      })
    }
  }

  mirrorView () {
    const s = this.cell
    const v = { root: new PIXI.Container(), bar: new PIXI.Container() }
    v.base = sprite('mirror_base.png', s, s)
    v.barSprite = sprite('mirror_bar.png', s, s)
    v.crack1 = sprite('mirror_crack_1.png', s, s)
    v.crack2 = sprite('mirror_crack_2.png', s, s)
    v.pending = sprite('mirror_pending.png', s, s)
    v.bar.addChild(v.barSprite, v.crack1, v.crack2)
    v.root.addChild(v.base, v.bar, v.pending)
    v.arcs = []
    for (let k = 0; k < 5; k++) {
      const a = sprite('lock_arc.png', s, s)
      a.rotation = k * 2 * Math.PI / 5
      v.root.addChild(a)
      v.arcs.push(a)
    }
    this.layers[1].addChild(v.root)
    return v
  }

  drawMirrors (prevString, curString, p) {
    const prev = new Map(parseRecords(prevString).map(r => [r[0] + ',' + r[1], r]))
    const cur = parseRecords(curString)
    const shown = []
    for (const r of cur) {
      const before = prev.get(r[0] + ',' + r[1])
      prev.delete(r[0] + ',' + r[1])
      if (!before) shown.push({ r, state: r, scale: easeOut(clamp01(p / 0.3)), from: r[2] })
      else shown.push({ r, state: p < 0.6 ? before : r, scale: 1, from: before[2] })
    }
    // broken this turn: the previous mirror stays until 0.6
    if (p < 0.6) for (const r of prev.values()) shown.push({ r, state: r, scale: 1, from: r[2] })
    while (this.mirrors.length < shown.length) this.mirrors.push(this.mirrorView())
    const locks = toggle('locks')
    this.mirrors.forEach((v, k) => {
      v.root.visible = k < shown.length
      if (!v.root.visible) return
      const { r, state, scale, from } = shown[k]
      const [x, y, type] = r
      const [, , , pending, toucher, wear, arcs] = state
      v.root.position.set(this.x0 + (x + 0.5) * this.cell, this.y0 + (y + 0.5) * this.cell)
      v.root.scale.set(Math.max(1e-3, scale))
      const a0 = from === 0 ? 0 : Math.PI / 2
      const a1 = type === 0 ? 0 : Math.PI / 2
      v.bar.rotation = a0 + (a1 - a0) * easeOut(clamp01(p / 0.3))
      const colour = toucher >= 0 ? COLOUR[toucher] : WHITE
      v.pending.visible = pending === 1
      v.pending.tint = colour
      v.barSprite.alpha = pending === 1 ? 0.4 : wear === 2 ? 0.65 : 1
      v.crack1.visible = pending !== 1 && wear === 1
      v.crack2.visible = pending !== 1 && wear === 2
      v.arcs.forEach((a, i) => {
        a.visible = locks && arcs > 0
        a.tint = i < arcs ? colour : LOCK_OFF
      })
    })
  }

  chipView () {
    const v = { root: new PIXI.Container() }
    v.solid = sprite('streak_badge.png', CHIP_W, CHIP_H)
    v.dashed = sprite('streak_badge_dashed.png', CHIP_W, CHIP_H)
    v.text = new PIXI.Text('', { fontFamily: 'Arial', fontSize: CHIP_FONT, fontWeight: 'bold', fill: WHITE })
    v.text.anchor.set(0.5)
    v.root.addChild(v.solid, v.dashed, v.text)
    this.layers[2].addChild(v.root)
    return v
  }

  drawChips (s) {
    const chips = parseRecords(s)
    while (this.chips.length < chips.length) this.chips.push(this.chipView())
    this.chips.forEach((v, k) => {
      v.root.visible = k < chips.length
      if (!v.root.visible) return
      const [x, y, digit, colour, dashed, dim] = chips[k]
      v.root.position.set(this.x0 + (x + 1) * this.cell, this.y0 + (y + 1) * this.cell)
      v.solid.visible = dashed !== 1
      v.dashed.visible = dashed === 1
      v.solid.tint = v.dashed.tint = COLOUR[colour]
      v.root.alpha = dim === 1 ? 0.6 : 1
      if (v.text.text !== String(digit)) v.text.text = String(digit)
    })
  }
}
