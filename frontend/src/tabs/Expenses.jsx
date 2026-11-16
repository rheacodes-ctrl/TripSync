import { useState } from 'react'
import { ArrowRight, BellRing, Check, PartyPopper, Plus, Receipt, Trash2 } from 'lucide-react'
import { api } from '../api/client.js'
import { Avatar, Badge, Button, Card, Empty, Field, FormModal, Input, Segmented, Select, SectionHeader, fmtDate, inr, statusLabel, statusTone } from '../components/ui.jsx'
import { computeBalances } from '../components/balances.js'

const round2 = (n) => Math.round(n * 100) / 100

export default function Expenses({ trip, user, isAdmin, run }) {
  const [open, setOpen] = useState(false)
  const name = (id) => trip.members.find((m) => m.userId === id)?.name || id
  const { net, transfers, outstanding } = computeBalances(trip)
  const mine = round2(net[user.userId] || 0)
  const maxAbs = Math.max(1, ...Object.values(net).map(Math.abs))

  return (
    <div className="grid lg:grid-cols-[1.5fr_1fr] gap-6 items-start">
      <div className="space-y-4">
        <SectionHeader title="Shared expenses" sub={`${trip.expenses.length} logged · ${inr(trip.expenses.reduce((s, e) => s + e.amount, 0))} total`}>
          <Button onClick={() => setOpen(true)}><Plus size={16} />Add expense</Button>
        </SectionHeader>
        <Card>
          {trip.expenses.length === 0 ? <Empty icon={Receipt} title="No expenses yet" action={<Button onClick={() => setOpen(true)}><Plus size={16} />Add the first expense</Button>}>Record who paid and how it’s shared. Balances update automatically.</Empty> : (
            <ul className="divide-y divide-line">
              {trip.expenses.map((e) => (
                <li key={e.expenseId} className="p-5">
                  <div className="flex justify-between gap-4">
                    <div className="flex gap-3 min-w-0">
                      <Avatar name={name(e.paidBy)} />
                      <div className="min-w-0">
                        <p className="font-medium flex items-center gap-2 flex-wrap">{e.description}<Badge tone={e.splitType === 'CUSTOM' ? 'sun' : 'gray'}>{e.splitType === 'CUSTOM' ? 'Custom split' : 'Equal split'}</Badge></p>
                        <p className="text-sm text-ink-soft">Paid by {e.paidBy === user.userId ? 'you' : name(e.paidBy)} · {fmtDate(e.expenseDate)}</p>
                      </div>
                    </div>
                    <div className="text-right shrink-0">
                      <p className="font-display font-semibold text-lg">{inr(e.amount)}</p>
                      {(isAdmin || e.paidBy === user.userId) && <button className="text-xs text-ink-soft hover:text-coral-600 inline-flex items-center gap-1" onClick={() => confirm('Delete this expense?') && run(() => api.deleteExpense(e.expenseId))}><Trash2 size={12} />Delete</button>}
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-2 mt-3">
                    {e.splits.map((s) => (
                      <span key={s.userId} className="inline-flex items-center gap-1.5 text-xs rounded-full bg-mist border border-line px-2.5 py-1">
                        {name(s.userId)} <b className="font-medium">{inr(s.shareAmount)}</b>
                        {s.userId !== e.paidBy && (s.isSettled ? <Check size={12} className="text-emerald-600" aria-label="settled" /> : null)}
                      </span>
                    ))}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <div className="space-y-4">
        <Card className={`p-6 ${mine > 0 ? 'bg-emerald-50 border-emerald-200' : mine < 0 ? 'bg-coral-50 border-coral-500/25' : ''}`}>
          <p className="text-sm text-ink-soft">Your balance</p>
          <p className="font-display text-3xl font-semibold mt-1">{mine > 0 ? `You’re owed ${inr(mine)}` : mine < 0 ? `You owe ${inr(-mine)}` : 'You’re settled up'}</p>
          <p className="text-sm text-ink-soft mt-2">Outstanding across the group: <b className="text-ink">{inr(outstanding)}</b></p>
        </Card>

        <Card className="p-5">
          <h3 className="font-display font-semibold text-lg mb-3">Balances</h3>
          <ul className="space-y-3">
            {Object.entries(net).map(([id, v]) => (
              <li key={id}>
                <div className="flex justify-between text-sm"><span>{name(id)}{id === user.userId && ' (you)'}</span>
                  <span className={v > 0.009 ? 'text-emerald-700 font-medium' : v < -0.009 ? 'text-coral-600 font-medium' : 'text-ink-soft'}>{v > 0.009 ? `gets back ${inr(v)}` : v < -0.009 ? `owes ${inr(-v)}` : 'settled'}</span></div>
                <div className="h-1.5 rounded-full bg-slate-100 mt-1.5 overflow-hidden"><div className={`h-full rounded-full ${v >= 0 ? 'bg-emerald-500' : 'bg-coral-500'}`} style={{ width: (Math.abs(v) / maxAbs) * 100 + '%' }} /></div>
              </li>
            ))}
          </ul>
        </Card>

        <Card className="p-5">
          <h3 className="font-display font-semibold text-lg mb-1">Who owes whom</h3>
          <p className="text-xs text-ink-soft mb-3">The fewest payments that clear everyone.</p>
          {transfers.length === 0 ? <p className="flex items-center gap-2 text-sm text-ink-soft"><PartyPopper size={17} className="text-sun-600" />Everyone is settled up.</p> : (
            <ul className="space-y-3">
              {transfers.map((t, i) => {
                const involved = t.from === user.userId || t.to === user.userId
                return (
                  <li key={i} className="rounded-xl border border-line p-3">
                    <div className="flex items-center justify-between gap-2 text-sm">
                      <span className="flex items-center gap-1.5 min-w-0 truncate">{name(t.from)}<ArrowRight size={14} className="text-ink-soft shrink-0" />{name(t.to)}</span>
                      <b>{inr(t.amount)}</b>
                    </div>
                    <div className="flex gap-2 mt-2.5">
                      {involved && <Button size="sm" onClick={() => run(() => api.addPayment(trip.tripId, { fromUserId: t.from, toUserId: t.to, amount: t.amount }))}><Check size={14} />Mark as paid</Button>}
                      {(isAdmin || t.to === user.userId) && <Button size="sm" variant="ghost" onClick={() => run(() => api.sendNotification(trip.tripId, { type: 'EXPENSE_REMINDER', userIds: [t.from], message: `Reminder: you owe ${name(t.to)} ${inr(t.amount)} for ${trip.tripName}.` }))}><BellRing size={14} />Send reminder</Button>}
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </Card>

        <Card className="p-5">
          <h3 className="font-display font-semibold text-lg mb-3">Settlement history</h3>
          {trip.payments.length === 0 ? <p className="text-sm text-ink-soft">No payments recorded for this trip.</p> : (
            <ul className="space-y-2.5 text-sm">
              {trip.payments.map((p) => (
                <li key={p.paymentId} className="flex justify-between items-center gap-2">
                  <span className="flex items-center gap-1.5 min-w-0 truncate">{name(p.fromUserId)}<ArrowRight size={13} className="text-ink-soft" />{name(p.toUserId)} · {inr(p.amount)}</span>
                  <Badge tone={statusTone(p.status)}>{statusLabel(p.status)}</Badge>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      {open && <ExpenseForm trip={trip} user={user} run={run} onClose={() => setOpen(false)} />}
    </div>
  )
}

function ExpenseForm({ trip, user, run, onClose }) {
  return (
    <FormModal title="Add expense" submitLabel="Add expense" size="lg" onClose={onClose}
      initial={{ description: '', amount: '', paidBy: user.userId, expenseDate: new Date().toISOString().slice(0, 10), splitType: 'EQUAL', participantIds: trip.members.map((m) => m.userId), shares: {} }}
      onSubmit={(f) => {
        const amount = Number(f.amount)
        if (!(amount > 0)) throw new Error('Amount must be greater than 0.')
        const payload = { description: f.description, amount, paidBy: f.paidBy, expenseDate: f.expenseDate, splitType: f.splitType }
        if (f.splitType === 'EQUAL') {
          if (f.participantIds.length === 0) throw new Error('Select at least one person to split with.')
          payload.participantIds = f.participantIds
        } else {
          const splits = trip.members.map((m) => ({ userId: m.userId, shareAmount: Number(f.shares[m.userId] || 0) })).filter((s) => s.shareAmount > 0)
          const total = round2(splits.reduce((s, x) => s + x.shareAmount, 0))
          if (splits.length === 0) throw new Error('Enter at least one share.')
          if (Math.abs(total - amount) > 0.009) throw new Error(`Shares add up to ${inr(total)} but the expense is ${inr(amount)}.`)
          payload.splits = splits
        }
        return run(() => api.addExpense(trip.tripId, payload))
      }}>
      {(f, set, setF) => {
        const amount = Number(f.amount) || 0
        const n = f.participantIds.length
        const allocated = round2(Object.values(f.shares).reduce((s, v) => s + (Number(v) || 0), 0))
        const left = round2(amount - allocated)
        return <>
          <Field label="Description"><Input required autoFocus value={f.description} onChange={set('description')} placeholder="Group dinner" /></Field>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Amount (₹)"><Input type="number" min="0.01" step="0.01" required value={f.amount} onChange={set('amount')} /></Field>
            <Field label="Date"><Input type="date" required value={f.expenseDate} onChange={set('expenseDate')} /></Field>
          </div>
          <Field label="Paid by"><Select value={f.paidBy} onChange={set('paidBy')}>{trip.members.map((m) => <option key={m.userId} value={m.userId}>{m.name}</option>)}</Select></Field>

          <div>
            <p className="text-sm font-medium mb-1.5">How to split</p>
            <Segmented value={f.splitType} onChange={(splitType) => setF((s) => ({ ...s, splitType }))} options={[['EQUAL', 'Split equally'], ['CUSTOM', 'Custom amounts']]} />
          </div>

          {f.splitType === 'EQUAL' ? (
            <div className="space-y-2">
              {trip.members.map((m) => {
                const on = f.participantIds.includes(m.userId)
                return (
                  <label key={m.userId} className="flex items-center gap-3 rounded-lg border border-line px-3 py-2 text-sm cursor-pointer hover:bg-mist">
                    <input type="checkbox" className="accent-[#0f7d82] w-4 h-4" checked={on}
                      onChange={(e) => setF((s) => ({ ...s, participantIds: e.target.checked ? [...s.participantIds, m.userId] : s.participantIds.filter((x) => x !== m.userId) }))} />
                    <Avatar name={m.name} size="sm" /><span className="flex-1">{m.name}</span>
                    <span className="text-ink-soft">{on && n && amount ? inr(amount / n) : '–'}</span>
                  </label>
                )
              })}
              <p className="text-xs text-ink-soft">Any leftover paise goes to the first person selected.</p>
            </div>
          ) : (
            <div className="space-y-2">
              {trip.members.map((m) => (
                <div key={m.userId} className="flex items-center gap-3 text-sm">
                  <Avatar name={m.name} size="sm" /><span className="flex-1">{m.name}</span>
                  <input type="number" min="0" step="0.01" aria-label={`${m.name}'s share`} value={f.shares[m.userId] ?? ''} placeholder="0"
                    onChange={(e) => setF((s) => ({ ...s, shares: { ...s.shares, [m.userId]: e.target.value } }))}
                    className="w-28 rounded-lg border border-line px-3 py-2 text-sm text-right focus:outline-none focus:border-lagoon-500 focus:ring-2 focus:ring-lagoon-500/25" />
                </div>
              ))}
              <p className={`text-sm font-medium ${Math.abs(left) < 0.01 && amount ? 'text-emerald-700' : 'text-ink-soft'}`}>
                {Math.abs(left) < 0.01 && amount ? 'Shares match the total.' : left > 0 ? `${inr(left)} still to assign` : `${inr(-left)} over the total`}
              </p>
            </div>
          )}
        </>
      }}
    </FormModal>
  )
}
