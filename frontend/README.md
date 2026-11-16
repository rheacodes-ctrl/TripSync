# TripSync Frontend

React + Vite + Tailwind CSS (plain JavaScript). Place this folder as `frontend/` next to `src/` and `database/` in the TripSync repo.

## Run
```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```
Demo logins (from `seed.sql`): `ishi@example.com / admin123` (admin), `rhea@example.com / password123`.

## Features
- Login / register (Traveler or Trip Admin)
- Trips dashboard with budget progress
- Trip page: Overview (budget, members), Itinerary (destinations, activities, votes, join),
  Reservations (hotel/transport/activity + status), Expenses (equal split, balances, suggested settlements, payments), Documents

## Data source
`src/api/client.js` is the only file components use for data.
- `VITE_USE_MOCK=true` (default): data lives in localStorage, seeded from `seed.sql` values.
- `VITE_USE_MOCK=false`: calls Spring Boot at `/api` (proxied to `localhost:8080` in `vite.config.js`).
  Copy `.env.example` to `.env` and flip the flag once the backend exists. Endpoints: see `REST.md`.

Passwords in the mock are plain text for demo only; the real backend must hash them (`password_hash`).
