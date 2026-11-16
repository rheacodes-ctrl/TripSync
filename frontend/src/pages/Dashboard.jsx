
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  Plus,
  RefreshCw,
  MapPin,
  CalendarDays,
  Users,
  Wallet,
  X,
  Search,
  Plane,
  Compass,
  ArrowUpRight,
  Sparkles,
  Clock,
  Mountain,
  Palmtree,
  Globe2,
  ChevronRight,
} from "lucide-react";

import { api } from "../api/client.js";
import { useAuth } from "../context/AuthContext.jsx";

const DESTINATIONS = [
  {
    name: "Goa",
    country: "India",
    tag: "Beach escapes",
    image:
      "https://images.unsplash.com/photo-1512343879784-a960bf40e7f2?w=900",
  },
  {
    name: "Manali",
    country: "India",
    tag: "Mountain adventures",
    image:
      "https://images.unsplash.com/photo-1626621341517-bbf3d9990a23?w=900",
  },
  {
    name: "Jaipur",
    country: "India",
    tag: "Culture & heritage",
    image:
      "https://images.unsplash.com/photo-1599661046827-dacff0c0f09a?w=900",
  },
  {
    name: "Bali",
    country: "Indonesia",
    tag: "Tropical getaway",
    image:
      "https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=900",
  },
];

const formatDate = (date) => {
  if (!date) return "Date not set";

  return new Date(`${date}T00:00:00`).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
};

const formatMoney = (amount) =>
  "₹" +
  Number(amount || 0).toLocaleString("en-IN", {
    maximumFractionDigits: 0,
  });

const getTripStatus = (trip) => {
  const today = new Date().toISOString().slice(0, 10);

  if (today < trip.startDate) return "Upcoming";
  if (today > trip.endDate) return "Completed";
  return "Ongoing";
};

const getTripImage = (trip) => {
  const name = `${trip.tripName || ""} ${trip.description || ""}`.toLowerCase();

  if (/goa|beach|island|bali|sea/.test(name)) return DESTINATIONS[0].image;
  if (/manali|mountain|snow|hill|shimla/.test(name))
    return DESTINATIONS[1].image;
  if (/jaipur|rajasthan|heritage/.test(name))
    return DESTINATIONS[2].image;

  return DESTINATIONS[3].image;
};

