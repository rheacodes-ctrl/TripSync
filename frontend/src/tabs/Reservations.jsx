import { useState } from 'react'
import { api } from '../api/client.js'
import { Badge, Button, Card, Empty, Field, FormModal, Input, Select, fmtDate, inr, statusTone } from '../components/ui.jsx'

const ICON = { HOTEL: '🏨', TRANSPORT: '✈️', ACTIVITY: '🎟️' }

export default function Reservations({ trip, run }) {
  const [open, setOpen] = useState(false)
  const total = trip.reservations.filter((r) => r.status !== 'CANCELLED').reduce((s, r) => s + r.cost, 0)
  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h2 className="font-semibold text-lg">Reservations <span className="text-sm font-normal text-slate-500">· {inr(total)} active</span></h2>
        <Button onClick={() => setOpen(true)}>+ Reservation</Button>
      </div>
      <Card>
        {trip.reservations.length === 0 ? <Empty>No reservations yet.</Empty> : (
          <ul className="divide-y divide-slate-100">
            {trip.reservations.map((r) => (
              <li key={r.reservationId} className="p-4 flex flex-wrap items-center gap-3">
                <span className="text-2xl">{ICON[r.type]}</span>
                <div className="flex-1 min-w-40">
                  <p className="font-medium text-sm">{r.providerName}</p>
                  <p className="text-xs text-slate-500">{r.type} · {fmtDate(r.reservationDate)}</p>
                </div>
                <span className="font-semibold text-sm">{inr(r.cost)}</span>
                <Badge tone={statusTone(r.status)}>{r.status}</Badge>
                <select value={r.status} onChange={(e) => run(() => api.setReservationStatus(r.reservationId, e.target.value))} className="text-xs border border-slate-300 rounded px-1 py-1 bg-white">
                  <option>PENDING</option><option>CONFIRMED</option><option>CANCELLED</option>
                </select>
                <button className="text-xs text-red-600 hover:underline" onClick={() => confirm('Delete reservation?') && run(() => api.deleteReservation(r.reservationId))}>Delete</button>
              </li>
            ))}
          </ul>
        )}
      </Card>
      {open && (
        <FormModal title="Add reservation" onClose={() => setOpen(false)}
          initial={{ type: 'HOTEL', providerName: '', cost: '', reservationDate: trip.startDate }}
          onSubmit={(f) => {
            if (Number(f.cost) < 0) throw new Error('Cost cannot be negative')
            return run(() => api.addReservation(trip.tripId, f))
          }}>
          {(f, set) => <>
            <Field label="Type"><Select value={f.type} onChange={set('type')}><option>HOTEL</option><option>TRANSPORT</option><option>ACTIVITY</option></Select></Field>
            <Field label="Provider"><Input required value={f.providerName} onChange={set('providerName')} /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Cost (₹)"><Input type="number" min="0" step="0.01" required value={f.cost} onChange={set('cost')} /></Field>
              <Field label="Date"><Input type="date" required value={f.reservationDate} onChange={set('reservationDate')} /></Field>
            </div>
          </>}
        </FormModal>
      )}
    </div>
  )
}
