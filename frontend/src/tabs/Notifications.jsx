import { BellRing, Mail, MessageCircle, Send } from 'lucide-react'
import { useState } from 'react'
import { api } from '../api/client.js'
import { Badge, Button, Card, Empty, Field, FormModal, SectionHeader, Select, Textarea, fmtDateTime, inr, statusLabel } from '../components/ui.jsx'
import { computeBalances } from '../components/balances.js'

const TYPES = { INVITATION: 'Invitation', RESERVATION_UPDATE: 'Reservation update', EXPENSE_REMINDER: 'Expense reminder', TRIP_UPDATE: 'Trip update' }
const CHANNEL = { EMAIL: { icon: Mail, label: 'Gmail' }, WHATSAPP: { icon: MessageCircle, label: 'WhatsApp' } }

export default function Notifications({ trip, isAdmin, run }) {
  const [compose, setCompose] = useState(false)
  const name = (id) => trip.members.find((m) => m.userId === id)?.name || id
  const { transfers } = computeBalances(trip)
  const debtors = [...new Set(transfers.map((t) => t.from))]

  const remindAll = () => run(async () => {
    for (const t of transfers) {
      await api.sendNotification(trip.tripId, { type: 'EXPENSE_REMINDER', userIds: [t.from], message: `Reminder: you owe ${name(t.to)} ${inr(t.amount)} for ${trip.tripName}.` })
    }
  })

  return (
    <div className="space-y-6">
      <SectionHeader title="Notifications" sub="Messages sent to travelers by Gmail and WhatsApp, based on each person’s profile settings">
        {isAdmin && <>
          <Button variant="ghost" disabled={!debtors.length} onClick={remindAll}><BellRing size={16} />Remind {debtors.length ? `${debtors.length} ${debtors.length === 1 ? 'person' : 'people'}` : 'everyone'} about expenses</Button>
          <Button onClick={() => setCompose(true)}><Send size={16} />New message</Button>
        </>}
      </SectionHeader>

      <Card>
        {trip.notifications.length === 0 ? <Empty icon={BellRing} title="Nothing sent yet">Invitations, reservation updates and reminders will show up here.</Empty> : (
          <ul className="divide-y divide-line">
            {trip.notifications.map((n) => {
              const { icon: Icon, label } = CHANNEL[n.channel]
              return (
                <li key={n.notificationId} className="p-4 sm:p-5 flex gap-4">
                  <span className={`grid place-items-center w-10 h-10 rounded-xl shrink-0 ${n.channel === 'EMAIL' ? 'bg-coral-50 text-coral-600' : 'bg-emerald-50 text-emerald-700'}`}><Icon size={18} /></span>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm"><span className="font-medium">{TYPES[n.type] || n.type}</span> to {name(n.toUserId)} via {label}</p>
                    <p className="text-sm text-ink-soft mt-0.5">{n.message}</p>
                  </div>
                  <div className="text-right shrink-0"><Badge tone="green">{statusLabel(n.status)}</Badge><p className="text-xs text-ink-soft mt-1.5">{fmtDateTime(n.sentAt)}</p></div>
                </li>
              )
            })}
          </ul>
        )}
      </Card>

      {compose && (
        <FormModal title="Send a message" submitLabel="Send" onClose={() => setCompose(false)}
          initial={{ type: 'TRIP_UPDATE', message: '', userIds: trip.members.map((m) => m.userId) }}
          onSubmit={(f) => {
            if (!f.message.trim()) throw new Error('Write a message first.')
            if (!f.userIds.length) throw new Error('Choose at least one recipient.')
            return run(() => api.sendNotification(trip.tripId, { type: f.type, userIds: f.userIds, message: f.message }))
          }}>
          {(f, set, setF) => <>
            <Field label="Type"><Select value={f.type} onChange={set('type')}>{Object.entries(TYPES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</Select></Field>
            <Field label="Message"><Textarea required value={f.message} onChange={set('message')} placeholder="Meet at the hotel lobby at 9 am." /></Field>
            <div>
              <p className="text-sm font-medium mb-2">Recipients</p>
              <div className="flex flex-wrap gap-3">
                {trip.members.map((m) => (
                  <label key={m.userId} className="flex items-center gap-2 text-sm">
                    <input type="checkbox" className="accent-[#0f7d82]" checked={f.userIds.includes(m.userId)}
                      onChange={(e) => setF((s) => ({ ...s, userIds: e.target.checked ? [...s.userIds, m.userId] : s.userIds.filter((x) => x !== m.userId) }))} />{m.name}
                  </label>
                ))}
              </div>
            </div>
          </>}
        </FormModal>
      )}
    </div>
  )
}
