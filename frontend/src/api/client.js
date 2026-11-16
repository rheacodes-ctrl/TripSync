// Single entry point for data. Components only import `api` from here.
// VITE_USE_MOCK=true (default) -> localStorage mock, no backend needed
// VITE_USE_MOCK=false          -> REST calls to Spring Boot at /api (contract: see REST.md)
import { load, save, uid, resetDb } from './mockDb.js'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'
const delay = (v) => new Promise((r) => setTimeout(() => r(structuredClone(v)), 100))

/* ---------------- REST implementation ---------------- */
async function http(method, path, body) {
  const token = localStorage.getItem('tripsync-token')
  const res = await fetch('/api' + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) throw new Error((await res.text()) || res.statusText)
  return res.status === 204 ? null : res.json()
}
const rest = {
  login: (email, password) => http('POST', '/auth/login', { email, password }),
  register: (d) => http('POST', '/auth/register', d),
  listTrips: (userId) => http('GET', `/users/${userId}/trips`),
  getTrip: (id) => http('GET', `/trips/${id}`),
  createTrip: (d) => http('POST', '/trips', d),
  updateTrip: (id, d) => http('PUT', `/trips/${id}`, d),
  deleteTrip: (id) => http('DELETE', `/trips/${id}`),
  addMember: (tripId, email) => http('POST', `/trips/${tripId}/members`, { email }),
  removeMember: (tripId, userId) => http('DELETE', `/trips/${tripId}/members/${userId}`),
  addDestination: (tripId, d) => http('POST', `/trips/${tripId}/destinations`, d),
  deleteDestination: (id) => http('DELETE', `/destinations/${id}`),
  addActivity: (destId, d) => http('POST', `/destinations/${destId}/activities`, d),
  setActivityStatus: (id, status) => http('PATCH', `/activities/${id}/status`, { status }),
  deleteActivity: (id) => http('DELETE', `/activities/${id}`),
  toggleVote: (id, userId) => http('POST', `/activities/${id}/votes`, { userId }),
  toggleParticipant: (id, userId) => http('POST', `/activities/${id}/participants`, { userId }),
  addReservation: (tripId, d) => http('POST', `/trips/${tripId}/reservations`, d),
  setReservationStatus: (id, status) => http('PATCH', `/reservations/${id}/status`, { status }),
  deleteReservation: (id) => http('DELETE', `/reservations/${id}`),
  addExpense: (tripId, d) => http('POST', `/trips/${tripId}/expenses`, d),
  deleteExpense: (id) => http('DELETE', `/expenses/${id}`),
  addPayment: (tripId, d) => http('POST', `/trips/${tripId}/payments`, d),
  addDocument: (tripId, d) => http('POST', `/trips/${tripId}/documents`, d),
  deleteDocument: (id) => http('DELETE', `/documents/${id}`),
}

/* ---------------- Mock implementation ---------------- */
const pub = ({ password, ...u }) => u
const commit = (v) => { save(); return delay(v ?? { ok: true }) }
const drop = (db, table, fn) => { db[table] = db[table].filter((x) => !fn(x)) }
const toggle = (db, table, key, id, userId) => {
  const has = db[table].some((x) => x[key] === id && x.userId === userId)
  if (has) drop(db, table, (x) => x[key] === id && x.userId === userId)
  else db[table].push({ [key]: id, userId })
}

