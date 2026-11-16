// Net balance per user = (paid for the group) - (own shares), adjusted by completed payments.
// Positive => is owed money. Negative => owes money.
export function computeBalances(trip) {
  const net = Object.fromEntries(trip.members.map((m) => [m.userId, 0]))
  const add = (id, v) => { net[id] = (net[id] || 0) + v }
  trip.expenses.forEach((e) => {
    add(e.paidBy, e.amount)
    e.splits.forEach((s) => add(s.userId, -s.shareAmount))
  })
  trip.payments.filter((p) => p.status === 'COMPLETED').forEach((p) => {
    add(p.fromUserId, p.amount); add(p.toUserId, -p.amount)
  })
  // greedy simplification of debts
  const cr = [], db = []
  Object.entries(net).forEach(([id, v]) => {
    const r = Math.round(v * 100) / 100
    if (r > 0.009) cr.push({ id, v: r }); else if (r < -0.009) db.push({ id, v: -r })
  })
  cr.sort((a, b) => b.v - a.v); db.sort((a, b) => b.v - a.v)
  const transfers = []
  let i = 0, j = 0
  while (i < cr.length && j < db.length) {
    const amt = Math.min(cr[i].v, db[j].v)
    transfers.push({ from: db[j].id, to: cr[i].id, amount: Math.round(amt * 100) / 100 })
    cr[i].v -= amt; db[j].v -= amt
    if (cr[i].v < 0.01) i++
    if (db[j].v < 0.01) j++
  }
  return { net, transfers }
}

export function totals(trip) {
  const reserved = trip.reservations.filter((r) => r.status !== 'CANCELLED').reduce((s, r) => s + r.cost, 0)
  const expenses = trip.expenses.reduce((s, e) => s + e.amount, 0)
  const planned = trip.destinations.flatMap((d) => d.activities).filter((a) => a.status === 'PLANNED').reduce((s, a) => s + a.estimatedCost, 0)
  return { reserved, expenses, planned, spent: reserved + expenses }
}