export default function Dashboard() {
  const { user } = useAuth();

  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("All");
  const [success, setSuccess] = useState("");

  const [form, setForm] = useState({
    tripName: "",
    description: "",
    startDate: "",
    endDate: "",
    budget: "",
  });

  const isAdmin = ["TRIP_ADMIN", "ADMIN"].includes(String(user?.role || "").toUpperCase());

  const load = useCallback(async () => {
    if (!user?.userId) {
      setError("Your login session is missing. Please log in again.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");

    try {
      const result = await api.listTrips(user.userId);

      const data = Array.isArray(result)
        ? result
        : result?.data || result?.trips || [];

      setTrips(data);
    } catch (err) {
      console.error("Unable to load trips:", err);
      setError(err.message || "Unable to load your trips.");
    } finally {
      setLoading(false);
    }
  }, [user?.userId]);

  useEffect(() => {
    load();
  }, [load]);

  const filteredTrips = useMemo(() => {
    return trips.filter((trip) => {
      const matchesSearch =
        `${trip.tripName} ${trip.description || ""}`
          .toLowerCase()
          .includes(search.toLowerCase());

      const status = getTripStatus(trip);
      const matchesFilter = filter === "All" || status === filter;

      return matchesSearch && matchesFilter;
    });
  }, [trips, search, filter]);

  const upcomingCount = trips.filter(
    (trip) => getTripStatus(trip) === "Upcoming"
  ).length;

  const updateField = (event) => {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const closeModal = () => {
    if (saving) return;

    setOpen(false);
    setError("");
    setForm({
      tripName: "",
      description: "",
      startDate: "",
      endDate: "",
      budget: "",
    });
  };

  const createTrip = async (event) => {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!user?.userId) {
      setError("Please log in again before creating a trip.");
      return;
    }

    if (!form.tripName.trim()) {
      setError("Please enter a trip name.");
      return;
    }

    if (!form.startDate || !form.endDate) {
      setError("Please select both travel dates.");
      return;
    }

    if (form.endDate < form.startDate) {
      setError("End date cannot be before the start date.");
      return;
    }

    if (!Number.isFinite(Number(form.budget)) || Number(form.budget) <= 0) {
      setError("Please enter a valid trip budget.");
      return;
    }

    setSaving(true);

    try {
      await api.createTrip({
        tripName: form.tripName.trim(),
        description: form.description.trim(),
        startDate: form.startDate,
        endDate: form.endDate,
        budget: Number(form.budget),
        adminId: user.userId,
      });

      setForm({
        tripName: "",
        description: "",
        startDate: "",
        endDate: "",
        budget: "",
      });

      setOpen(false);
      setSuccess("Your new adventure has been created!");
      await load();
    } catch (err) {
      console.error("Trip creation failed:", err);
      setError(err.message || "Could not create your trip.");
    } finally {
      setSaving(false);
    }
  };

  const totalBudget = trips.reduce(
    (sum, trip) => sum + Number(trip.budget || 0),
    0
  );

  return (
    <main className="min-h-screen bg-[#F7F5EF] text-[#263B32] pb-16">

      {/* Travel hero */}
      <section className="relative overflow-hidden bg-[#244D3C] text-white">
        <div className="absolute inset-0">
          <img
            src="https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=1800"
            alt="Beautiful scenic travel landscape"
            className="h-full w-full object-cover opacity-35"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-[#163A2D]/95 via-[#244D3C]/75 to-transparent" />
        </div>

        <div className="relative mx-auto max-w-7xl px-5 py-14 md:px-10 md:py-20">
          <div className="flex flex-col justify-between gap-10 md:flex-row md:items-center">

            <div className="max-w-2xl">
              <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-4 py-2 text-sm text-white/90 backdrop-blur">
                <Sparkles size={16} />
                Your adventures begin here
              </div>

              <h1 className="font-serif text-4xl font-semibold leading-tight md:text-6xl">
                Hello, {user?.name || "Traveler"}!
                <br />
                <span className="text-[#E8B98D]">
                  Where are we going next?
                </span>
              </h1>

              <p className="mt-5 max-w-lg text-base leading-relaxed text-white/75 md:text-lg">
                Gather your favourite people, discover new places and
                create unforgettable journeys together.
              </p>

              <div className="mt-8 flex flex-wrap gap-3">
                {isAdmin && (
                  <button
                    onClick={() => setOpen(true)}
                    className="inline-flex items-center gap-2 rounded-full bg-[#E8B98D] px-7 py-3.5 font-semibold text-[#263B32] transition hover:-translate-y-1 hover:bg-[#F0C9A5]"
                  >
                    <Plus size={19} />
                    Plan a new trip
                  </button>
                )}

                <a
                  href="#discover"
                  className="inline-flex items-center gap-2 rounded-full border border-white/40 px-7 py-3.5 font-semibold text-white transition hover:bg-white/10"
                >
                  <Compass size={18} />
                  Explore destinations
                </a>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 md:w-72">
              <div className="rounded-2xl border border-white/15 bg-white/10 p-5 backdrop-blur">
                <Plane className="mb-4 text-[#E8B98D]" size={25} />
                <p className="text-3xl font-semibold">{trips.length}</p>
                <p className="mt-1 text-sm text-white/70">Total trips</p>
              </div>

              <div className="rounded-2xl border border-white/15 bg-white/10 p-5 backdrop-blur">
                <CalendarDays className="mb-4 text-[#E8B98D]" size={25} />
                <p className="text-3xl font-semibold">{upcomingCount}</p>
                <p className="mt-1 text-sm text-white/70">Upcoming</p>
              </div>

              <div className="col-span-2 rounded-2xl border border-white/15 bg-white/10 p-5 backdrop-blur">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-white/70">Total planned budget</p>
                    <p className="mt-1 text-2xl font-semibold">
                      {formatMoney(totalBudget)}
                    </p>
                  </div>
                  <Wallet size={29} className="text-[#E8B98D]" />
                </div>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* Main content */}
      <div className="mx-auto max-w-7xl px-5 md:px-10">

        {/* Success */}
        {success && (
          <div className="mt-6 flex items-center justify-between rounded-xl bg-green-100 p-4 text-green-800">
            <p>{success}</p>
            <button onClick={() => setSuccess("")}>
              <X size={18} />
            </button>
          </div>
        )}

        {/* Trips */}
        <section className="pt-14">

          <div className="flex flex-col justify-between gap-5 md:flex-row md:items-end">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-[#A77C53]">
                Your travel journal
              </p>

              <h2 className="mt-2 font-serif text-3xl font-semibold md:text-4xl">
                Your trips
              </h2>

              <p className="mt-2 text-[#78867D]">
                All your adventures, planned and upcoming.
              </p>
            </div>

            <button
              onClick={load}
              disabled={loading}
              className="inline-flex items-center justify-center gap-2 rounded-full border border-[#D7DDD5] bg-white px-5 py-3 text-sm font-medium transition hover:bg-[#EDF0E9] disabled:opacity-50"
            >
              <RefreshCw size={16} className={loading ? "animate-spin" : ""} />
              Refresh trips
            </button>
          </div>

          {/* Search and filters */}
          <div className="mt-7 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">

            <div className="relative w-full sm:max-w-sm">
              <Search
                size={18}
                className="absolute left-4 top-1/2 -translate-y-1/2 text-[#89968D]"
              />
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search your adventures..."
                className="w-full rounded-full border border-[#DCE1D8] bg-white py-3 pl-11 pr-4 outline-none transition focus:border-[#658B72]"
              />
            </div>

            <div className="flex flex-wrap gap-2">
              {["All", "Upcoming", "Ongoing", "Completed"].map((item) => (
                <button
                  key={item}
                  onClick={() => setFilter(item)}
                  className={`rounded-full px-5 py-2.5 text-sm font-medium transition ${
                    filter === item
                      ? "bg-[#244D3C] text-white"
                      : "border border-[#DCE1D8] bg-white text-[#52665A] hover:bg-[#EDF0E9]"
                  }`}
                >
                  {item}
                </button>
              ))}
            </div>

          </div>

          {/* Loading */}
          {loading && (
            <div className="py-20 text-center">
              <RefreshCw
                className="mx-auto animate-spin text-[#658B72]"
                size={32}
              />
              <p className="mt-4 text-[#78867D]">
                Gathering your adventures...
              </p>
            </div>
          )}

          {/* Error */}
          {!loading && error && !open && (
            <div className="mt-8 rounded-2xl border border-red-200 bg-red-50 p-6">
              <p className="font-semibold text-red-800">
                Couldn't load your trips
              </p>
              <p className="mt-2 text-sm text-red-700">{error}</p>
              <button
                onClick={load}
                className="mt-4 rounded-full bg-red-700 px-5 py-2 text-sm font-semibold text-white"
              >
                Try again
              </button>
            </div>
          )}

          {/* Empty state */}
          {!loading && !error && trips.length === 0 && (
            <div className="mt-8 rounded-3xl border border-dashed border-[#CBD5C9] bg-white p-12 text-center">
              <Globe2 className="mx-auto text-[#658B72]" size={42} />

              <h3 className="mt-5 font-serif text-2xl font-semibold">
                Your travel story starts here
              </h3>

              <p className="mx-auto mt-2 max-w-md text-[#78867D]">
                You haven't joined any trips yet. Create your first
                adventure and start planning with your friends.
              </p>

              {isAdmin && (
                <button
                  onClick={() => setOpen(true)}
                  className="mt-7 inline-flex items-center gap-2 rounded-full bg-[#244D3C] px-7 py-3.5 font-semibold text-white transition hover:bg-[#37664F]"
                >
                  <Plus size={18} />
                  Create your first trip
                </button>
              )}
            </div>
          )}

          {/* Trip cards */}
          {!loading && !error && filteredTrips.length > 0 && (
            <div className="mt-8 grid gap-7 sm:grid-cols-2 lg:grid-cols-3">

              {filteredTrips.map((trip) => {
                const status = getTripStatus(trip);

                return (
                  <Link
                    to={`/trips/${trip.tripId}`}
                    key={trip.tripId}
                    className="group overflow-hidden rounded-3xl border border-[#E1E4DD] bg-white shadow-sm transition duration-300 hover:-translate-y-1 hover:shadow-xl"
                  >
                    <div className="relative h-56 overflow-hidden">
                      <img
                        src={trip.coverImage || getTripImage(trip)}
                        alt={trip.tripName}
                        className="h-full w-full object-cover transition duration-700 group-hover:scale-110"
                      />

                      <div className="absolute inset-0 bg-gradient-to-t from-black/65 to-transparent" />

                      <span className="absolute left-4 top-4 rounded-full bg-white/90 px-3 py-1.5 text-xs font-semibold text-[#244D3C]">
                        {status}
                      </span>

                      <div className="absolute bottom-5 left-5 right-5 text-white">
                        <p className="flex items-center gap-1.5 text-sm text-white/80">
                          <MapPin size={15} />
                          Your next destination
                        </p>
                        <h3 className="mt-1 font-serif text-2xl font-semibold">
                          {trip.tripName}
                        </h3>
                      </div>
                    </div>

                    <div className="p-5">
                      <p className="line-clamp-2 min-h-10 text-sm text-[#78867D]">
                        {trip.description || "An adventure waiting to happen."}
                      </p>

                      <div className="mt-5 space-y-3 text-sm text-[#52665A]">
                        <div className="flex items-center gap-3">
                          <CalendarDays size={17} className="text-[#A77C53]" />
                          <span>
                            {formatDate(trip.startDate)} – {formatDate(trip.endDate)}
                          </span>
                        </div>

                        <div className="flex items-center gap-3">
                          <Wallet size={17} className="text-[#A77C53]" />
                          <span>Budget: {formatMoney(trip.budget)}</span>
                        </div>

                        <div className="flex items-center gap-3">
                          <Users size={17} className="text-[#A77C53]" />
                          <span>{trip.memberCount ?? trip.members?.length ?? 1} travellers</span>
                        </div>
                      </div>

                      <div className="mt-6 flex items-center justify-between border-t border-[#EDF0E9] pt-4">
                        <span className="text-sm font-semibold text-[#244D3C]">
                          Explore trip
                        </span>
                        <ArrowUpRight
                          size={19}
                          className="text-[#658B72] transition group-hover:-translate-y-1 group-hover:translate-x-1"
                        />
                      </div>
                    </div>
                  </Link>
                );
              })}

            </div>
          )}

          {!loading && !error && trips.length > 0 && filteredTrips.length === 0 && (
            <div className="py-16 text-center">
              <Search size={30} className="mx-auto text-[#89968D]" />
              <p className="mt-3 font-semibold">No matching trips found.</p>
              <button
                onClick={() => {
                  setSearch("");
                  setFilter("All");
                }}
                className="mt-3 text-sm font-semibold text-[#658B72] underline"
              >
                Clear filters
              </button>
            </div>
          )}

        </section>

        {/* Destination inspiration */}
        <section id="discover" className="pt-20">
          <div className="flex items-end justify-between">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-[#A77C53]">
                Get inspired
              </p>
              <h2 className="mt-2 font-serif text-3xl font-semibold md:text-4xl">
                Somewhere beautiful awaits.
              </h2>
              <p className="mt-2 text-[#78867D]">
                A little inspiration for your next getaway.
              </p>
            </div>
            <Compass className="hidden text-[#658B72] md:block" size={34} />
          </div>

          <div className="mt-8 grid grid-cols-2 gap-4 lg:grid-cols-4">
            {DESTINATIONS.map((destination) => (
              <div
                key={destination.name}
                className="group relative h-64 overflow-hidden rounded-3xl sm:h-80"
              >
                <img
                  src={destination.image}
                  alt={destination.name}
                  loading="lazy"
                  className="h-full w-full object-cover transition duration-700 group-hover:scale-110"
                />

                <div className="absolute inset-0 bg-gradient-to-t from-black/75 via-transparent to-transparent" />

                <div className="absolute bottom-5 left-5 text-white">
                  <p className="text-xs font-medium uppercase tracking-wider text-white/75">
                    {destination.tag}
                  </p>
                  <h3 className="mt-1 font-serif text-2xl font-semibold">
                    {destination.name}
                  </h3>
                  <p className="text-sm text-white/75">{destination.country}</p>
                </div>
              </div>
            ))}
          </div>
        </section>

      </div>

      {/* Create trip modal */}
      {open && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/60 p-4 backdrop-blur-sm"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget) closeModal();
          }}
        >
          <div className="my-auto w-full max-w-xl rounded-3xl bg-[#F7F5EF] p-6 shadow-2xl md:p-9">

            <div className="flex items-start justify-between">
              <div>
                <p className="flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.2em] text-[#A77C53]">
                  <Sparkles size={14} />
                  Your next adventure
                </p>

                <h2 className="mt-2 font-serif text-3xl font-semibold text-[#244D3C]">
                  Plan a new trip
                </h2>

                <p className="mt-2 text-sm text-[#78867D]">
                  Every great journey starts with an idea.
                </p>
              </div>

              <button
                type="button"
                onClick={closeModal}
                className="rounded-full p-2 text-[#78867D] transition hover:bg-[#E8EDE5]"
                aria-label="Close modal"
              >
                <X size={22} />
              </button>
            </div>

            <form onSubmit={createTrip} className="mt-7 space-y-5">

              <div>
                <label className="mb-2 block text-sm font-semibold text-[#52665A]">
                  What shall we call this trip?
                </label>
                <input
                  name="tripName"
                  value={form.tripName}
                  onChange={updateField}
                  placeholder="e.g. Goa getaway with friends"
                  required
                  maxLength={100}
                  className="w-full rounded-xl border border-[#DCE1D8] bg-white px-4 py-3.5 text-[#263B32] outline-none transition focus:border-[#658B72] focus:ring-2 focus:ring-[#658B72]/20"
                />
              </div>

              <div>
                <label className="mb-2 block text-sm font-semibold text-[#52665A]">
                  Tell us about the adventure
                </label>
                <textarea
                  name="description"
                  value={form.description}
                  onChange={updateField}
                  placeholder="Beach days, mountain views, food trails..."
                  rows={3}
                  className="w-full resize-none rounded-xl border border-[#DCE1D8] bg-white px-4 py-3.5 text-[#263B32] outline-none transition focus:border-[#658B72] focus:ring-2 focus:ring-[#658B72]/20"
                />
              </div>

              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label className="mb-2 block text-sm font-semibold text-[#52665A]">
                    Departure date
                  </label>
                  <input
                    type="date"
                    name="startDate"
                    value={form.startDate}
                    onChange={updateField}
                    required
                    className="w-full rounded-xl border border-[#DCE1D8] bg-white px-4 py-3 text-[#263B32] outline-none focus:border-[#658B72]"
                  />
                </div>

                <div>
                  <label className="mb-2 block text-sm font-semibold text-[#52665A]">
                    Return date
                  </label>
                  <input
                    type="date"
                    name="endDate"
                    value={form.endDate}
                    onChange={updateField}
                    min={form.startDate || undefined}
                    required
                    className="w-full rounded-xl border border-[#DCE1D8] bg-white px-4 py-3 text-[#263B32] outline-none focus:border-[#658B72]"
                  />
                </div>
              </div>

              <div>
                <label className="mb-2 block text-sm font-semibold text-[#52665A]">
                  Estimated total budget (₹)
                </label>
                <div className="relative">
                  <Wallet
                    size={18}
                    className="absolute left-4 top-1/2 -translate-y-1/2 text-[#89968D]"
                  />
                  <input
                    type="number"
                    name="budget"
                    value={form.budget}
                    onChange={updateField}
                    placeholder="e.g. 60000"
                    min="1"
                    required
                    className="w-full rounded-xl border border-[#DCE1D8] bg-white py-3.5 pl-11 pr-4 text-[#263B32] outline-none focus:border-[#658B72]"
                  />
                </div>
              </div>

              {error && (
                <div className="rounded-xl bg-red-50 p-3 text-sm text-red-700">
                  {error}
                </div>
              )}

              <div className="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
                <button
                  type="button"
                  onClick={closeModal}
                  disabled={saving}
                  className="rounded-full border border-[#CBD5C9] px-6 py-3 font-semibold text-[#52665A] hover:bg-[#E8EDE5]"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={saving}
                  className="inline-flex items-center justify-center gap-2 rounded-full bg-[#244D3C] px-7 py-3 font-semibold text-white transition hover:bg-[#37664F] disabled:opacity-50"
                >
                  {saving ? (
                    <>
                      <RefreshCw size={17} className="animate-spin" />
                      Creating...
                    </>
                  ) : (
                    <>
                      <Plus size={18} />
                      Create my trip
                    </>
                  )}
                </button>
              </div>

            </form>
          </div>
        </div>
      )}

    </main>
  );
}
