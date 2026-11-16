import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { Button, Card, Field, Input, Select } from '../components/ui.jsx'
import { isMock } from '../api/client.js'

export default function Login() {
  const { user, login, register } = useAuth()
  const nav = useNavigate()
  const [mode, setMode] = useState('login')
  const [f, setF] = useState({ name: '', email: '', password: '', phone: '', role: 'TRAVELER' })
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  if (user) return <Navigate to="/" replace />

  const submit = async (e) => {
    e.preventDefault(); setBusy(true); setErr('')
    try {
      mode === 'login' ? await login(f.email, f.password) : await register(f)
      nav('/')
    } catch (x) { setErr(x.message); setBusy(false) }
  }
  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-gradient-to-br from-teal-50 to-sky-100">
      <Card className="w-full max-w-sm p-6">
        <h1 className="text-2xl font-bold text-teal-700">🧭 TripSync</h1>
        <p className="text-sm text-slate-500 mb-5">Plan trips together. Split costs fairly.</p>
        <form onSubmit={submit} className="space-y-3">
          {mode === 'register' && <>
            <Field label="Name"><Input required value={f.name} onChange={set('name')} /></Field>
            <Field label="Phone"><Input value={f.phone} onChange={set('phone')} /></Field>
            <Field label="Role">
              <Select value={f.role} onChange={set('role')}>
                <option value="TRAVELER">Traveler</option><option value="TRIP_ADMIN">Trip admin</option>
              </Select>
            </Field>
          </>}
          <Field label="Email"><Input type="email" required value={f.email} onChange={set('email')} /></Field>
          <Field label="Password"><Input type="password" required value={f.password} onChange={set('password')} /></Field>
          {err && <p className="text-sm text-red-600">{err}</p>}
          <Button disabled={busy} className="w-full">{mode === 'login' ? 'Log in' : 'Create account'}</Button>
        </form>
        <button className="text-sm text-teal-700 mt-4 hover:underline" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setErr('') }}>
          {mode === 'login' ? 'New here? Create an account' : 'Have an account? Log in'}
        </button>
        {isMock && mode === 'login' && (
          <div className="mt-4 text-xs bg-slate-50 rounded-lg p-3 text-slate-600">
            <b>Demo accounts</b> (from seed.sql)
            <button type="button" className="block hover:underline" onClick={() => setF({ ...f, email: 'ishi@example.com', password: 'admin123' })}>Ishi (admin) – ishi@example.com / admin123</button>
            <button type="button" className="block hover:underline" onClick={() => setF({ ...f, email: 'rhea@example.com', password: 'password123' })}>Rhea – rhea@example.com / password123</button>
          </div>
        )}
      </Card>
    </div>
  )
}
