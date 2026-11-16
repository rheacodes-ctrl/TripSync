import { useState } from 'react'
import { Ban, BedDouble, Check, Plane, Plus, Ticket, Trash2 } from 'lucide-react'
import { api } from '../api/client.js'
import { Badge, Button, Card, Empty, Field, FormModal, Input, SectionHeader, Segmented, Select, fmtDate, inr, statusLabel, statusTone } from '../components/ui.jsx'

const TYPES = {
  HOTEL: { icon: BedDouble, label: 'Hotel', tone: 'bg-sky-50 text-sky-700' },
  TRANSPORT: { icon: Plane, label: 'Transport', tone: 'bg-lagoon-50 text-lagoon-700' },
  ACTIVITY: { icon: Ticket, label: 'Activity', tone: 'bg-violet-50 text-violet-700' },
}
const MODES = ['FLIGHT', 'TRAIN', 'BUS', 'CAR', 'FERRY']

function detailLine(r) {
  const d = r.details || {}
  if (r.type === 'HOTEL') return d.checkInDate ? `${fmtDate(d.checkInDate)} to ${fmtDate(d.checkOutDate)} · ${d.numberOfRooms} ${d.numberOfRooms == 1 ? 'room' : 'rooms'}` : ''
  if (r.type === 'TRANSPORT') return [d.transportType && statusLabel(d.transportType), d.departureLocation && d.arrivalLocation && `${d.departureLocation} to ${d.arrivalLocation}`, d.departureTime, d.bookingReference && `Ref ${d.bookingReference}`].filter(Boolean).join(' · ')
  return d.activityName || ''
}

