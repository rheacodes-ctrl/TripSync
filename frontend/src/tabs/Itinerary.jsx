import { useState } from 'react'
import { CalendarDays, Clock, MapPin, Plus, ThumbsUp, Trash2, Trophy } from 'lucide-react'
import { api } from '../api/client.js'
import { AvatarStack, Badge, Button, Card, Empty, Field, FormModal, Input, SectionHeader, Select, Textarea, fmtDate, fmtRange, inr, statusLabel, statusTone } from '../components/ui.jsx'

const STATUSES = ['PLANNED', 'COMPLETED', 'CANCELLED']

export default function Itinerary({ trip, user, isAdmin, run }) {
  const [destForm, setDestForm] = useState(false)
  const [actFor, setActFor] = useState(null) // destination being given a new activity
  const name = (id) => trip.members.find((m) => m.userId === id)?.name || id
  const actDest = trip.destinations.find((d) => d.destinationId === actFor)

  return (
    <div className="space-y-6">
      <SectionHeader title="Itinerary" sub={`${trip.destinations.length} ${trip.destinations.length === 1 ? 'stop' : 'stops'} between ${fmtRange(trip.startDate, trip.endDate)}`}>
        {isAdmin && <Button onClick={() => setDestForm(true)}><Plus size={16} />Add destination</Button>}
      </SectionHeader>

      {trip.destinations.length === 0 && <Card><Empty icon={MapPin} title="No destinations yet" action={isAdmin && <Button onClick={() => setDestForm(true)}><Plus size={16} />Add the first stop</Button>}>{isAdmin ? 'Add the places you’ll visit, then plan activities for each.' : 'The trip admin hasn’t added any stops yet.'}</Empty></Card>}

      <ol className="relative">
        {trip.destinations.map((d, i) => {
          const topVotes = Math.max(0, ...d.activities.map((a) => a.voterIds.length))
          const est = d.activities.filter((a) => a.status !== 'CANCELLED').reduce((s, a) => s + a.estimatedCost, 0)
          return (
            <li key={d.destinationId} className="relative pl-10 pb-8 last:pb-0">
              {i < trip.destinations.length - 1 && <span className="absolute left-[15px] top-8 bottom-0 w-px bg-lagoon-200" aria-hidden />}
              <span className="absolute left-0 top-1 grid place-items-center w-8 h-8 rounded-full bg-lagoon-600 text-white ring-4 ring-mist"><MapPin size={15} /></span>
              <Card className="p-5">
                <div className="flex flex-wrap justify-between gap-3">
                  <div>
                    <h3 className="font-display font-semibold text-xl">{d.name}</h3>
                    <p className="text-sm text-ink-soft mt-0.5">{d.city}, {d.country}</p>
                    <p className="inline-flex items-center gap-1.5 text-sm text-lagoon-700 font-medium mt-1.5"><CalendarDays size={14} />{fmtRange(d.arrivalDate, d.departureDate)}</p>
                    {d.description && <p className="text-sm text-ink-soft mt-2 max-w-xl">{d.description}</p>}
                  </div>
                  <div className="flex gap-2 items-start">
                    <Button variant="ghost" size="sm" onClick={() => setActFor(d.destinationId)}><Plus size={14} />Activity</Button>
                    {isAdmin && <Button variant="danger" size="sm" aria-label={`Delete ${d.name}`} onClick={() => confirm('Delete this destination and its activities?') && run(() => api.deleteDestination(d.destinationId))}><Trash2 size={14} /></Button>}
                  </div>
                </div>

                <div className="mt-5 flex items-center justify-between text-xs text-ink-soft"><span>{d.activities.length} {d.activities.length === 1 ? 'activity' : 'activities'}</span><span>Estimated {inr(est)}</span></div>
                <ul className="mt-2 space-y-2.5">
                  {d.activities.length === 0 && <li className="text-sm text-ink-soft rounded-xl border border-dashed border-line p-4 text-center">No activities yet. Add one so the group can vote.</li>}
                  {d.activities.map((a) => {
                    const voted = a.voterIds.includes(user.userId)
                    const joined = a.participantIds.includes(user.userId)
                    return (
                      <li key={a.activityId} className={`rounded-xl border border-line bg-mist/60 p-4 ${a.status === 'CANCELLED' ? 'opacity-60' : ''}`}>
                        <div className="flex flex-wrap justify-between gap-3">
                          <div className="min-w-0">
                            <p className="font-medium flex items-center gap-2 flex-wrap"><span className={a.status === 'CANCELLED' ? 'line-through' : ''}>{a.name}</span>
                              <Badge tone={statusTone(a.status)}>{statusLabel(a.status)}</Badge>
                              {topVotes > 0 && a.voterIds.length === topVotes && <Badge tone="sun" icon={Trophy}>Most voted</Badge>}
                            </p>
                            <p className="flex flex-wrap items-center gap-x-3 text-xs text-ink-soft mt-1">
                              <span>{fmtDate(a.activityDate)}</span>
                              {a.activityTime && <span className="inline-flex items-center gap-1"><Clock size={12} />{a.activityTime}</span>}
                              <span>Est. {inr(a.estimatedCost)}</span>
                            </p>
                            {a.description && <p className="text-sm text-ink-soft mt-1.5">{a.description}</p>}
                          </div>
                          <div className="flex items-center gap-2 self-start">
                            <button onClick={() => run(() => api.toggleVote(a.activityId, user.userId))} aria-pressed={voted} aria-label={voted ? 'Remove your vote' : 'Vote for this activity'}
                              className={`inline-flex items-center gap-1.5 text-sm px-3 py-1.5 rounded-full border transition-colors ${voted ? 'bg-lagoon-600 text-white border-lagoon-600' : 'bg-white border-line text-ink-soft hover:border-lagoon-500'}`}>
                              <ThumbsUp size={14} />{a.voterIds.length}
                            </button>
                            <button onClick={() => run(() => api.toggleParticipant(a.activityId, user.userId))} aria-pressed={joined}
                              className={`text-sm px-3 py-1.5 rounded-full border transition-colors ${joined ? 'bg-ink text-white border-ink' : 'bg-white border-line text-ink-soft hover:border-lagoon-500'}`}>
                              {joined ? 'Joined' : 'Join'}
                            </button>
                          </div>
                        </div>
                        <div className="flex flex-wrap items-center justify-between gap-3 mt-3 pt-3 border-t border-line">
                          <div className="flex items-center gap-2 text-xs text-ink-soft">
                            {a.participantIds.length ? <><AvatarStack names={a.participantIds.map(name)} /><span>{a.participantIds.map(name).join(', ')}</span></> : <span>Nobody has joined yet</span>}
                          </div>
                          {isAdmin && (
                            <div className="flex items-center gap-2">
                              <Select aria-label="Activity status" value={a.status} onChange={(e) => run(() => api.setActivityStatus(a.activityId, e.target.value))} style={{ width: 'auto' }} >
                                {STATUSES.map((s) => <option key={s} value={s}>{statusLabel(s)}</option>)}
                              </Select>
                              <Button variant="quiet" size="sm" aria-label={`Delete ${a.name}`} onClick={() => confirm('Delete this activity?') && run(() => api.deleteActivity(a.activityId))}><Trash2 size={14} /></Button>
                            </div>
                          )}
                        </div>
                      </li>
                    )
                  })}
                </ul>
              </Card>
            </li>
          )
        })}
      </ol>

      {destForm && (
        <FormModal title="Add destination" submitLabel="Add destination" onClose={() => setDestForm(false)}
          initial={{ name: '', city: '', country: '', description: '', arrivalDate: trip.startDate, departureDate: trip.startDate }}
          onSubmit={(f) => {
            if (f.departureDate < f.arrivalDate) throw new Error('Departure must be on or after arrival.')
            return run(() => api.addDestination(trip.tripId, f))
          }}>
          {(f, set) => <>
            <Field label="Name"><Input required value={f.name} onChange={set('name')} placeholder="Baga Beach" /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="City"><Input required value={f.city} onChange={set('city')} /></Field>
              <Field label="Country"><Input required value={f.country} onChange={set('country')} /></Field>
            </div>
            <Field label="Description"><Textarea value={f.description} onChange={set('description')} /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Arrival"><Input type="date" required min={trip.startDate} max={trip.endDate} value={f.arrivalDate} onChange={set('arrivalDate')} /></Field>
              <Field label="Departure"><Input type="date" required min={f.arrivalDate || trip.startDate} max={trip.endDate} value={f.departureDate} onChange={set('departureDate')} /></Field>
            </div>
            <p className="text-xs text-ink-soft">Dates must fall within the trip: {fmtRange(trip.startDate, trip.endDate)}.</p>
          </>}
        </FormModal>
      )}
      {actDest && (
        <FormModal title={`Add activity in ${actDest.name}`} submitLabel="Add activity" onClose={() => setActFor(null)}
          initial={{ name: '', description: '', activityDate: actDest.arrivalDate, activityTime: '', estimatedCost: '0' }}
          onSubmit={(f) => {
            if (Number(f.estimatedCost) < 0) throw new Error('Cost cannot be negative.')
            return run(() => api.addActivity(actFor, f))
          }}>
          {(f, set) => <>
            <Field label="Name"><Input required value={f.name} onChange={set('name')} placeholder="Sunset kayaking" /></Field>
            <Field label="Description"><Textarea value={f.description} onChange={set('description')} /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Date"><Input type="date" required min={actDest.arrivalDate} max={actDest.departureDate} value={f.activityDate} onChange={set('activityDate')} /></Field>
              <Field label="Time"><Input type="time" value={f.activityTime} onChange={set('activityTime')} /></Field>
            </div>
            <Field label="Estimated cost (₹)"><Input type="number" min="0" step="0.01" value={f.estimatedCost} onChange={set('estimatedCost')} /></Field>
            <p className="text-xs text-ink-soft">Must fall within the stay: {fmtRange(actDest.arrivalDate, actDest.departureDate)}.</p>
          </>}
        </FormModal>
      )}
    </div>
  )
}
