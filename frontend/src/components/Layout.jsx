import { Link, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { Button, Badge } from './ui.jsx'
import { isMock, resetDb } from '../api/client.js'

export default function Layout() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  return (
    <div className="min-h-screen">
      <header className="bg-white border-b border-slate-200 sticky top-0 z-40">
        <div className="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">
          <Link to="/" className="font-bold text-lg text-teal-700">🧭 TripSync</Link>
          <div className="flex items-center gap-3 text-sm">
            {isMock && (
              <button className="text-xs text-slate-400 hover:text-slate-700 hidden sm:block"
                onClick={() => { if (confirm('Reset demo data to the seed.sql values?')) { resetDb(); location.href = '/' } }}>
                Reset demo data
              </button>
            )}
            <span className="hidden sm:inline">{user.name}</span>
            <Badge tone={user.role === 'TRIP_ADMIN' ? 'blue' : 'gray'}>{user.role === 'TRIP_ADMIN' ? 'Admin' : 'Traveler'}</Badge>
            <Button variant="ghost" onClick={() => { logout(); nav('/login') }}>Log out</Button>
          </div>
        </div>
      </header>
      <main className="max-w-6xl mx-auto px-4 py-6"><Outlet /></main>
    </div>
  )
}
