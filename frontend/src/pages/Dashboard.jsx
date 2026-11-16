import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { Button, Card, Empty, Field, FormModal, Input, Textarea, fmtDate, inr } from '../components/ui.jsx'

export default function Dashboard() {
  const { user } = useAuth()
  const [trips, setTrips] = useState(null)
  const [open, setOpen] = useState(false)
  const load = useCallback(() => api.listTrips(user.userId).then(setTrips), [user.userId])
  useEffect(() => { load() }, [load])

  return (
    <>
      <div className="flex items-center justify-between mb-5">
        <div>
          <h1 className="text-2xl font-bold">Your trips</h1>
          <p className="text-sm text-slate-500">Hi {user.name}, where to next?</p>
        </div>
        {user.role === 'TRIP_ADMIN' && <Button onClick={() => setOpen(true)}>+ New trip</Button>}
      </div>
      {!trips ? <Empty>Loading…</Empty> : trips.length === 0 ? (
        <Card><Empty>{user.role === 'TRIP_ADMIN' ? 'No trips yet — create your first one.' : 'You have not been added to any trips yet. Ask a trip admin to add your email.'}</Empty></Card>
      ) : (
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {trips.map((t) => {
            const pct = Math.min(100, Math.round((t.spent / t.budget) * 100))
            return (
              <Link key={t.tripId} to={`/trips/${t.tripId}`}>
                <Card className="p-4 hover:shadow-md hover:border-teal-300 transition h-full">
                  <h2 className="font-semibold text-lg">{t.tripName}</h2>
                  <p className="text-sm text-slate-500">{fmtDate(t.startDate)} → {fmtDate(t.endDate)}</p>
                  <p className="text-sm text-slate-600 mt-2 line-clamp-2">{t.description}</p>
                  <div className="mt-4 h-2 bg-slate-100 rounded-full overflow-hidden">
                    <div className={`h-full ${pct >= 100 ? 'bg-red-500' : 'bg-teal-500'}`} style={{ width: pct + '%' }} />
                  </div>
                  <div className="flex justify-between text-xs text-slate-500 mt-1">
                    <span>{inr(t.spent)} of {inr(t.budget)}</span>
                    <span>{t.memberCount} members · {t.destinationCount} stops</span>
                  </div>
                </Card>
              </Link>
            )
          })}
        </div>
      )}
      {open && <TripForm title="New trip" onClose={() => setOpen(false)}
        onSubmit={(f) => api.createTrip({ ...f, adminId: user.userId }).then(load)} />}
    </>
  )
}

export function TripForm({ title, onClose, onSubmit, initial = { tripName: '', description: '', startDate: '', endDate: '', budget: '' } }) {
  return (
    <FormModal title={title} onClose={onClose} initial={initial}
      onSubmit={(f) => {
        if (f.endDate < f.startDate) throw new Error('End date must be on or after start date')
        if (!(Number(f.budget) > 0)) throw new Error('Budget must be greater than 0')
        return onSubmit(f)
      }}>
      {(f, set) => <>
        <Field label="Trip name"><Input required value={f.tripName} onChange={set('tripName')} /></Field>
        <Field label="Description"><Textarea value={f.description || ''} onChange={set('description')} /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Start"><Input type="date" required value={f.startDate} onChange={set('startDate')} /></Field>
          <Field label="End"><Input type="date" required value={f.endDate} onChange={set('endDate')} /></Field>
        </div>
        <Field label="Budget (₹)"><Input type="number" min="1" step="0.01" required value={f.budget} onChange={set('budget')} /></Field>
      </>}
    </FormModal>
  )
}
