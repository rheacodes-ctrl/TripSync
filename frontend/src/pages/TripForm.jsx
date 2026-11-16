import { useState } from "react";
import { X, Wallet, RefreshCw, Save } from "lucide-react";

export default function TripForm({ title = "Edit trip", initial = {}, onClose, onSubmit }) {
  const [form, setForm] = useState({
    tripName: initial.tripName || "",
    description: initial.description || "",
    startDate: initial.startDate || "",
    endDate: initial.endDate || "",
    budget: initial.budget ?? "",
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const updateField = (e) => {
    setForm((old) => ({ ...old, [e.target.name]: e.target.value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!form.tripName.trim()) return setError("Enter a trip name.");
    if (!form.startDate || !form.endDate) return setError("Select both dates.");
    if (form.endDate < form.startDate) return setError("Return date must be after departure.");
    if (!Number.isFinite(Number(form.budget)) || Number(form.budget) <= 0)
      return setError("Enter a valid budget.");

    setSaving(true);
    try {
      await onSubmit({
        tripName: form.tripName.trim(),
        description: form.description.trim(),
        startDate: form.startDate,
        endDate: form.endDate,
        budget: Number(form.budget),
      });
      onClose();
    } catch (err) {
      setError(err.message || "Unable to save changes.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/60 p-4 backdrop-blur-sm">
      <div className="my-auto w-full max-w-xl rounded-3xl bg-[#F7F5EF] p-6 shadow-2xl md:p-9">
        <div className="flex justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[#A77C53]">
              TripSync · Your journey
            </p>
            <h2 className="mt-2 font-serif text-3xl font-semibold text-[#244D3C]">{title}</h2>
            <p className="mt-2 text-sm text-[#78867D]">Update your trip details.</p>
          </div>
          <button type="button" onClick={onClose} disabled={saving} aria-label="Close">
            <X size={22} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="mt-7 space-y-5">
          <div>
            <label className="mb-2 block text-sm font-semibold">Trip name</label>
            <input name="tripName" value={form.tripName} onChange={updateField}
              required maxLength={100}
              className="w-full rounded-xl border bg-white px-4 py-3.5" />
          </div>

          <div>
            <label className="mb-2 block text-sm font-semibold">Description</label>
            <textarea name="description" value={form.description} onChange={updateField}
              rows={3} className="w-full rounded-xl border bg-white px-4 py-3.5" />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-2 block text-sm font-semibold">Departure date</label>
              <input type="date" name="startDate" value={form.startDate}
                onChange={updateField} required className="w-full rounded-xl border bg-white px-3 py-3" />
            </div>
            <div>
              <label className="mb-2 block text-sm font-semibold">Return date</label>
              <input type="date" name="endDate" value={form.endDate}
                min={form.startDate || undefined} onChange={updateField}
                required className="w-full rounded-xl border bg-white px-3 py-3" />
            </div>
          </div>

          <div>
            <label className="mb-2 block text-sm font-semibold">Estimated budget (₹)</label>
            <div className="relative">
              <Wallet size={18} className="absolute left-4 top-1/2 -translate-y-1/2" />
              <input type="number" name="budget" value={form.budget} onChange={updateField}
                min="1" required className="w-full rounded-xl border bg-white py-3.5 pl-11 pr-4" />
            </div>
          </div>

          {error && <p className="rounded-xl bg-red-50 p-3 text-sm text-red-700">{error}</p>}

          <div className="flex justify-end gap-3">
            <button type="button" onClick={onClose} disabled={saving}
              className="rounded-full border px-6 py-3">Cancel</button>
            <button type="submit" disabled={saving}
              className="flex items-center gap-2 rounded-full bg-[#244D3C] px-6 py-3 font-semibold text-white">
              {saving ? <RefreshCw className="animate-spin" size={17} /> : <Save size={17} />}
              {saving ? "Saving..." : "Save changes"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