const mock = {
  async login(email, password) {
    const u = load().users.find((x) => x.email.toLowerCase() === email.toLowerCase() && x.password === password)
    if (!u) throw new Error('Invalid email or password')
    return delay({ user: pub(u), token: 'mock' })
  },
  async register({ name, email, password, role, phone }) {
    const db = load()
    if (db.users.some((u) => u.email.toLowerCase() === email.toLowerCase())) throw new Error('Email already registered')
    const u = { userId: uid('U'), name, email, password, role, phone }
    db.users.push(u)
    return commit({ user: pub(u), token: 'mock' })
  },
  async listTrips(userId) {
    const db = load()
    const mine = db.tripMembers.filter((m) => m.userId === userId).map((m) => m.tripId)
    return delay(db.trips.filter((t) => mine.includes(t.tripId) || t.adminId === userId).map((t) => ({
      ...t,
      memberCount: db.tripMembers.filter((m) => m.tripId === t.tripId).length,
      destinationCount: db.destinations.filter((d) => d.tripId === t.tripId).length,
      spent: db.reservations.filter((r) => r.tripId === t.tripId && r.status !== 'CANCELLED').reduce((s, r) => s + r.cost, 0)
        + db.expenses.filter((e) => e.tripId === t.tripId).reduce((s, e) => s + e.amount, 0),
    })))
  },
  async getTrip(id) {
    const db = load()
    const trip = db.trips.find((t) => t.tripId === id)
    if (!trip) throw new Error('Trip not found')
    const users = db.users.map(pub)
    const sortBy = (k) => (a, b) => String(a[k]).localeCompare(String(b[k]))
    return delay({
      ...trip,
      admin: users.find((u) => u.userId === trip.adminId),
      members: db.tripMembers.filter((m) => m.tripId === id).map((m) => users.find((u) => u.userId === m.userId)).filter(Boolean),
      destinations: db.destinations.filter((d) => d.tripId === id).sort(sortBy('arrivalDate')).map((d) => ({
        ...d,
        activities: db.activities.filter((a) => a.destinationId === d.destinationId).sort(sortBy('activityDate')).map((a) => ({
          ...a,
          participantIds: db.activityParticipants.filter((p) => p.activityId === a.activityId).map((p) => p.userId),
          voterIds: db.activityVotes.filter((v) => v.activityId === a.activityId).map((v) => v.userId),
        })),
      })),
      expenses: db.expenses.filter((e) => e.tripId === id).sort((a, b) => b.expenseDate.localeCompare(a.expenseDate))
        .map((e) => ({ ...e, splits: db.expenseSplits.filter((s) => s.expenseId === e.expenseId) })),
      reservations: db.reservations.filter((r) => r.tripId === id),
      payments: db.payments.filter((p) => p.tripId === id),
      documents: db.documents.filter((d) => d.tripId === id),
    })
  },
  async createTrip(d) {
    const db = load()
    const t = { tripId: uid('T'), ...d, budget: Number(d.budget) }
    db.trips.push(t)
    db.tripMembers.push({ tripId: t.tripId, userId: d.adminId })
    return commit(t)
  },
  async updateTrip(id, d) { Object.assign(load().trips.find((x) => x.tripId === id), d, { budget: Number(d.budget) }); return commit() },
  async deleteTrip(id) {
    const db = load()
    const dIds = db.destinations.filter((d) => d.tripId === id).map((d) => d.destinationId)
    const aIds = db.activities.filter((a) => dIds.includes(a.destinationId)).map((a) => a.activityId)
    const eIds = db.expenses.filter((e) => e.tripId === id).map((e) => e.expenseId)
    drop(db, 'trips', (x) => x.tripId === id)
    ;['tripMembers', 'destinations', 'reservations', 'expenses', 'payments', 'documents'].forEach((t) => drop(db, t, (x) => x.tripId === id))
    ;['activities'].forEach((t) => drop(db, t, (x) => aIds.includes(x.activityId)))
    ;['activityParticipants', 'activityVotes'].forEach((t) => drop(db, t, (x) => aIds.includes(x.activityId)))
    drop(db, 'expenseSplits', (x) => eIds.includes(x.expenseId))
    return commit()
  },
  async addMember(tripId, email) {
    const db = load()
    const u = db.users.find((x) => x.email.toLowerCase() === email.toLowerCase())
    if (!u) throw new Error('No user registered with that email')
    if (db.tripMembers.some((m) => m.tripId === tripId && m.userId === u.userId)) throw new Error('Already a member')
    db.tripMembers.push({ tripId, userId: u.userId })
    return commit()
  },
  async removeMember(tripId, userId) { drop(load(), 'tripMembers', (m) => m.tripId === tripId && m.userId === userId); return commit() },
  async addDestination(tripId, d) { load().destinations.push({ destinationId: uid('D'), tripId, ...d }); return commit() },
  async deleteDestination(id) {
    const db = load()
    const aIds = db.activities.filter((a) => a.destinationId === id).map((a) => a.activityId)
    drop(db, 'destinations', (d) => d.destinationId === id)
    drop(db, 'activities', (a) => a.destinationId === id)
    drop(db, 'activityVotes', (v) => aIds.includes(v.activityId))
    drop(db, 'activityParticipants', (v) => aIds.includes(v.activityId))
    return commit()
  },
  async addActivity(destinationId, d) {
    load().activities.push({ activityId: uid('A'), destinationId, status: 'PLANNED', ...d, estimatedCost: Number(d.estimatedCost || 0) })
    return commit()
  },
  async setActivityStatus(id, status) { load().activities.find((a) => a.activityId === id).status = status; return commit() },
  async deleteActivity(id) {
    const db = load()
    drop(db, 'activities', (a) => a.activityId === id)
    drop(db, 'activityVotes', (v) => v.activityId === id)
    drop(db, 'activityParticipants', (v) => v.activityId === id)
    return commit()
  },
  async toggleVote(id, userId) { toggle(load(), 'activityVotes', 'activityId', id, userId); return commit() },
  async toggleParticipant(id, userId) { toggle(load(), 'activityParticipants', 'activityId', id, userId); return commit() },
  async addReservation(tripId, d) { load().reservations.push({ reservationId: uid('R'), tripId, status: 'PENDING', ...d, cost: Number(d.cost) }); return commit() },
  async setReservationStatus(id, status) { load().reservations.find((r) => r.reservationId === id).status = status; return commit() },
  async deleteReservation(id) { drop(load(), 'reservations', (r) => r.reservationId === id); return commit() },
  async addExpense(tripId, { description, amount, paidBy, expenseDate, participantIds }) {
    const db = load()
    const amt = Number(amount)
    const expenseId = uid('E')
    db.expenses.push({ expenseId, tripId, description, amount: amt, paidBy, expenseDate })
    // equal split, like Expense.splitEqually(); leftover paise goes to the first person
    const n = participantIds.length
    const base = Math.floor((amt * 100) / n) / 100
    const rem = Math.round((amt - base * n) * 100) / 100
    participantIds.forEach((userId, i) =>
      db.expenseSplits.push({ expenseId, userId, shareAmount: +(i === 0 ? base + rem : base).toFixed(2), isSettled: userId === paidBy }))
    return commit()
  },
  async deleteExpense(id) {
    const db = load()
    drop(db, 'expenses', (e) => e.expenseId === id)
    drop(db, 'expenseSplits', (s) => s.expenseId === id)
    return commit()
  },
  async addPayment(tripId, d) {
    load().payments.push({ paymentId: uid('P'), tripId, status: 'COMPLETED', paymentDate: new Date().toISOString().slice(0, 10), ...d, amount: Number(d.amount) })
    return commit()
  },
  async addDocument(tripId, d) { load().documents.push({ documentId: uid('DOC'), tripId, uploadedAt: new Date().toISOString().slice(0, 10), filePath: 'uploads/' + d.fileName, ...d }); return commit() },
  async deleteDocument(id) { drop(load(), 'documents', (d) => d.documentId === id); return commit() },
}

export const api = USE_MOCK ? mock : rest
export const isMock = USE_MOCK
export { resetDb }
