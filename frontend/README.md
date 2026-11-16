# TripSync Frontend

React + Vite + Tailwind CSS v4 (plain JavaScript), with `lucide-react` icons. Place this folder as `frontend/` next to `src/` and `database/` in the TripSync repo.

## Run
```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```
Demo logins (from `seed.sql`): `ishi@example.com / admin123` (trip admin), `rhea@example.com / password123` (traveler). The login page also has one-click demo buttons.

## Pages
| Route | What it is |
|---|---|
| `/` | Public landing page |
| `/login` | Login / sign up with travel hero image, role picker (Traveler or Trip admin) |
| `/trips` | Dashboard: search, upcoming/past filter, trip cards with budget progress |
| `/trips/:id` | Trip page with tabs: Overview, Itinerary, Reservations, Expenses, Notifications, Documents |
| `/profile` | Profile, role capabilities, travel preferences, Gmail / WhatsApp notification channels |

## Requirement coverage (frontend side)
- **Users / roles:** Traveler and Trip admin accounts; admin-only actions are hidden for travelers (edit/delete trip, add/remove travelers, add destinations, manage reservations, activity status, reminders).
- **Trips:** create, edit, delete; name, description, dates, budget; add/remove travelers by email.
- **Destinations and activities:** city/country/description/dates; dates validated against the trip, activity dates against the stay; votes, join, status (Planned / Completed / Cancelled), estimated cost.
- **Reservations:** Hotel / Transport / Activity with type-specific details, Confirm / Cancel, running cost total.
- **Expenses:** equal or custom split, "who owes whom", outstanding balance, mark as paid (settlement history), send reminder.
- **Budget validation:** reservations and expenses that would pass the budget are rejected with a `BudgetExceededException` message.
- **Notifications:** invitation, reservation update, expense reminder and custom messages, shown per recipient and channel.

## Travel image
The login and landing pages use an illustrated travel scene (`src/components/Scene.jsx`) so there is nothing to download. To use a real photo instead, save it as `public/hero.jpg`; it is layered over the illustration automatically (remove the file to go back).

## Data source
`src/api/client.js` is the only file components use for data.
- `VITE_USE_MOCK=true` (default): data lives in localStorage, seeded from `seed.sql` values. Notifications are only logged, not sent.
- `VITE_USE_MOCK=false`: calls Spring Boot at `/api` (proxied to `localhost:8080` in `vite.config.js`). Copy `.env.example` to `.env` and flip the flag once the backend exists. Endpoints: see `REST.md`.

Passwords in the mock are plain text for demo only; the real backend must hash them (`password_hash`).
