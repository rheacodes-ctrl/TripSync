import { useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { Eye, EyeOff, Map, Receipt, Users, ShieldCheck, UserRound } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
import { Button, ErrorNote, Field, Input, Logo } from '../components/ui.jsx'
import { HeroScene } from '../components/Scene.jsx'
import { isMock } from '../api/client.js'

const POINTS = [
  [Map, 'One itinerary for everyone', 'Destinations, activities and votes in a shared plan.'],
  [Receipt, 'Costs split without arguments', 'Equal or custom shares, and who owes whom.'],
  [Users, 'Everyone stays in the loop', 'Invitations and updates by email and WhatsApp.'],
]

const ROLES = [
  ['TRAVELER', UserRound, 'Traveler', 'Join trips, vote on activities and share expenses.'],
  ['TRIP_ADMIN', ShieldCheck, 'Trip admin', 'Create trips, manage members and bookings.'],
]

export default function Login() {
  const { user, login, register } = useAuth()
  const nav = useNavigate()
  const [params] = useSearchParams()
  const [mode, setMode] = useState(params.get('mode') === 'register' ? 'register' : 'login')
  const [f, setF] = useState({ name: '', email: '', password: '', phone: '', role: 'TRAVELER' })
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const [show, setShow] = useState(false)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })
  if (user) return <Navigate to="/trips" replace />

  const run = async (fn) => {
    setBusy(true); setErr('')
    try { await fn(); nav('/trips') } catch (x) { setErr(x.message); setBusy(false) }
  }
  const submit = (e) => {
    e.preventDefault()
    if (mode === 'register' && f.password.length < 6) return setErr('Password must be at least 6 characters.')
    run(() => (mode === 'login' ? login(f.email, f.password) : register(f)))
  }

  return (
    <div className="min-h-screen grid lg:grid-cols-[1.1fr_1fr]">
      {/* Travel image panel. Drop a photo at public/hero.jpg to layer it over the illustration. */}
      <aside className="relative overflow-hidden min-h-56 lg:min-h-screen">
        <HeroScene className="absolute inset-0 w-full h-full" />
        <div className="absolute inset-0 bg-cover bg-center" style={{ backgroundImage: 'url(/hero.jpg)' }} />
        <div className="absolute inset-0 bg-gradient-to-t from-ink/75 via-ink/5 to-transparent" />
        <div className="relative h-full flex flex-col justify-between p-6 lg:p-10">
          <Link to="/" aria-label="TripSync home"><Logo light /></Link>
          <div className="text-white max-w-md">
            <h2 className="font-display text-3xl lg:text-5xl font-semibold leading-[1.05]">Wherever you’re headed, get there together.</h2>
            <ul className="hidden lg:block mt-8 space-y-4">
              {POINTS.map(([Icon, t, d]) => (
                <li key={t} className="flex gap-3">
                  <span className="grid place-items-center w-9 h-9 rounded-lg bg-white/15 backdrop-blur shrink-0"><Icon size={18} /></span>
                  <div><p className="font-medium">{t}</p><p className="text-sm text-white/75">{d}</p></div>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </aside>

      <main className="flex items-center justify-center p-6 sm:p-10 bg-white">
        <div className="w-full max-w-sm">
          <h1 className="font-display text-3xl font-semibold">{mode === 'login' ? 'Welcome back' : 'Create your account'}</h1>
          <p className="text-ink-soft mt-1.5 mb-6">{mode === 'login' ? 'Log in to see your trips.' : 'Start planning your next trip in a minute.'}</p>

          <form onSubmit={submit} className="space-y-4">
            {mode === 'register' && <>
              <div className="grid grid-cols-2 gap-2" role="radiogroup" aria-label="Account type">
                {ROLES.map(([k, Icon, label, desc]) => (
                  <button type="button" key={k} role="radio" aria-checked={f.role === k} onClick={() => setF({ ...f, role: k })}
                    className={`text-left rounded-xl border p-3 transition-colors ${f.role === k ? 'border-lagoon-600 bg-lagoon-50 ring-1 ring-lagoon-600' : 'border-line hover:bg-mist'}`}>
                    <Icon size={18} className={f.role === k ? 'text-lagoon-600' : 'text-ink-soft'} />
                    <p className="text-sm font-medium mt-1.5">{label}</p>
                    <p className="text-xs text-ink-soft mt-0.5 leading-snug">{desc}</p>
                  </button>
                ))}
              </div>
              <Field label="Full name"><Input required autoComplete="name" value={f.name} onChange={set('name')} /></Field>
              <Field label="Phone" hint="Used for WhatsApp trip updates. Optional."><Input type="tel" autoComplete="tel" value={f.phone} onChange={set('phone')} /></Field>
            </>}
            <Field label="Email"><Input type="email" required autoComplete="email" value={f.email} onChange={set('email')} /></Field>
            <Field label="Password">
              <div className="relative">
                <Input type={show ? 'text' : 'password'} required autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={f.password} onChange={set('password')} />
                <button type="button" onClick={() => setShow(!show)} aria-label={show ? 'Hide password' : 'Show password'} className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-ink">
                  {show ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </Field>
            <ErrorNote message={err} />
            <Button disabled={busy} size="lg" className="w-full">{mode === 'login' ? 'Log in' : 'Create account'}</Button>
          </form>

          <p className="text-sm text-ink-soft mt-5">
            {mode === 'login' ? 'New to TripSync? ' : 'Already have an account? '}
            <button className="text-lagoon-700 font-medium hover:underline" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setErr('') }}>
              {mode === 'login' ? 'Create an account' : 'Log in'}
            </button>
          </p>

          {isMock && mode === 'login' && (
            <div className="mt-8 rounded-xl border border-dashed border-line p-4">
              <p className="text-sm font-medium">Try the demo</p>
              <p className="text-xs text-ink-soft mt-0.5 mb-3">Sample accounts from seed.sql. Nothing leaves your browser.</p>
              <div className="flex gap-2">
                <Button type="button" variant="ghost" size="sm" disabled={busy} onClick={() => run(() => login('ishi@example.com', 'admin123'))}>Log in as admin</Button>
                <Button type="button" variant="ghost" size="sm" disabled={busy} onClick={() => run(() => login('rhea@example.com', 'password123'))}>Log in as traveler</Button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
