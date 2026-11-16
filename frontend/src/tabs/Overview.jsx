import { useState } from 'react'
import { AlertTriangle, Landmark, Mail, MapPin, Receipt, Ticket, UserPlus, Vote } from 'lucide-react'
import { api } from '../api/client.js'
import { Avatar, Badge, Button, Card, Field, FormModal, Input, SectionHeader, Stat, inr } from '../components/ui.jsx'
import { totals } from '../components/balances.js'
import { roleLabel } from '../components/Layout.jsx'

export default function Overview({ trip, isAdmin, run }) {
  const [adding, setAdding] = useState(false)
  const t = totals(trip)
  const pct = trip.budget ? Math.round((t.spent / trip.budget) * 100) : 0
  const over = t.spent > trip.budget
  const remaining = trip.budget - t.spent
  const acts = trip.destinations.flatMap((d) => d.activities)
  const seg = (v) => Math.min(100, (v / trip.budget) * 100)

  return (
    <div className="space-y-6">
      {trip.description && <p className="text-lg text-ink-soft max-w-3xl">{trip.description}</p>}

      {(over || pct >= 90) && (
        <div role="alert" className={`flex gap-3 rounded-xl p-4 text-sm border ${over ? 'bg-coral-50 border-coral-500/25 text-coral-600' : 'bg-sun-100 border-sun-500/40 text-ink'}`}>
          <AlertTriangle size={18} className="shrink-0 mt-0.5" />
          <p>{over ? `Spending is ${inr(t.spent - trip.budget)} over the trip budget.` : `${pct}% of the budget is used. New bookings and expenses over ${inr(Math.max(0, remaining))} will be blocked.`}</p>
        </div>
      )}

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Stat icon={Landmark} label="Budget" value={inr(trip.budget)} />
        <Stat icon={Ticket} label="Reservations" value={inr(t.reserved)} sub="Excludes cancelled" tone="bg-sky-50 text-sky-700" />
        <Stat icon={Receipt} label="Shared expenses" value={inr(t.expenses)} tone="bg-sun-100 text-sun-600" />
        <Stat icon={Vote} label="Planned activities" value={inr(t.planned)} sub="Estimate, not yet spent" tone="bg-violet-50 text-violet-700" />
      </div>

      <Card className="p-6">
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <h3 className="font-display font-semibold text-lg">Budget</h3>
          <p className="text-sm text-ink-soft"><span className="font-medium text-ink">{inr(t.spent)}</span> of {inr(trip.budget)} used ({pct}%)</p>
        </div>
        <div className="flex h-3.5 rounded-full overflow-hidden bg-slate-100 mt-4" role="img" aria-label={`Reservations ${inr(t.reserved)}, expenses ${inr(t.expenses)}, ${inr(Math.max(0, remaining))} remaining`}>
          <div className="bg-lagoon-600" style={{ width: seg(t.reserved) + '%' }} />
          <div className="bg-sun-500" style={{ width: Math.min(100 - seg(t.reserved), seg(t.expenses)) + '%' }} />
        </div>
        <div className="flex flex-wrap gap-x-6 gap-y-1 mt-3 text-sm text-ink-soft">
          <span className="inline-flex items-center gap-2"><i className="w-2.5 h-2.5 rounded-sm bg-lagoon-600" />Reservations</span>
          <span className="inline-flex items-center gap-2"><i className="w-2.5 h-2.5 rounded-sm bg-sun-500" />Expenses</span>
          <span className="inline-flex items-center gap-2"><i className="w-2.5 h-2.5 rounded-sm bg-slate-200" />{inr(Math.max(0, remaining))} remaining</span>
        </div>
      </Card>

      <div className="grid lg:grid-cols-[1.6fr_1fr] gap-6 items-start">
        <Card className="p-6">
          <SectionHeader title={`Travelers (${trip.members.length})`}>
            {isAdmin && <Button variant="ghost" size="sm" onClick={() => setAdding(true)}><UserPlus size={15} />Add traveler</Button>}
          </SectionHeader>
          <ul className="divide-y divide-line mt-4">
            {trip.members.map((m) => (
              <li key={m.userId} className="flex items-start gap-3 py-3.5">
                <Avatar name={m.name} size="lg" />
                <div className="flex-1 min-w-0">
                  <p className="font-medium flex items-center gap-2 flex-wrap">{m.name}
                    {m.userId === trip.adminId ? <Badge tone="sun">Trip admin</Badge> : <Badge tone="gray">{roleLabel(m.role)}</Badge>}
                  </p>
                  <p className="text-sm text-ink-soft truncate">{m.email}</p>
                  {m.preferences?.length > 0 && <div className="flex flex-wrap gap-1.5 mt-2">{m.preferences.map((p) => <span key={p} className="text-xs px-2 py-0.5 rounded-full bg-lagoon-50 text-lagoon-700">{p}</span>)}</div>}
                </div>
                {isAdmin && m.userId !== trip.adminId && (
                  <Button variant="quiet" size="sm" onClick={() => confirm(`Remove ${m.name} from this trip?`) && run(() => api.removeMember(trip.tripId, m.userId))}>Remove</Button>
                )}
              </li>
            ))}
          </ul>
        </Card>

        <Card className="p-6">
          <h3 className="font-display font-semibold text-lg">Trip at a glance</h3>
          <dl className="mt-4 space-y-3 text-sm">
            {[[MapPin, 'Destinations', trip.destinations.length], [Vote, 'Activities', acts.length], [Ticket, 'Reservations', trip.reservations.length], [Receipt, 'Expenses logged', trip.expenses.length]].map(([Icon, l, v]) => (
              <div key={l} className="flex items-center justify-between"><dt className="flex items-center gap-2 text-ink-soft"><Icon size={16} />{l}</dt><dd className="font-medium">{v}</dd></div>
            ))}
          </dl>
        </Card>
      </div>

      {adding && (
        <FormModal title="Add traveler" submitLabel="Add and invite" initial={{ email: '' }} onClose={() => setAdding(false)}
          onSubmit={(f) => run(() => api.addMember(trip.tripId, f.email))}>
          {(f, set) => <>
            <Field label="Email of a registered user"><Input type="email" required autoFocus value={f.email} onChange={set('email')} placeholder="friend@example.com" /></Field>
            <p className="flex gap-2 text-sm text-ink-soft"><Mail size={16} className="shrink-0 mt-0.5" />They get an invitation on the channels they chose in their profile (Gmail, WhatsApp).</p>
          </>}
        </FormModal>
      )}
    </div>
  )
}
