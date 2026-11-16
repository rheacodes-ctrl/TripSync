// In-memory/localStorage database mirroring database/schema.sql and seed.sql.
// Lets the frontend run before the Spring Boot backend exists.
const KEY = 'tripsync-mock-db-v1'
const pairs = (keys, rows) => rows.map((r) => Object.fromEntries(keys.map((k, i) => [k, r[i]])))

const seed = {
  users: [
    { userId: 'U001', name: 'Ishi', email: 'ishi@example.com', password: 'admin123', role: 'TRIP_ADMIN', phone: '9876543210' },
    { userId: 'U002', name: 'Rhea', email: 'rhea@example.com', password: 'password123', role: 'TRAVELER', phone: '9876543211' },
    { userId: 'U003', name: 'Shreya', email: 'shreya@example.com', password: 'password123', role: 'TRAVELER', phone: '9876543212' },
  ],
  trips: [{ tripId: 'T001', tripName: 'Goa Vacation', description: 'A fun trip with friends', startDate: '2027-01-10', endDate: '2027-01-15', budget: 60000, adminId: 'U001' }],
  tripMembers: pairs(['tripId', 'userId'], [['T001', 'U001'], ['T001', 'U002'], ['T001', 'U003']]),
  destinations: [
    { destinationId: 'D001', tripId: 'T001', name: 'Baga Beach', city: 'North Goa', country: 'India', description: 'Popular beach destination', arrivalDate: '2027-01-10', departureDate: '2027-01-11' },
    { destinationId: 'D002', tripId: 'T001', name: 'Fort Aguada', city: 'Candolim', country: 'India', description: 'Historic Portuguese fort', arrivalDate: '2027-01-12', departureDate: '2027-01-12' },
    { destinationId: 'D003', tripId: 'T001', name: 'Anjuna Beach', city: 'Anjuna', country: 'India', description: 'Famous scenic beach', arrivalDate: '2027-01-13', departureDate: '2027-01-14' },
  ],
  activities: [
    { activityId: 'A001', destinationId: 'D001', name: 'Water Sports', description: 'Water sports at Baga', activityDate: '2027-01-10', activityTime: '10:00', estimatedCost: 1500, status: 'PLANNED' },
    { activityId: 'A002', destinationId: 'D001', name: 'Beach Dinner', description: 'Dinner by the beach', activityDate: '2027-01-11', activityTime: '19:00', estimatedCost: 2000, status: 'PLANNED' },
    { activityId: 'A003', destinationId: 'D002', name: 'Fort Tour', description: 'Explore the historic fort', activityDate: '2027-01-12', activityTime: '11:00', estimatedCost: 500, status: 'PLANNED' },
  ],
  activityParticipants: pairs(['activityId', 'userId'], [['A001', 'U002'], ['A001', 'U003'], ['A002', 'U001'], ['A002', 'U002'], ['A003', 'U001']]),
  activityVotes: pairs(['activityId', 'userId'], [['A001', 'U002'], ['A001', 'U003']]),
  reservations: [
    { reservationId: 'R001', tripId: 'T001', type: 'HOTEL', providerName: 'Taj Resort', cost: 12000, reservationDate: '2027-01-10', status: 'CONFIRMED' },
    { reservationId: 'R002', tripId: 'T001', type: 'TRANSPORT', providerName: 'IndiGo', cost: 8500, reservationDate: '2027-01-09', status: 'CONFIRMED' },
    { reservationId: 'R003', tripId: 'T001', type: 'ACTIVITY', providerName: 'Water Sports Provider', cost: 1500, reservationDate: '2027-01-10', status: 'CONFIRMED' },
  ],
  expenses: [{ expenseId: 'E001', tripId: 'T001', description: 'Group Dinner', amount: 3000, paidBy: 'U002', expenseDate: '2027-01-10' }],
  expenseSplits: [
    { expenseId: 'E001', userId: 'U001', shareAmount: 1000, isSettled: false },
    { expenseId: 'E001', userId: 'U002', shareAmount: 1000, isSettled: true },
    { expenseId: 'E001', userId: 'U003', shareAmount: 1000, isSettled: false },
  ],
  payments: [{ paymentId: 'P001', tripId: 'T001', fromUserId: 'U003', toUserId: 'U002', amount: 1000, status: 'COMPLETED', paymentDate: '2027-01-10' }],
  documents: [{ documentId: 'DOC001', tripId: 'T001', uploadedBy: 'U001', fileName: 'hotel-booking.pdf', filePath: 'uploads/hotel-booking.pdf', uploadedAt: '2027-01-08' }],
}

let cache = null
export function load() {
  if (cache) return cache
  try { cache = JSON.parse(localStorage.getItem(KEY)) } catch { /* ignore */ }
  if (!cache) cache = structuredClone(seed)
  return cache
}
export function save() { try { localStorage.setItem(KEY, JSON.stringify(cache)) } catch { /* ignore */ } }
export function resetDb() { cache = structuredClone(seed); save() }
export const uid = (p) => p + Math.random().toString(36).slice(2, 7).toUpperCase()
