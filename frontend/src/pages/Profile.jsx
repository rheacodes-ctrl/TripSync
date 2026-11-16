import { useState } from 'react'
import { Check, Mail, MessageCircle, Plus, ShieldCheck, UserRound } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
import { Avatar, Badge, Button, Card, ErrorNote, Field, Input } from '../components/ui.jsx'

const SUGGESTED = ['Beaches', 'Mountains', 'Heritage walks', 'Adventure sports', 'Street food', 'Vegetarian', 'Nightlife', 'Photography', 'Budget stays', 'Slow mornings']

// Same abstract User, two behaviours: the role decides what the app lets you do.
const CAPABILITIES = {
  TRIP_ADMIN: { icon: ShieldCheck, title: 'Trip admin', items: ['Create, edit and delete trips', 'Add and remove travelers', 'Add destinations and manage reservations', 'Change activity status and send reminders'] },
  TRAVELER: { icon: UserRound, title: 'Traveler', items: ['Join trips you are invited to', 'Propose activities, vote and join them', 'Add shared expenses and settle up', 'View itinerary, bookings and documents'] },
}

export default function Profile() {
  const { user, updateProfile } = useAuth()
  const [name, setName] = useState(user.name)
  const [phone, setPhone] = useState(user.phone || '')
  const [prefs, setPrefs] = useState(user.preferences || [])
  const [via, setVia] = useState(user.notifyVia || ['EMAIL'])
  const [custom, setCustom] = useState('')
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const cap = CAPABILITIES[user.role]
  const togglePref = (p) => setPrefs((s) => (s.includes(p) ? s.filter((x) => x !== p) : [...s, p]))
  const toggleVia = (c) => setVia((s) => (s.includes(c) ? s.filter((x) => x !== c) : [...s, c]))
  const addCustom = () => { const v = custom.trim(); if (v && !prefs.includes(v)) setPrefs([...prefs, v]); setCustom('') }

  const save = async (e) => {
    e.preventDefault(); setErr(''); setMsg('')
    if (via.includes('WHATSAPP') && !phone.trim()) return setErr('Add a phone number to receive WhatsApp updates.')
    setBusy(true)
    try { await updateProfile({ name, phone, preferences: prefs, notifyVia: via }); setMsg('Profile saved.') } catch (x) { setErr(x.message) }
    setBusy(false)
  }

  return (
    <div className="grid lg:grid-cols-[320px_1fr] gap-6 items-start">
      <Card className="p-6">
        <Avatar name={user.name} size="xl" />
        <h1 className="font-display font-semibold text-2xl mt-4">{user.name}</h1>
        <p className="text-sm text-ink-soft">{user.email}</p>
        <div className="mt-3"><Badge tone={user.role === 'TRIP_ADMIN' ? 'sun' : 'blue'} icon={cap.icon}>{cap.title}</Badge></div>
        <div className="border-t border-line mt-5 pt-5">
          <p className="text-sm font-medium">What you can do</p>
          <ul className="mt-3 space-y-2">
            {cap.items.map((i) => <li key={i} className="flex gap-2 text-sm text-ink-soft"><Check size={16} className="text-lagoon-600 shrink-0 mt-0.5" />{i}</li>)}
          </ul>
        </div>
      </Card>

      <form onSubmit={save} className="space-y-6">
        <Card className="p-6 space-y-4">
          <h2 className="font-display font-semibold text-xl">Personal details</h2>
          <div className="grid sm:grid-cols-2 gap-4">
            <Field label="Full name"><Input required value={name} onChange={(e) => setName(e.target.value)} /></Field>
            <Field label="Phone" hint="Used for WhatsApp updates."><Input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} /></Field>
            <Field label="Email"><Input value={user.email} disabled /></Field>
            <Field label="Account type"><Input value={cap.title} disabled /></Field>
          </div>
        </Card>

        <Card className="p-6">
          <h2 className="font-display font-semibold text-xl">Travel preferences</h2>
          <p className="text-sm text-ink-soft mt-1">Your group can see these when planning activities and food.</p>
          <div className="flex flex-wrap gap-2 mt-4">
            {[...new Set([...SUGGESTED, ...prefs])].map((p) => (
              <button type="button" key={p} aria-pressed={prefs.includes(p)} onClick={() => togglePref(p)}
                className={`px-3 py-1.5 rounded-full text-sm border transition-colors ${prefs.includes(p) ? 'bg-lagoon-600 border-lagoon-600 text-white' : 'bg-white border-line text-ink-soft hover:border-lagoon-500'}`}>{p}</button>
            ))}
          </div>
          <div className="flex gap-2 mt-4 max-w-sm">
            <Input value={custom} onChange={(e) => setCustom(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addCustom())} placeholder="Add your own" aria-label="Add a preference" />
            <Button type="button" variant="ghost" onClick={addCustom}><Plus size={16} />Add</Button>
          </div>
        </Card>

        <Card className="p-6">
          <h2 className="font-display font-semibold text-xl">Notifications</h2>
          <p className="text-sm text-ink-soft mt-1">Choose where invitations, booking updates and expense reminders are sent.</p>
          <div className="grid sm:grid-cols-2 gap-3 mt-4">
            {[['EMAIL', Mail, 'Gmail', user.email], ['WHATSAPP', MessageCircle, 'WhatsApp', phone || 'Add a phone number']].map(([k, Icon, label, sub]) => (
              <button type="button" key={k} role="switch" aria-checked={via.includes(k)} onClick={() => toggleVia(k)}
                className={`flex items-center gap-3 rounded-xl border p-4 text-left transition-colors ${via.includes(k) ? 'border-lagoon-600 bg-lagoon-50' : 'border-line hover:bg-mist'}`}>
                <Icon size={20} className={via.includes(k) ? 'text-lagoon-600' : 'text-ink-soft'} />
                <span className="flex-1 min-w-0"><span className="block text-sm font-medium">{label}</span><span className="block text-xs text-ink-soft truncate">{sub}</span></span>
                <span className={`w-9 h-5 rounded-full relative transition-colors ${via.includes(k) ? 'bg-lagoon-600' : 'bg-slate-300'}`}><span className={`absolute top-0.5 w-4 h-4 rounded-full bg-white transition-all ${via.includes(k) ? 'left-[18px]' : 'left-0.5'}`} /></span>
              </button>
            ))}
          </div>
        </Card>

        <ErrorNote message={err} />
        <div className="flex items-center gap-4">
          <Button disabled={busy} size="lg">Save changes</Button>
          {msg && <span className="text-sm text-emerald-700 inline-flex items-center gap-1.5" role="status"><Check size={16} />{msg}</span>}
        </div>
      </form>
    </div>
  )
}
