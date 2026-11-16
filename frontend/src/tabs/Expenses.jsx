import { useState } from 'react'
import { api } from '../api/client.js'
import { Badge, Button, Card, Empty, Field, FormModal, Input, Select, fmtDate, inr, statusTone } from '../components/ui.jsx'
import { computeBalances } from '../components/balances.js'

export default function Expenses({ trip, user, run }) {
  const [open, setOpen] = useState(false)
  const name = (id) => trip.members.find((m) => m.userId === id)?.name || id
  const { net, transfers } = computeBalances(trip)

  return (
    <div className="grid lg:grid-cols-5 gap-5">
      <div className="lg:col-span-3 space-y-4">
        <div className="flex justify-between items-center">
          <h2 className="font-semibold text-lg">Shared expenses</h2>
          <Button onClick={() => setOpen(true)}>+ Expense</Button>
        </div>
        <Card>
          {trip.expenses.length === 0 ? <Empty>No expenses yet.</Empty> : (
            <ul className="divide-y divide-slate-100">
              {trip.expenses.map((e) => (
                <li key={e.expenseId} className="p-4">
                  <div className="flex justify-between gap-3">
                    <div>
                      <p className="font-medium text-sm">{e.description}</p>
                      <p className="text-xs text-slate-500">Paid by {name(e.paidBy)} · {fmtDate(e.expenseDate)}</p>
                    </div>
                    <div className="text-right">
                      <p className="font-semibold text-sm">{inr(e.amount)}</p>
                      <button className="text-xs text-red-600 hover:underline" onClick={() => confirm('Delete expense?') && run(() => api.deleteExpense(e.expenseId))}>Delete</button>
                    </div>
                  </div>
                  <p className="text-xs text-slate-500 mt-2">
                    Split: {e.splits.map((s) => `${name(s.userId)} ${inr(s.shareAmount)}`).join(' · ')}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
      <div className="lg:col-span-2 space-y-4">
        <Card className="p-4">
          <h3 className="font-semibold mb-3">Balances</h3>
          <ul className="space-y-1.5 text-sm">
            {Object.entries(net).map(([id, v]) => (
              <li key={id} className="flex justify-between">
                <span>{name(id)}{id === user.userId && ' (you)'}</span>
                <span className={v > 0.009 ? 'text-emerald-600' : v < -0.009 ? 'text-red-600' : 'text-slate-400'}>
                  {v > 0.009 ? `gets ${inr(v)}` : v < -0.009 ? `owes ${inr(-v)}` : 'settled'}
                </span>
              </li>
            ))}
          </ul>
        </Card>
        <Card className="p-4">
          <h3 className="font-semibold mb-3">Suggested settlements</h3>
          {transfers.length === 0 ? <p className="text-sm text-slate-500">Everyone is settled up 🎉</p> : (
            <ul className="space-y-2">
              {transfers.map((t, i) => (
                <li key={i} className="flex items-center justify-between gap-2 text-sm">
                  <span>{name(t.from)} → {name(t.to)}: <b>{inr(t.amount)}</b></span>
                  {(t.from === user.userId || t.to === user.userId) && (
                    <Button variant="ghost" className="!py-1 !px-2 text-xs"
                      onClick={() => run(() => api.addPayment(trip.tripId, { fromUserId: t.from, toUserId: t.to, amount: t.amount }))}>
                      Mark paid
                    </Button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </Card>
        <Card className="p-4">
          <h3 className="font-semibold mb-3">Payment history</h3>
          {trip.payments.length === 0 ? <p className="text-sm text-slate-500">No payments recorded.</p> : (
            <ul className="space-y-1.5 text-sm">
              {trip.payments.map((p) => (
                <li key={p.paymentId} className="flex justify-between items-center">
                  <span>{name(p.fromUserId)} → {name(p.toUserId)} · {inr(p.amount)}</span>
                  <Badge tone={statusTone(p.status)}>{p.status}</Badge>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
      {open && (
        <FormModal title="Add expense" onClose={() => setOpen(false)}
          initial={{ description: '', amount: '', paidBy: user.userId, expenseDate: new Date().toISOString().slice(0, 10), participantIds: trip.members.map((m) => m.userId) }}
          onSubmit={(f) => {
            if (!(Number(f.amount) > 0)) throw new Error('Amount must be greater than 0')
            if (f.participantIds.length === 0) throw new Error('Select at least one person to split with')
            return run(() => api.addExpense(trip.tripId, f))
          }}>
          {(f, set, setF) => <>
            <Field label="Description"><Input required value={f.description} onChange={set('description')} /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Amount (₹)"><Input type="number" min="0.01" step="0.01" required value={f.amount} onChange={set('amount')} /></Field>
              <Field label="Date"><Input type="date" required value={f.expenseDate} onChange={set('expenseDate')} /></Field>
            </div>
            <Field label="Paid by"><Select value={f.paidBy} onChange={set('paidBy')}>{trip.members.map((m) => <option key={m.userId} value={m.userId}>{m.name}</option>)}</Select></Field>
            <Field label="Split equally between">
              <div className="flex flex-wrap gap-3">
                {trip.members.map((m) => (
                  <label key={m.userId} className="flex items-center gap-1.5 text-sm">
                    <input type="checkbox" checked={f.participantIds.includes(m.userId)}
                      onChange={(e) => setF((s) => ({ ...s, participantIds: e.target.checked ? [...s.participantIds, m.userId] : s.participantIds.filter((x) => x !== m.userId) }))} />
                    {m.name}
                  </label>
                ))}
              </div>
            </Field>
          </>}
        </FormModal>
      )}
    </div>
  )
}
