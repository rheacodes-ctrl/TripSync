import { useState } from 'react'
import { api } from '../api/client.js'
import { Avatar, Badge, Button, Card, Empty, Field, FormModal, Input, Select, Textarea, fmtDate, inr, statusTone } from '../components/ui.jsx'

export default function Itinerary({ trip, user, isAdmin, run }) {
  const [destForm, setDestForm] = useState(false)
  const [actFor, setActFor] = useState(null) // destination id
  const name = (id) => trip.members.find((m) => m.userId === id)?.name || id

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h2 className="font-semibold text-lg">Destinations & activities</h2>
        {isAdmin && <Button onClick={() => setDestForm(true)}>+ Destination</Button>}
      </div>
      {trip.destinations.length === 0 && <Card><Empty>No destinations yet.</Empty></Card>}
      {trip.destinations.map((d) => (
        <Card key={d.destinationId} className="p-4">
          <div className="flex flex-wrap justify-between gap-2">
            <div>
              <h3 className="font-semibold">📍 {d.name}</h3>
              <p className="text-sm text-slate-500">{d.city}, {d.country} · {fmtDate(d.arrivalDate)}{d.departureDate !== d.arrivalDate && ` – ${fmtDate(d.departureDate)}`}</p>
              {d.description && <p className="text-sm text-slate-600 mt-1">{d.description}</p>}
            </div>
            <div className="flex gap-2 items-start">
              <Button variant="ghost" onClick={() => setActFor(d.destinationId)}>+ Activity</Button>
              {isAdmin && <Button variant="danger" onClick={() => confirm('Delete destination and its activities?') && run(() => api.deleteDestination(d.destinationId))}>Delete</Button>}
            </div>
          </div>
          <ul className="mt-3 space-y-2">
            {d.activities.length === 0 && <li className="text-sm text-slate-400">No activities planned.</li>}
            {d.activities.map((a) => {
              const voted = a.voterIds.includes(user.userId)
              const joined = a.participantIds.includes(user.userId)
              return (
                <li key={a.activityId} className="border border-slate-100 rounded-lg p-3 bg-slate-50">
                  <div className="flex flex-wrap justify-between gap-2">
                    <div>
                      <p className="font-medium text-sm">{a.name} <Badge tone={statusTone(a.status)}>{a.status}</Badge></p>
                      <p className="text-xs text-slate-500">{fmtDate(a.activityDate)}{a.activityTime && ` · ${a.activityTime}`} · est. {inr(a.estimatedCost)}</p>
                      {a.description && <p className="text-xs text-slate-600 mt-1">{a.description}</p>}
                    </div>
                    <div className="flex items-center gap-2">
                      <button onClick={() => run(() => api.toggleVote(a.activityId, user.userId))}
                        className={`text-xs px-2.5 py-1 rounded-full border ${voted ? 'bg-teal-600 text-white border-teal-600' : 'bg-white border-slate-300 text-slate-600'}`}>
                        👍 {a.voterIds.length}
                      </button>
                      <button onClick={() => run(() => api.toggleParticipant(a.activityId, user.userId))}
                        className={`text-xs px-2.5 py-1 rounded-full border ${joined ? 'bg-sky-600 text-white border-sky-600' : 'bg-white border-slate-300 text-slate-600'}`}>
                        {joined ? 'Joined' : 'Join'}
                      </button>
                    </div>
                  </div>
                  <div className="flex flex-wrap items-center justify-between gap-2 mt-2">
                    <div className="flex -space-x-1">{a.participantIds.map((id) => <Avatar key={id} name={name(id)} />)}</div>
                    <div className="flex items-center gap-2">
                      <select value={a.status} onChange={(e) => run(() => api.setActivityStatus(a.activityId, e.target.value))} className="text-xs border border-slate-300 rounded px-1 py-0.5 bg-white">
                        <option>PLANNED</option><option>COMPLETED</option><option>CANCELLED</option>
                      </select>
                      <button className="text-xs text-red-600 hover:underline" onClick={() => confirm('Delete activity?') && run(() => api.deleteActivity(a.activityId))}>Delete</button>
                    </div>
                  </div>
                </li>
              )
            })}
          </ul>
        </Card>
      ))}
      {destForm && (
        <FormModal title="Add destination" onClose={() => setDestForm(false)}
          initial={{ name: '', city: '', country: '', description: '', arrivalDate: trip.startDate, departureDate: trip.startDate }}
          onSubmit={(f) => {
            if (f.departureDate < f.arrivalDate) throw new Error('Departure must be on or after arrival')
            return run(() => api.addDestination(trip.tripId, f))
          }}>
          {(f, set) => <>
            <Field label="Name"><Input required value={f.name} onChange={set('name')} /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="City"><Input required value={f.city} onChange={set('city')} /></Field>
              <Field label="Country"><Input required value={f.country} onChange={set('country')} /></Field>
            </div>
            <Field label="Description"><Textarea value={f.description} onChange={set('description')} /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Arrival"><Input type="date" required value={f.arrivalDate} onChange={set('arrivalDate')} /></Field>
              <Field label="Departure"><Input type="date" required value={f.departureDate} onChange={set('departureDate')} /></Field>
            </div>
          </>}
        </FormModal>
      )}
      {actFor && (
        <FormModal title="Add activity" onClose={() => setActFor(null)}
          initial={{ name: '', description: '', activityDate: trip.destinations.find((d) => d.destinationId === actFor)?.arrivalDate || trip.startDate, activityTime: '', estimatedCost: '0' }}
          onSubmit={(f) => {
            if (Number(f.estimatedCost) < 0) throw new Error('Cost cannot be negative')
            return run(() => api.addActivity(actFor, f))
          }}>
          {(f, set) => <>
            <Field label="Name"><Input required value={f.name} onChange={set('name')} /></Field>
            <Field label="Description"><Textarea value={f.description} onChange={set('description')} /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Date"><Input type="date" required value={f.activityDate} onChange={set('activityDate')} /></Field>
              <Field label="Time"><Input type="time" value={f.activityTime} onChange={set('activityTime')} /></Field>
            </div>
            <Field label="Estimated cost (₹)"><Input type="number" min="0" step="0.01" value={f.estimatedCost} onChange={set('estimatedCost')} /></Field>
          </>}
        </FormModal>
      )}
    </div>
  )
}
