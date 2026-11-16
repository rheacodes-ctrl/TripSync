import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { ChevronDown, LogOut, RotateCcw, UserRound } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
import { Avatar, Badge, Logo } from './ui.jsx'
import { isMock, resetDb } from '../api/client.js'

export const roleLabel = (r) => (r === 'TRIP_ADMIN' ? 'Trip admin' : 'Traveler')

function UserMenu() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  const [open, setOpen] = useState(false)
  const ref = useRef()
  useEffect(() => {
    const h = (e) => { if (!ref.current?.contains(e.target)) setOpen(false) }
    document.addEventListener('mousedown', h)
    return () => document.removeEventListener('mousedown', h)
  }, [])
  return (
    <div className="relative" ref={ref}>
      <button onClick={() => setOpen(!open)} className="flex items-center gap-2 rounded-full pl-1 pr-2 py-1 hover:bg-mist" aria-haspopup="menu" aria-expanded={open}>
        <Avatar name={user.name} />
        <span className="hidden sm:block text-sm font-medium">{user.name}</span>
        <ChevronDown size={15} className="text-ink-soft" />
      </button>
      {open && (
        <div role="menu" className="fade-in absolute right-0 mt-2 w-60 bg-white rounded-xl border border-line shadow-xl p-2">
          <div className="px-3 py-2">
            <p className="text-sm font-medium">{user.name}</p>
            <p className="text-xs text-ink-soft truncate">{user.email}</p>
            <div className="mt-2"><Badge tone={user.role === 'TRIP_ADMIN' ? 'sun' : 'blue'}>{roleLabel(user.role)}</Badge></div>
          </div>
          <div className="border-t border-line my-1" />
          <Link role="menuitem" to="/profile" onClick={() => setOpen(false)} className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm hover:bg-mist"><UserRound size={16} />Profile & preferences</Link>
          <button role="menuitem" onClick={() => { logout(); nav('/login') }} className="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm hover:bg-mist text-left"><LogOut size={16} />Log out</button>
        </div>
      )}
    </div>
  )
}

const navCls = ({ isActive }) => `px-3 py-2 rounded-lg text-sm font-medium transition-colors ${isActive ? 'text-lagoon-700 bg-lagoon-50' : 'text-ink-soft hover:text-ink hover:bg-mist'}`

export default function Layout() {
  return (
    <div className="min-h-screen flex flex-col">
      <header className="bg-white/90 backdrop-blur border-b border-line sticky top-0 z-40">
        <div className="max-w-6xl mx-auto px-4 h-16 flex items-center gap-6">
          <Link to="/trips" aria-label="TripSync home"><Logo /></Link>
          <nav className="flex gap-1">
            <NavLink to="/trips" className={navCls}>Trips</NavLink>
            <NavLink to="/profile" className={navCls}>Profile</NavLink>
          </nav>
          <div className="ml-auto flex items-center gap-3">
            {isMock && (
              <button className="hidden md:flex items-center gap-1.5 text-xs text-slate-500 hover:text-ink"
                onClick={() => { if (confirm('Reset demo data to the seed.sql values?')) { resetDb(); location.href = '/trips' } }}>
                <RotateCcw size={13} />Reset demo data
              </button>
            )}
            <UserMenu />
          </div>
        </div>
      </header>
      <main className="flex-1 w-full max-w-6xl mx-auto px-4 py-8"><Outlet /></main>
      <footer className="border-t border-line bg-white">
        <div className="max-w-6xl mx-auto px-4 py-6 flex flex-wrap items-center justify-between gap-3 text-sm text-ink-soft">
          <Logo className="!text-base" />
          <p>Plan together. Book together. Split fairly.</p>
        </div>
      </footer>
    </div>
  )
}
