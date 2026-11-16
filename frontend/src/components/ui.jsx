import { useEffect, useState } from 'react'
import { AlertTriangle, Compass, X } from 'lucide-react'
import { hash } from './Scene.jsx'

/* ---------- formatting ---------- */
export const inr = (n) => '₹' + Number(n || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })
export const fmtDate = (d) => (d ? new Date(d.slice(0, 10) + 'T00:00:00').toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '')
export const fmtShort = (d) => (d ? new Date(d.slice(0, 10) + 'T00:00:00').toLocaleDateString('en-IN', { day: 'numeric', month: 'short' }) : '')
export const fmtRange = (a, b) => (a === b ? fmtDate(a) : `${fmtShort(a)} – ${fmtDate(b)}`)
export const fmtDateTime = (d) => (d ? new Date(d).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' }) : '')
export const daysBetween = (a, b) => Math.round((new Date(b + 'T00:00:00') - new Date(a + 'T00:00:00')) / 86400000)
export const today = () => new Date().toISOString().slice(0, 10)

/* ---------- brand ---------- */
export function Logo({ light = false, className = '' }) {
  return (
    <span className={`inline-flex items-center gap-2 font-display font-semibold text-xl ${light ? 'text-white' : 'text-ink'} ${className}`}>
      <span className="grid place-items-center w-8 h-8 rounded-lg bg-lagoon-600 text-white"><Compass size={18} strokeWidth={2.2} /></span>
      TripSync
    </span>
  )
}

/* ---------- primitives ---------- */
export const Button = ({ variant = 'primary', size = 'md', className = '', ...p }) => {
  const v = {
    primary: 'bg-lagoon-600 text-white hover:bg-lagoon-700 shadow-sm',
    sun: 'bg-sun-500 text-ink hover:bg-sun-400 shadow-sm',
    ghost: 'bg-white border border-line text-ink hover:bg-mist',
    quiet: 'text-ink-soft hover:bg-lagoon-50 hover:text-lagoon-700',
    danger: 'bg-white border border-coral-500/30 text-coral-600 hover:bg-coral-50',
    onDark: 'bg-white/15 text-white border border-white/30 hover:bg-white/25 backdrop-blur',
  }[variant]
  const s = { sm: 'px-2.5 py-1.5 text-xs gap-1.5', md: 'px-4 py-2.5 text-sm gap-2', lg: 'px-5 py-3 text-base gap-2' }[size]
  return <button className={`inline-flex items-center justify-center rounded-lg font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${s} ${v} ${className}`} {...p} />
}

export const Card = ({ className = '', ...p }) => <div className={`${/\bbg-/.test(className) ? '' : 'bg-white'} rounded-2xl border ${/\bborder-(?!line)/.test(className) ? '' : 'border-line'} ${className}`} {...p} />

const tones = {
  green: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20', yellow: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  red: 'bg-coral-50 text-coral-600 ring-coral-500/20', blue: 'bg-lagoon-50 text-lagoon-700 ring-lagoon-600/20',
  gray: 'bg-slate-100 text-slate-600 ring-slate-500/15', sun: 'bg-sun-100 text-sun-600 ring-sun-500/25',
}
export const Badge = ({ tone = 'gray', icon: Icon, children }) => (
  <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ring-1 ring-inset ${tones[tone]}`}>
    {Icon && <Icon size={12} />}{children}
  </span>
)
export const statusTone = (s) => ({ CONFIRMED: 'green', COMPLETED: 'green', SENT: 'green', PENDING: 'yellow', PLANNED: 'blue', CANCELLED: 'red' }[s] || 'gray')
export const statusLabel = (s) => (s ? s[0] + s.slice(1).toLowerCase() : '')

export function Field({ label, hint, children }) {
  return (
    <label className="block text-sm">
      <span className="text-ink font-medium">{label}</span>
      <div className="mt-1.5">{children}</div>
      {hint && <span className="block text-xs text-ink-soft mt-1">{hint}</span>}
    </label>
  )
}
export const inputCls = 'w-full rounded-lg border border-line bg-white px-3 py-2.5 text-sm text-ink placeholder:text-slate-400 focus:outline-none focus:border-lagoon-500 focus:ring-2 focus:ring-lagoon-500/25 disabled:bg-mist'
export const Input = (p) => <input className={inputCls} {...p} />
export const Select = (p) => <select className={inputCls} {...p} />
export const Textarea = (p) => <textarea rows={3} className={inputCls} {...p} />

export function ProgressBar({ value, max, tone, className = '' }) {
  const pct = max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0
  const color = tone || (value > max ? 'bg-coral-500' : pct > 85 ? 'bg-sun-500' : 'bg-lagoon-500')
  return <div className={`h-2 bg-slate-100 rounded-full overflow-hidden ${className}`}><div className={`h-full rounded-full ${color}`} style={{ width: pct + '%' }} /></div>
}

/* ---------- people ---------- */
const AV = ['bg-lagoon-100 text-lagoon-800', 'bg-sun-100 text-sun-600', 'bg-sky-100 text-sky-800', 'bg-rose-100 text-rose-700', 'bg-violet-100 text-violet-700', 'bg-emerald-100 text-emerald-700']
export const Avatar = ({ name = '?', size = 'md', ring = false }) => {
  const sz = { sm: 'w-6 h-6 text-[10px]', md: 'w-8 h-8 text-sm', lg: 'w-12 h-12 text-lg', xl: 'w-16 h-16 text-2xl' }[size]
  return (
    <span title={name} className={`inline-flex shrink-0 rounded-full items-center justify-center font-semibold ${sz} ${AV[hash(name) % AV.length]} ${ring ? 'ring-2 ring-white' : ''}`}>
      {name[0]?.toUpperCase()}
    </span>
  )
}
export const AvatarStack = ({ names = [], max = 4, size = 'sm' }) => (
  <div className="flex -space-x-1.5">
    {names.slice(0, max).map((n, i) => <Avatar key={i} name={n} size={size} ring />)}
    {names.length > max && <span className="inline-flex w-6 h-6 rounded-full bg-slate-100 text-slate-600 text-[10px] font-semibold items-center justify-center ring-2 ring-white">+{names.length - max}</span>}
  </div>
)

/* ---------- empty / header ---------- */
export const Empty = ({ icon: Icon, title, children, action }) => (
  <div className="text-center py-10 px-4">
    {Icon && <span className="inline-grid place-items-center w-12 h-12 rounded-full bg-lagoon-50 text-lagoon-600 mb-3"><Icon size={22} /></span>}
    {title && <p className="font-medium text-ink">{title}</p>}
    {children && <p className="text-sm text-ink-soft mt-1 max-w-sm mx-auto">{children}</p>}
    {action && <div className="mt-4">{action}</div>}
  </div>
)

export const SectionHeader = ({ title, sub, children }) => (
  <div className="flex flex-wrap items-end justify-between gap-3">
    <div><h2 className="font-display font-semibold text-xl">{title}</h2>{sub && <p className="text-sm text-ink-soft mt-0.5">{sub}</p>}</div>
    {children && <div className="flex gap-2">{children}</div>}
  </div>
)

export const Stat = ({ icon: Icon, label, value, sub, tone = 'bg-lagoon-50 text-lagoon-600' }) => (
  <Card className="p-4 flex items-start gap-3">
    {Icon && <span className={`grid place-items-center w-10 h-10 rounded-xl shrink-0 ${tone}`}><Icon size={19} /></span>}
    <div className="min-w-0"><p className="text-sm text-ink-soft">{label}</p><p className="font-display text-2xl font-semibold leading-tight mt-0.5">{value}</p>{sub && <p className="text-xs text-ink-soft mt-0.5">{sub}</p>}</div>
  </Card>
)

/* ---------- modal + form ---------- */
export function Modal({ title, onClose, children, size = 'md' }) {
  useEffect(() => {
    const h = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [onClose])
  const w = { md: 'max-w-md', lg: 'max-w-xl' }[size]
  return (
    <div className="fade-in fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-ink/55 backdrop-blur-[2px] p-0 sm:p-4" onMouseDown={onClose}>
      <div role="dialog" aria-modal="true" aria-label={title} className={`pop-in bg-white sm:rounded-2xl rounded-t-2xl w-full ${w} max-h-[92vh] overflow-auto p-6 shadow-2xl`} onMouseDown={(e) => e.stopPropagation()}>
        <div className="flex justify-between items-center mb-5">
          <h3 className="font-display font-semibold text-xl">{title}</h3>
          <button onClick={onClose} className="text-slate-400 hover:text-ink rounded-md p-1" aria-label="Close"><X size={20} /></button>
        </div>
        {children}
      </div>
    </div>
  )
}

/** Shows backend/validation errors; BudgetExceededException gets its own treatment. */
export function ErrorNote({ message }) {
  if (!message) return null
  const budget = message.startsWith('BudgetExceededException')
  const text = budget ? message.replace(/^BudgetExceededException:?\s*/, '') : message
  return (
    <div role="alert" className={`flex gap-2.5 rounded-lg p-3 text-sm ${budget ? 'bg-sun-100 text-ink border border-sun-500/40' : 'bg-coral-50 text-coral-600 border border-coral-500/20'}`}>
      <AlertTriangle size={17} className="shrink-0 mt-0.5" />
      <div>{budget && <p className="font-medium">Trip budget would be exceeded</p>}<p>{text}</p></div>
    </div>
  )
}

export function FormModal({ title, onClose, onSubmit, initial = {}, children, submitLabel = 'Save', size }) {
  const [f, setF] = useState(initial)
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setF((s) => ({ ...s, [k]: e.target.value }))
  const submit = async (e) => {
    e.preventDefault(); setBusy(true); setErr('')
    try { await onSubmit(f); onClose() } catch (x) { setErr(x.message); setBusy(false) }
  }
  return (
    <Modal title={title} onClose={onClose} size={size}>
      <form onSubmit={submit} className="space-y-4">
        {children(f, set, setF)}
        <ErrorNote message={err} />
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="ghost" onClick={onClose}>Cancel</Button>
          <Button disabled={busy}>{submitLabel}</Button>
        </div>
      </form>
    </Modal>
  )
}

/* small segmented control used for split mode, reservation type etc. */
export function Segmented({ value, onChange, options }) {
  return (
    <div className="inline-flex p-1 bg-mist rounded-lg border border-line w-full">
      {options.map(([k, label]) => (
        <button type="button" key={k} onClick={() => onChange(k)}
          className={`flex-1 px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${value === k ? 'bg-white text-lagoon-700 shadow-sm' : 'text-ink-soft hover:text-ink'}`}>{label}</button>
      ))}
    </div>
  )
}
