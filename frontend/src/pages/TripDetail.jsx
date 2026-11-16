import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft, Bell, CalendarDays, FileText, LayoutDashboard,
  Map, Pencil, Receipt, Ticket, Trash2, UserRound
} from 'lucide-react';

import { api } from '../api/client.js';
import { useAuth } from '../context/AuthContext.jsx';
import { Button, Empty, daysBetween, fmtRange } from '../components/ui.jsx';
import { CoverArt } from '../components/Scene.jsx';
import TripForm from './TripForm.jsx';

import Overview from '../tabs/Overview.jsx';
import Itinerary from '../tabs/Itinerary.jsx';
import Reservations from '../tabs/Reservations.jsx';
import Expenses from '../tabs/Expenses.jsx';
import Notifications from '../tabs/Notifications.jsx';
import Documents from '../tabs/Documents.jsx';

const TABS = [
  ['overview', 'Overview', LayoutDashboard],
  ['itinerary', 'Itinerary', Map],
  ['reservations', 'Reservations', Ticket],
  ['expenses', 'Expenses', Receipt],
  ['notifications', 'Notifications', Bell],
  ['documents', 'Documents', FileText],
];

export default function TripDetail() {
  const { tripId } = useParams();
  const { user } = useAuth();
  const nav = useNavigate();

  const [trip, setTrip] = useState(null);
  const [error, setError] = useState('');
  const [tab, setTab] = useState('overview');
  const [editing, setEditing] = useState(false);

  const refresh = useCallback(async () => {
    try {
      const result = await api.getTrip(tripId);
      setTrip(result?.data || result);
      setError('');
    } catch (e) {
      setError(e.message);
    }
  }, [tripId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  if (error) {
    return (
      <Empty icon={Map} title="Couldn't open this trip"
        action={<Link to="/trips"><Button variant="ghost">Back to trips</Button></Link>}>
        {error}
      </Empty>
    );
  }

  if (!trip) return <p className="py-20 text-center">Loading trip...</p>;

  const isAdmin = trip.adminId === user?.userId ||
    ['TRIP_ADMIN', 'ADMIN'].includes(String(user?.role || '').toUpperCase());

  const ctx = { trip, user, isAdmin, refresh };
  ctx.run = async (fn) => {
    await fn();
    await refresh();
  };

  const days = daysBetween(trip.startDate, trip.endDate) + 1;

  return (
    <>
      <Link to="/trips" className="inline-flex items-center gap-1.5 text-sm text-ink-soft">
        <ArrowLeft size={15} /> All trips
      </Link>

      <section className="relative mt-3 overflow-hidden rounded-3xl text-white">
        <CoverArt seed={trip.tripName} className="absolute inset-0 h-full w-full" />
        <div className="absolute inset-0 bg-gradient-to-t from-ink/80 via-ink/25 to-transparent" />
        <div className="relative flex flex-wrap items-end justify-between gap-4 p-6 pt-24 sm:p-8 sm:pt-32">
          <div>
            <h1 className="font-display text-3xl font-semibold sm:text-4xl">{trip.tripName}</h1>
            <p className="mt-2 flex flex-wrap items-center gap-4 text-sm text-white/90">
              <span className="inline-flex items-center gap-1.5">
                <CalendarDays size={15} /> {fmtRange(trip.startDate, trip.endDate)} ({days} days)
              </span>
              <span className="inline-flex items-center gap-1.5">
                <UserRound size={15} /> Admin: {trip.admin?.name || 'Trip admin'}
              </span>
            </p>
          </div>

          {isAdmin && (
            <div className="flex gap-2">
              <Button variant="onDark" onClick={() => setEditing(true)}>
                <Pencil size={15} /> Edit trip
              </Button>
              <Button variant="onDark" onClick={async () => {
                if (confirm('Delete this trip and all its data?')) {
                  await api.deleteTrip(tripId);
                  nav('/trips');
                }
              }}>
                <Trash2 size={15} /> Delete
              </Button>
            </div>
          )}
        </div>
      </section>

      <div role="tablist" className="mt-6 mb-6 flex gap-1 overflow-x-auto border-b border-line">
        {TABS.map(([key, label, Icon]) => (
          <button key={key} role="tab" aria-selected={tab === key}
            onClick={() => setTab(key)}
            className={`-mb-px inline-flex items-center gap-2 whitespace-nowrap border-b-2 px-4 py-3 text-sm font-medium ${
              tab === key ? 'border-lagoon-600 text-lagoon-700' : 'border-transparent text-ink-soft'
            }`}>
            <Icon size={16} /> {label}
          </button>
        ))}
      </div>

      {tab === 'overview' && <Overview {...ctx} />}
      {tab === 'itinerary' && <Itinerary {...ctx} />}
      {tab === 'reservations' && <Reservations {...ctx} />}
      {tab === 'expenses' && <Expenses {...ctx} />}
      {tab === 'notifications' && <Notifications {...ctx} />}
      {tab === 'documents' && <Documents {...ctx} />}

      {editing && (
        <TripForm
          title="Edit trip"
          initial={trip}
          onClose={() => setEditing(false)}
          onSubmit={(form) => ctx.run(() => api.updateTrip(tripId, form))}
        />
      )}
    </>
  );
}