export default function Reservations({ trip, isAdmin, run }) {
  const [open, setOpen] = useState(false)
  const active = trip.reservations.filter((r) => r.status !== 'CANCELLED')
  const total = active.reduce((s, r) => s + r.cost, 0)

  return (
    <div className="space-y-6">
      <SectionHeader title="Reservations" sub={`${inr(total)} in active bookings, counted against the trip budget`}>
        {isAdmin && <Button onClick={() => setOpen(true)}><Plus size={16} />Add reservation</Button>}
      </SectionHeader>

      <div className="grid sm:grid-cols-3 gap-4">
        {Object.entries(TYPES).map(([k, { icon: Icon, label, tone }]) => {
          const rs = active.filter((r) => r.type === k)
          return (
            <Card key={k} className="p-4 flex items-center gap-3">
              <span className={`grid place-items-center w-10 h-10 rounded-xl ${tone}`}><Icon size={19} /></span>
              <div><p className="text-sm text-ink-soft">{label}</p><p className="font-display font-semibold text-xl">{inr(rs.reduce((s, r) => s + r.cost, 0))}</p><p className="text-xs text-ink-soft">{rs.length} active</p></div>
            </Card>
          )
        })}
      </div>

      {trip.reservations.length === 0 ? <Card><Empty icon={Ticket} title="No reservations yet">{isAdmin ? 'Track hotels, transport and activity tickets here.' : 'The trip admin hasn’t added any bookings yet.'}</Empty></Card> : (
        <div className="space-y-3">
          {trip.reservations.map((r) => {
            const { icon: Icon, label, tone } = TYPES[r.type]
            const cancelled = r.status === 'CANCELLED'
            return (
              <Card key={r.reservationId} className={`p-4 sm:p-5 flex flex-wrap items-center gap-4 ${cancelled ? 'opacity-70' : ''}`}>
                <span className={`grid place-items-center w-11 h-11 rounded-xl shrink-0 ${tone}`}><Icon size={20} /></span>
                <div className="flex-1 min-w-48">
                  <p className="font-medium flex items-center gap-2 flex-wrap"><span className={cancelled ? 'line-through' : ''}>{r.providerName}</span><Badge tone={statusTone(r.status)}>{statusLabel(r.status)}</Badge></p>
                  <p className="text-sm text-ink-soft mt-0.5">{label} · booked {fmtDate(r.reservationDate)}</p>
                  {detailLine(r) && <p className="text-sm text-ink-soft">{detailLine(r)}</p>}
                </div>
                <p className={`font-display font-semibold text-lg ${cancelled ? 'line-through text-ink-soft' : ''}`}>{inr(r.cost)}</p>
                {isAdmin && (
                  <div className="flex gap-2">
                    {r.status !== 'CONFIRMED' && <Button size="sm" onClick={() => run(() => api.setReservationStatus(r.reservationId, 'CONFIRMED'))}><Check size={14} />{cancelled ? 'Reinstate and confirm' : 'Confirm'}</Button>}
                    {!cancelled && <Button size="sm" variant="ghost" onClick={() => confirm(`Cancel ${r.providerName}?`) && run(() => api.setReservationStatus(r.reservationId, 'CANCELLED'))}><Ban size={14} />Cancel</Button>}
                    <Button size="sm" variant="quiet" aria-label={`Delete ${r.providerName}`} onClick={() => confirm('Delete this reservation?') && run(() => api.deleteReservation(r.reservationId))}><Trash2 size={14} /></Button>
                  </div>
                )}
              </Card>
            )
          })}
        </div>
      )}

      {open && (
        <FormModal title="Add reservation" submitLabel="Add reservation" size="lg" onClose={() => setOpen(false)}
          initial={{ type: 'HOTEL', providerName: '', cost: '', reservationDate: trip.startDate, checkInDate: trip.startDate, checkOutDate: trip.endDate, numberOfRooms: '1', transportType: 'FLIGHT', departureLocation: '', arrivalLocation: '', departureTime: '', bookingReference: '', activityName: '' }}
          onSubmit={(f) => {
            if (Number(f.cost) < 0) throw new Error('Cost cannot be negative.')
            let details
            if (f.type === 'HOTEL') {
              if (f.checkOutDate <= f.checkInDate) throw new Error('Check-out must be after check-in.')
              if (!(Number(f.numberOfRooms) >= 1)) throw new Error('Book at least one room.')
              details = { checkInDate: f.checkInDate, checkOutDate: f.checkOutDate, numberOfRooms: Number(f.numberOfRooms) }
            } else if (f.type === 'TRANSPORT') {
              if (!f.departureLocation.trim() || !f.arrivalLocation.trim()) throw new Error('Enter both departure and arrival locations.')
              details = { transportType: f.transportType, departureLocation: f.departureLocation, arrivalLocation: f.arrivalLocation, departureTime: f.departureTime, bookingReference: f.bookingReference }
            } else details = { activityName: f.activityName }
            return run(() => api.addReservation(trip.tripId, { type: f.type, providerName: f.providerName, cost: f.cost, reservationDate: f.reservationDate, details }))
          }}>
          {(f, set, setF) => <>
            <Segmented value={f.type} onChange={(type) => setF((s) => ({ ...s, type }))} options={Object.entries(TYPES).map(([k, v]) => [k, v.label])} />
            <Field label={f.type === 'HOTEL' ? 'Hotel name' : f.type === 'TRANSPORT' ? 'Operator' : 'Provider'}><Input required value={f.providerName} onChange={set('providerName')} /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Cost (₹)"><Input type="number" min="0" step="0.01" required value={f.cost} onChange={set('cost')} /></Field>
              <Field label="Booking date"><Input type="date" required value={f.reservationDate} onChange={set('reservationDate')} /></Field>
            </div>
            {f.type === 'HOTEL' && (
              <div className="grid grid-cols-3 gap-3">
                <Field label="Check-in"><Input type="date" required min={trip.startDate} max={trip.endDate} value={f.checkInDate} onChange={set('checkInDate')} /></Field>
                <Field label="Check-out"><Input type="date" required min={f.checkInDate} max={trip.endDate} value={f.checkOutDate} onChange={set('checkOutDate')} /></Field>
                <Field label="Rooms"><Input type="number" min="1" required value={f.numberOfRooms} onChange={set('numberOfRooms')} /></Field>
              </div>
            )}
            {f.type === 'TRANSPORT' && <>
              <div className="grid grid-cols-2 gap-3">
                <Field label="Mode"><Select value={f.transportType} onChange={set('transportType')}>{MODES.map((m) => <option key={m} value={m}>{statusLabel(m)}</option>)}</Select></Field>
                <Field label="Departure time"><Input type="time" value={f.departureTime} onChange={set('departureTime')} /></Field>
                <Field label="From"><Input value={f.departureLocation} onChange={set('departureLocation')} /></Field>
                <Field label="To"><Input value={f.arrivalLocation} onChange={set('arrivalLocation')} /></Field>
              </div>
              <Field label="Booking reference"><Input value={f.bookingReference} onChange={set('bookingReference')} /></Field>
            </>}
            {f.type === 'ACTIVITY' && <Field label="Activity"><Input value={f.activityName} onChange={set('activityName')} placeholder="Water sports package" /></Field>}
          </>}
        </FormModal>
      )}
    </div>
  )
}
