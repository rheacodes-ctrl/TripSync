import { useState } from 'react'
import { api } from '../api/client.js'
import { Avatar, Badge, Button, Card, Field, FormModal, Input, inr } from '../components/ui.jsx'
import { totals } from '../components/balances.js'

export default function Overview({ trip, isAdmin, run }) {
  const [adding, setAdding] = useState(false)
  const t = totals(trip)
  const pct = Math.round((t.spent / trip.budget) * 100)
  const over = t.spent > trip.budget
  const stat = (label, v) => <Card className="p-4"><p className="text-xs text-slate-500">{label}</p><p className="text-xl font-semibold mt-1">{inr(v)}</p></Card>
  return (
    <div className="space-y-5">
      {trip.description && <p className="text-slate-600">{trip.description}</p>}
      {over && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg p-3">
          ⚠ Budget exceeded by {inr(t.spent - trip.budget)}.
        </div>
      )}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        {stat('Budget', trip.budget)}{stat('Reservations', t.reserved)}{stat('Shared expenses', t.expenses)}{stat('Planned activities', t.planned)}
      </div>
      <Card className="p-4">
        <div className="flex justify-between text-sm mb-2"><span>Budget used</span><span className="font-medium">{pct}%</span></div>
        <div className="h-3 bg-slate-100 rounded-full overflow-hidden">
          <div className={`h-full ${over ? 'bg-red-500' : pct > 80 ? 'bg-amber-500' : 'bg-teal-500'}`} style={{ width: Math.min(100, pct) + '%' }} />
        </div>
        <p className="text-xs text-slate-500 mt-2">{inr(Math.max(0, trip.budget - t.spent))} remaining (reservations + expenses)</p>
      </Card>
      <Card className="p-4">
        <div className="flex justify-between items-center mb-3">
          <h3 className="font-semibold">Members ({trip.members.length})</h3>
          {isAdmin && <Button variant="ghost" onClick={() => setAdding(true)}>+ Add member</Button>}
        </div>
        <ul className="divide-y divide-slate-100">
          {trip.members.map((m) => (
            <li key={m.userId} className="flex items-center gap-3 py-2">
              <Avatar name={m.name} />
              <div className="flex-1 min-w-0"><p className="text-sm font-medium">{m.name}</p><p className="text-xs text-slate-500 truncate">{m.email}</p></div>
              {m.userId === trip.adminId ? <Badge tone="blue">Admin</Badge> : isAdmin && (
                <button className="text-xs text-red-600 hover:underline" onClick={() => confirm(`Remove ${m.name}?`) && run(() => api.removeMember(trip.tripId, m.userId))}>Remove</button>
              )}
            </li>
          ))}
        </ul>
      </Card>
      {adding && (
        <FormModal title="Add member" initial={{ email: '' }} submitLabel="Add" onClose={() => setAdding(false)}
          onSubmit={(f) => run(() => api.addMember(trip.tripId, f.email))}>
          {(f, set) => <Field label="Email of a registered user"><Input type="email" required value={f.email} onChange={set('email')} /></Field>}
        </FormModal>
      )}
    </div>
  )
}
