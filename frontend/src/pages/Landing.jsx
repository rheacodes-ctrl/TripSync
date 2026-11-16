import { Link } from 'react-router-dom'
import { BellRing, CalendarRange, ShieldCheck, Ticket, Vote, Wallet } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
import { Avatar, Button, Logo, ProgressBar } from '../components/ui.jsx'
import { HeroScene } from '../components/Scene.jsx'

const FEATURES = [
  [CalendarRange, 'Itinerary that stays in order', 'Add stops with arrival and departure dates. Activities can only be scheduled inside the stay.'],
  [Vote, 'Vote before you commit', 'Everyone votes on activities and joins the ones they want. Costs are estimated up front.'],
  [Ticket, 'Bookings in one list', 'Hotels, transport and activity tickets with confirm and cancel, and a running cost total.'],
  [Wallet, 'Fair splits, clear balances', 'Split equally or by custom shares, see who owes whom, and record each settlement.'],
  [ShieldCheck, 'A budget that pushes back', 'Spending that would cross the trip budget is blocked with a clear message.'],
  [BellRing, 'Updates by email and WhatsApp', 'Invitations, booking changes and expense reminders reach people where they will see them.'],
]

const STEPS = [
  ['Create a trip', 'Name it, set the dates and a budget, then add your group by email.'],
  ['Build the plan together', 'Add destinations and activities, vote, and log bookings as they happen.'],
  ['Travel, then settle up', 'Record shared costs on the go. TripSync works out the fewest payments to clear everyone.'],
]

export default function Landing() {
  const { user } = useAuth()
  return (
    <div className="bg-white">
      <header className="absolute top-0 inset-x-0 z-20">
        <div className="max-w-6xl mx-auto px-4 h-20 flex items-center justify-between">
          <Logo light />
          <nav className="flex items-center gap-2">
            {user ? <Link to="/trips"><Button variant="sun">Open my trips</Button></Link> : <>
              <Link to="/login" className="hidden sm:block"><Button variant="onDark">Log in</Button></Link>
              <Link to="/login?mode=register"><Button variant="sun">Sign up free</Button></Link>
            </>}
          </nav>
        </div>
      </header>

      <section className="relative overflow-hidden min-h-[640px] lg:min-h-[720px] flex items-center">
        <HeroScene className="absolute inset-0 w-full h-full" />
        <div className="absolute inset-0 bg-cover bg-center" style={{ backgroundImage: 'url(/hero.jpg)' }} />
        <div className="absolute inset-0 bg-gradient-to-r from-ink/80 via-ink/40 to-transparent" />
        <div className="relative max-w-6xl mx-auto px-4 w-full grid lg:grid-cols-[1.2fr_1fr] gap-10 items-center pt-24 pb-16">
          <div className="text-white">
            <h1 className="font-display text-4xl sm:text-6xl font-semibold leading-[1.03] max-w-2xl">Plan the trip together. Settle up without the spreadsheet.</h1>
            <p className="mt-5 text-lg text-white/85 max-w-xl">TripSync keeps the itinerary, the bookings and the shared costs for your group in one place, and tells everyone when something changes.</p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to={user ? '/trips' : '/login?mode=register'}><Button variant="sun" size="lg">Start planning</Button></Link>
              {!user && <Link to="/login"><Button variant="onDark" size="lg">Log in</Button></Link>}
            </div>
          </div>

          {/* A real slice of the product, as the page’s characteristic moment */}
          <div className="hidden lg:block justify-self-end w-full max-w-sm bg-white rounded-2xl shadow-2xl p-5 text-ink">
            <div className="flex items-start justify-between">
              <div><p className="font-display font-semibold text-xl">Goa Vacation</p><p className="text-sm text-ink-soft">10 – 15 Jan 2027 · 3 stops</p></div>
              <div className="flex -space-x-2"><Avatar name="Ishi" ring /><Avatar name="Rhea" ring /><Avatar name="Shreya" ring /></div>
            </div>
            <div className="mt-5"><div className="flex justify-between text-sm mb-1.5"><span className="text-ink-soft">Budget used</span><span className="font-medium">₹25,000 of ₹60,000</span></div><ProgressBar value={25000} max={60000} /></div>
            <ul className="mt-5 space-y-2 text-sm">
              <li className="flex justify-between rounded-lg bg-mist px-3 py-2"><span>Water sports at Baga</span><span className="text-lagoon-700 font-medium">2 votes</span></li>
              <li className="flex justify-between rounded-lg bg-mist px-3 py-2"><span>Rhea paid for dinner</span><span className="text-ink-soft">₹3,000</span></li>
              <li className="flex justify-between rounded-lg bg-mist px-3 py-2"><span>Shreya owes Rhea</span><span className="text-coral-600 font-medium">₹1,000</span></li>
            </ul>
          </div>
        </div>
      </section>

      <section className="max-w-6xl mx-auto px-4 py-20">
        <h2 className="font-display text-3xl sm:text-4xl font-semibold max-w-2xl">Everything a group trip needs, nothing it doesn’t.</h2>
        <div className="mt-10 grid sm:grid-cols-2 lg:grid-cols-3 gap-x-8 gap-y-10">
          {FEATURES.map(([Icon, t, d]) => (
            <div key={t}>
              <span className="grid place-items-center w-11 h-11 rounded-xl bg-lagoon-50 text-lagoon-600"><Icon size={21} /></span>
              <h3 className="font-display font-semibold text-lg mt-4">{t}</h3>
              <p className="text-ink-soft mt-1.5 leading-relaxed">{d}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="bg-mist">
        <div className="max-w-6xl mx-auto px-4 py-20 grid lg:grid-cols-[1fr_1.4fr] gap-12">
          <h2 className="font-display text-3xl sm:text-4xl font-semibold">From first idea to final settlement</h2>
          <ol className="space-y-6">
            {STEPS.map(([t, d], i) => (
              <li key={t} className="flex gap-4">
                <span className="grid place-items-center w-9 h-9 rounded-full bg-lagoon-600 text-white font-display font-semibold shrink-0">{i + 1}</span>
                <div><h3 className="font-display font-semibold text-lg">{t}</h3><p className="text-ink-soft mt-1">{d}</p></div>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className="max-w-6xl mx-auto px-4 py-20 text-center">
        <h2 className="font-display text-3xl sm:text-4xl font-semibold">Your next trip starts here</h2>
        <p className="text-ink-soft mt-3">Create an account as a traveler or a trip admin.</p>
        <Link to={user ? '/trips' : '/login?mode=register'} className="inline-block mt-6"><Button size="lg">Create your account</Button></Link>
      </section>

      <footer className="border-t border-line">
        <div className="max-w-6xl mx-auto px-4 py-6 flex flex-wrap justify-between gap-3 text-sm text-ink-soft"><Logo className="!text-base" /><p>© {new Date().getFullYear()} TripSync</p></div>
      </footer>
    </div>
  )
}
