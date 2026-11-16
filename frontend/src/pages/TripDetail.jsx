import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { Button, Empty, fmtDate } from '../components/ui.jsx'
import { TripForm } from './Dashboard.jsx'
import Overview from '../tabs/Overview.jsx'
import Itinerary from '../tabs/Itinerary.jsx'
import Reservations from '../tabs/Reservations.jsx'
import Expenses from '../tabs/Expenses.jsx'
import Documents from '../tabs/Documents.jsx'

const TABS = [['overview', 'Overview'], ['itinerary', 'Itinerary'], ['reservations', 'Reservations'], ['expenses', 'Expenses'], ['documents', 'Documents']]

export default function TripDetail() {
  const { tripId } = useParams()
  const { user } = useAuth()
  const nav = useNavigate()
  const [trip, setTrip] = useState(null)
  const [error, setError] = useState('')
  const [tab, setTab] = useState('overview')
  const [editing, setEditing] = useState(false)
  const refresh = useCallback(() => api.getTrip(tripId).then(setTrip).catch((e) => setError(e.message)), [tripId])
  useEffect(() => { refresh() }, [refresh])

  if (error) return <Empty>{error} — <Link className="text-teal-700 underline" to="/">back</Link></Empty>
  if (!trip) return <Empty>Loading…</Empty>
  const isAdmin = trip.adminId === user.userId
  const ctx = { trip, user, isAdmin, refresh }
  // run an API call then reload trip data
  ctx.run = async (fn) => { await fn(); await refresh() }

  return (
    <>
      <Link to="/" className="text-sm text-slate-500 hover:text-teal-700">← All trips</Link>
      <div className="flex flex-wrap items-start justify-between gap-3 mt-2 mb-4">
        <div>
          <h1 className="text-2xl font-bold">{trip.tripName}</h1>
          <p className="text-sm text-slate-500">{fmtDate(trip.startDate)} → {fmtDate(trip.endDate)} · Admin: {trip.admin?.name}</p>
        </div>
        {isAdmin && (
          <div className="flex gap-2">
            <Button variant="ghost" onClick={() => setEditing(true)}>Edit</Button>
            <Button variant="danger" onClick={async () => { if (confirm('Delete this trip and all its data?')) { await api.deleteTrip(tripId); nav('/') } }}>Delete</Button>
          </div>
        )}
      </div>
      <div className="flex gap-1 border-b border-slate-200 mb-5 overflow-x-auto">
        {TABS.map(([k, label]) => (
          <button key={k} onClick={() => setTab(k)}
            className={`px-4 py-2 text-sm font-medium whitespace-nowrap border-b-2 -mb-px ${tab === k ? 'border-teal-600 text-teal-700' : 'border-transparent text-slate-500 hover:text-slate-800'}`}>
            {label}
          </button>
        ))}
      </div>
      {tab === 'overview' && <Overview {...ctx} />}
      {tab === 'itinerary' && <Itinerary {...ctx} />}
      {tab === 'reservations' && <Reservations {...ctx} />}
      {tab === 'expenses' && <Expenses {...ctx} />}
      {tab === 'documents' && <Documents {...ctx} />}
      {editing && <TripForm title="Edit trip" initial={trip} onClose={() => setEditing(false)}
        onSubmit={(f) => ctx.run(() => api.updateTrip(tripId, f))} />}
    </>
  )
}
