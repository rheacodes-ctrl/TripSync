import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarDays, MapPin, Plane, Plus, Search, Wallet } from 'lucide-react'
import { api } from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { AvatarStack, Button, Card, Empty, Field, FormModal, Input, ProgressBar, Textarea, daysBetween, fmtRange, inr, today } from '../components/ui.jsx'
import { CoverArt } from '../components/Scene.jsx'

const FILTERS = [['all', 'All'], ['upcoming', 'Upcoming'], ['past', 'Past']]

export default function Dashboard() {
  const { user } = useAuth()
  const isAdmin = user.role === 'TRIP_ADMIN'
  const [trips, setTrips] = useState(null)
  const [open, setOpen] = useState(false)
  const [q, setQ] = useState('')
  const [filter, setFilter] = useState('all')
  const load = useCallback(() => api.listTrips(user.userId).then(setTrips), [user.userId])
  useEffect(() => { load() }, [load])

  const shown = useMemo(() => (trips || []).filter((t) =>
    t.tripName.toLowerCase().includes(q.toLowerCase())
    && (filter === 'all' || (filter === 'past' ? t.endDate < today() : t.endDate >= today()))), [trips, q, filter])
  const upcoming = (trips || []).filter((t) => t.endDate >= today()).length
  const totalBudget = (trips || []).reduce((s, t) => s + t.budget, 0)

  return (
    <>
      <section className="relative overflow-hidden rounded-3xl text-white">
        <CoverArt seed="dashboard-banner" className="absolute inset-0 w-full h-full" />
        <div className="absolute inset-0 bg-gradient-to-r from-ink/70 to-ink/10" />
        <div className="relative p-6 sm:p-9 flex flex-wrap items-end justify-between gap-6">
          <div>
            <h1 className="font-display text-3xl sm:text-4xl font-semibold">Hi {user.name}, where to next?</h1>
            <p className="mt-2 text-white/85">{trips ? (upcoming ? `You have ${upcoming} upcoming ${upcoming === 1 ? 'trip' : 'trips'}.` : 'No upcoming trips yet.') : ' '}</p>
            {isAdmin && <Button variant="sun" className="mt-5" onClick={() => setOpen(true)}><Plus size={17} />New trip</Button>}
          </div>
          <dl className="flex gap-3">
            {[[Plane, 'Trips', trips?.length ?? '–'], [Wallet, 'Total budget', trips ? inr(totalBudget) : '–']].map(([Icon, l, v]) => (
              <div key={l} className="rounded-xl bg-white/15 backdrop-blur border border-white/25 px-4 py-3 min-w-28">
                <dt className="flex items-center gap-1.5 text-xs text-white/80"><Icon size={13} />{l}</dt>
                <dd className="font-display text-xl font-semibold mt-0.5">{v}</dd>
              </div>
            ))}
          </dl>
        </div>
      </section>

      <div className="flex flex-wrap items-center justify-between gap-3 mt-8 mb-5">
        <h2 className="font-display font-semibold text-2xl">Your trips</h2>
        <div className="flex flex-wrap gap-3">
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search trips" aria-label="Search trips"
              className="pl-9 pr-3 py-2.5 rounded-lg border border-line bg-white text-sm w-48 focus:outline-none focus:border-lagoon-500 focus:ring-2 focus:ring-lagoon-500/25" />
          </div>
          <div className="inline-flex p-1 bg-white border border-line rounded-lg">
            {FILTERS.map(([k, l]) => (
              <button key={k} onClick={() => setFilter(k)} className={`px-3 py-1.5 rounded-md text-sm font-medium ${filter === k ? 'bg-lagoon-600 text-white' : 'text-ink-soft hover:text-ink'}`}>{l}</button>
            ))}
          </div>
        </div>
      </div>

      {!trips ? <p className="text-center text-ink-soft py-16">Loading trips…</p> : shown.length === 0 ? (
        <Card>
          <Empty icon={MapPin} title={trips.length ? 'No trips match' : 'No trips yet'}
            action={isAdmin && !trips.length && <Button onClick={() => setOpen(true)}><Plus size={16} />Create your first trip</Button>}>
            {trips.length ? 'Try a different search or filter.' : isAdmin ? 'Create a trip, then add your group by email.' : 'You haven’t been added to a trip yet. Ask a trip admin to add your email.'}
          </Empty>
        </Card>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {shown.map((t) => {
            const days = daysBetween(t.startDate, t.endDate) + 1
            return (
              <Link key={t.tripId} to={`/trips/${t.tripId}`} className="group">
                <Card className="overflow-hidden h-full transition-shadow group-hover:shadow-lg">
                  <div className="relative h-36">
                    <CoverArt seed={t.tripName} className="absolute inset-0 w-full h-full" />
                    <span className="absolute top-3 left-3 rounded-full bg-white/90 text-ink text-xs font-medium px-2.5 py-1">{days} {days === 1 ? 'day' : 'days'}</span>
                  </div>
                  <div className="p-5">
                    <h3 className="font-display font-semibold text-xl">{t.tripName}</h3>
                    <p className="flex items-center gap-1.5 text-sm text-ink-soft mt-1"><CalendarDays size={14} />{fmtRange(t.startDate, t.endDate)}</p>
                    <p className="text-sm text-ink-soft mt-2 line-clamp-2 min-h-10">{t.description}</p>
                    <ProgressBar value={t.spent} max={t.budget} className="mt-4" />
                    <div className="flex justify-between text-xs text-ink-soft mt-1.5"><span>{inr(t.spent)} of {inr(t.budget)}</span><span>{t.destinationCount} {t.destinationCount === 1 ? 'stop' : 'stops'}</span></div>
                    <div className="mt-4 pt-4 border-t border-line flex items-center justify-between">
                      <AvatarStack names={(t.members || []).map((m) => m.name)} />
                      <span className="text-xs text-ink-soft">{t.memberCount} {t.memberCount === 1 ? 'traveler' : 'travelers'}</span>
                    </div>
                  </div>
                </Card>
              </Link>
            )
          })}
        </div>
      )}
      {open && <TripForm title="New trip" submitLabel="Create trip" onClose={() => setOpen(false)}
        onSubmit={(f) => api.createTrip({ ...f, adminId: user.userId }).then(load)} />}
    </>
  )
}

export function TripForm({ title, onClose, onSubmit, submitLabel, initial = { tripName: '', description: '', startDate: '', endDate: '', budget: '' } }) {
  return (
    <FormModal title={title} onClose={onClose} initial={initial} submitLabel={submitLabel}
      onSubmit={(f) => {
        if (f.endDate < f.startDate) throw new Error('End date must be on or after the start date.')
        if (!(Number(f.budget) > 0)) throw new Error('Budget must be greater than 0.')
        return onSubmit(f)
      }}>
      {(f, set) => <>
        <Field label="Trip name"><Input required value={f.tripName} onChange={set('tripName')} placeholder="Goa Vacation" /></Field>
        <Field label="Description"><Textarea value={f.description || ''} onChange={set('description')} placeholder="What is this trip about?" /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Start date"><Input type="date" required value={f.startDate} onChange={set('startDate')} /></Field>
          <Field label="End date"><Input type="date" required min={f.startDate} value={f.endDate} onChange={set('endDate')} /></Field>
        </div>
        <Field label="Budget (₹)"><Input type="number" min="1" step="0.01" required value={f.budget} onChange={set('budget')} /></Field>
      </>}
    </FormModal>
  )
}
