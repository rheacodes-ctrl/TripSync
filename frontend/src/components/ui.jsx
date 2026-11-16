import { useEffect } from 'react'

export const inr = (n) => '₹' + Number(n || 0).toLocaleString('en-IN', { maximumFractionDigits: 2 })
export const fmtDate = (d) => (d ? new Date(d + 'T00:00:00').toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '')

export const Button = ({ variant = 'primary', className = '', ...p }) => {
  const v = {
    primary: 'bg-teal-600 text-white hover:bg-teal-700',
    ghost: 'bg-white border border-slate-300 text-slate-700 hover:bg-slate-100',
    danger: 'bg-white border border-red-200 text-red-600 hover:bg-red-50',
  }[variant]
  return <button className={`px-3.5 py-2 rounded-lg text-sm font-medium transition disabled:opacity-50 ${v} ${className}`} {...p} />
}

export const Card = ({ className = '', ...p }) => <div className={`bg-white rounded-xl border border-slate-200 shadow-sm ${className}`} {...p} />

const tones = {
  green: 'bg-emerald-100 text-emerald-700', yellow: 'bg-amber-100 text-amber-700',
  red: 'bg-red-100 text-red-700', blue: 'bg-sky-100 text-sky-700', gray: 'bg-slate-100 text-slate-600',
}
export const Badge = ({ tone = 'gray', children }) => (
  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${tones[tone]}`}>{children}</span>
)
export const statusTone = (s) => ({ CONFIRMED: 'green', COMPLETED: 'green', PENDING: 'yellow', PLANNED: 'blue', CANCELLED: 'red' }[s] || 'gray')

export function Field({ label, children }) {
  return <label className="block text-sm"><span className="text-slate-600 font-medium">{label}</span><div className="mt-1">{children}</div></label>
}
export const inputCls = 'w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500'
export const Input = (p) => <input className={inputCls} {...p} />
export const Select = (p) => <select className={inputCls} {...p} />
export const Textarea = (p) => <textarea rows={3} className={inputCls} {...p} />

export function Modal({ title, onClose, children }) {
  useEffect(() => {
    const h = (e) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [onClose])
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4" onMouseDown={onClose}>
      <div className="bg-white rounded-xl w-full max-w-md max-h-[90vh] overflow-auto p-5 shadow-xl" onMouseDown={(e) => e.stopPropagation()}>
        <div className="flex justify-between items-center mb-4">
          <h3 className="font-semibold text-lg">{title}</h3>
          <button onClick={onClose} className="text-slate-400 hover:text-slate-700 text-xl leading-none" aria-label="Close">×</button>
        </div>
        {children}
      </div>
    </div>
  )
}

export const Empty = ({ children }) => <p className="text-sm text-slate-500 text-center py-8">{children}</p>

export const Avatar = ({ name = '?' }) => (
  <span title={name} className="inline-flex w-8 h-8 rounded-full bg-teal-100 text-teal-700 items-center justify-center text-sm font-semibold">
    {name[0]?.toUpperCase()}
  </span>
)

// Generic "form in a modal" that handles submit + errors.
import { useState } from 'react'
export function FormModal({ title, onClose, onSubmit, initial = {}, children, submitLabel = 'Save' }) {
  const [f, setF] = useState(initial)
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setF((s) => ({ ...s, [k]: e.target.value }))
  const submit = async (e) => {
    e.preventDefault(); setBusy(true); setErr('')
    try { await onSubmit(f); onClose() } catch (x) { setErr(x.message); setBusy(false) }
  }
  return (
    <Modal title={title} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        {children(f, set, setF)}
        {err && <p className="text-sm text-red-600">{err}</p>}
        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" variant="ghost" onClick={onClose}>Cancel</Button>
          <Button disabled={busy}>{submitLabel}</Button>
        </div>
      </form>
    </Modal>
  )
}
