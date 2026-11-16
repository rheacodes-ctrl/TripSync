import { useId } from 'react'

/* Deterministic helpers so every trip gets its own (stable) cover illustration */
export const hash = (str) => { let h = 2166136261; for (const c of String(str)) { h ^= c.charCodeAt(0); h = Math.imul(h, 16777619) } return h >>> 0 }
const rng = (seed) => { let s = seed >>> 0 || 1; return () => (s = (Math.imul(s, 1664525) + 1013904223) >>> 0) / 4294967296 }

function ridge(rand, W, H, base, amp, steps) {
  const p = Array.from({ length: steps + 1 }, (_, i) => [(W / steps) * i, base - rand() * amp])
  let d = `M0 ${H} L0 ${p[0][1].toFixed(1)}`
  for (let i = 1; i < p.length - 1; i++) {
    const mx = (p[i][0] + p[i + 1][0]) / 2, my = (p[i][1] + p[i + 1][1]) / 2
    d += ` Q${p[i][0].toFixed(1)} ${p[i][1].toFixed(1)} ${mx.toFixed(1)} ${my.toFixed(1)}`
  }
  return d + ` L${W} ${p[p.length - 1][1].toFixed(1)} L${W} ${H} Z`
}

const PALETTES = [
  { sky: ['#2b2f77', '#f27b5e', '#f9c86b'], sun: '#ffe6a8', layers: ['#6b4e9b', '#2e6f8e', '#0e5560'] }, // sunset over water
  { sky: ['#16324f', '#4f86a8', '#f2d49b'], sun: '#fff3cf', layers: ['#7fa6bf', '#3f7691', '#1d4e5f'] }, // mountain dawn
  { sky: ['#0b4f6c', '#1fa2a6', '#bfe8d5'], sun: '#fffbe6', layers: ['#58b4ae', '#1b8a8f', '#0b5d63'] }, // lagoon
  { sky: ['#3a1c4a', '#c4506b', '#f7b267'], sun: '#ffe2b8', layers: ['#8a3f6b', '#5b2a5e', '#2d1b45'] }, // dusk
]

/** Small illustrated landscape used as a trip cover. */
export function CoverArt({ seed = 'trip', className = '' }) {
  const id = useId().replace(/:/g, '')
  const h = hash(seed)
  const pal = PALETTES[h % PALETTES.length]
  const rand = rng(h)
  const W = 400, H = 240
  const sunX = 80 + rand() * 240
  return (
    <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="xMidYMid slice" className={className} aria-hidden="true">
      <defs>
        <linearGradient id={`s${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor={pal.sky[0]} /><stop offset=".6" stopColor={pal.sky[1]} /><stop offset="1" stopColor={pal.sky[2]} />
        </linearGradient>
      </defs>
      <rect width={W} height={H} fill={`url(#s${id})`} />
      <circle cx={sunX} cy={118} r={34} fill={pal.sun} opacity=".25" />
      <circle cx={sunX} cy={118} r={20} fill={pal.sun} />
      <path d={ridge(rand, W, H, 150, 60, 7)} fill={pal.layers[0]} opacity=".9" />
      <path d={ridge(rand, W, H, 180, 50, 8)} fill={pal.layers[1]} />
      <path d={ridge(rand, W, H, 212, 36, 6)} fill={pal.layers[2]} />
    </svg>
  )
}

/** Large hero illustration: sunset, headland, sea, sailboat and a plane trail. */
export function HeroScene({ className = '' }) {
  const id = useId().replace(/:/g, '')
  const rand = rng(11)
  const stars = Array.from({ length: 34 }, () => [rand() * 1200, rand() * 260, 0.8 + rand() * 1.4, 0.25 + rand() * 0.5])
  const far = ridge(rng(5), 1200, 800, 540, 90, 10)
  const mid = ridge(rng(9), 1200, 800, 585, 70, 9)
  const near = 'M0 800V610Q90 575 190 600T360 650Q470 690 600 800Z'
  return (
    <svg viewBox="0 0 1200 800" preserveAspectRatio="xMidYMid slice" className={className} aria-hidden="true">
      <defs>
        <linearGradient id={`sky${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#161a55" /><stop offset=".32" stopColor="#6a3a8a" />
          <stop offset=".6" stopColor="#ef6f5c" /><stop offset=".74" stopColor="#fbc56b" />
        </linearGradient>
        <linearGradient id={`sea${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#e98a5d" /><stop offset=".22" stopColor="#2c6e86" /><stop offset="1" stopColor="#09303f" />
        </linearGradient>
        <radialGradient id={`glow${id}`}>
          <stop offset="0" stopColor="#fff1c4" stopOpacity=".95" /><stop offset=".45" stopColor="#ffd37e" stopOpacity=".4" /><stop offset="1" stopColor="#ffd37e" stopOpacity="0" />
        </radialGradient>
      </defs>
      <rect width="1200" height="800" fill={`url(#sky${id})`} />
      {stars.map(([x, y, r, o], i) => <circle key={i} cx={x} cy={y} r={r} fill="#fff" opacity={o} />)}
      <circle cx="780" cy="500" r="260" fill={`url(#glow${id})`} />
      <circle cx="780" cy="500" r="92" fill="#ffefc0" />
      <path d={far} fill="#7b4f9a" opacity=".85" />
      <path d={mid} fill="#4b3d86" />
      {/* sea */}
      <rect y="585" width="1200" height="215" fill={`url(#sea${id})`} />
      {[0, 1, 2, 3, 4, 5, 6].map((i) => (
        <rect key={i} x={780 - (150 - i * 18) / 2} y={598 + i * 24} width={150 - i * 18} height="5" rx="2.5" fill="#ffe3a3" opacity={0.75 - i * 0.09} />
      ))}
      {/* headland */}
      <path d={near} fill="#0a3a47" />
      {/* sailboat */}
      <g transform="translate(420 590)">
        <path d="M0 40h96l-14 18H16z" fill="#102b3a" />
        <path d="M46 36V-62L8 30z" fill="#fff4dc" />
        <path d="M52 36V-40l34 70z" fill="#f4a340" />
        <path d="M49 38V-70" stroke="#102b3a" strokeWidth="3" />
      </g>
      {/* plane + trail */}
      <path d="M90 300C260 250 420 190 640 120" fill="none" stroke="#fff" strokeOpacity=".5" strokeWidth="2.5" strokeDasharray="3 11" strokeLinecap="round" />
      <g className="plane-drift">
        <g transform="translate(664 108) rotate(-18)" fill="#fff">
          <path d="M0 0c6-6 22-8 36-8l-3 8 3 8c-14 0-30-2-36-8z" />
          <path d="M14-6-2-24h8l18 18z" /><path d="M14 6-2 24h8l18-18z" />
        </g>
      </g>
    </svg>
  )
}
