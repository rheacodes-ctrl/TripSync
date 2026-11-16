import { useRef, useState } from 'react'
import { FileText, Trash2, Upload } from 'lucide-react'
import { api } from '../api/client.js'
import { Button, Card, Empty, SectionHeader, fmtDate } from '../components/ui.jsx'

export default function Documents({ trip, user, isAdmin, run }) {
  const ref = useRef()
  const [busy, setBusy] = useState(false)
  const name = (id) => trip.members.find((m) => m.userId === id)?.name || id
  const onPick = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    setBusy(true)
    // NOTE: only metadata is stored here. With the Spring Boot backend, send a multipart upload instead.
    await run(() => api.addDocument(trip.tripId, { fileName: file.name, uploadedBy: user.userId }))
    setBusy(false); e.target.value = ''
  }
  return (
    <div className="space-y-4">
      <SectionHeader title="Documents" sub="Tickets, bookings and ID copies for the group">
        <Button disabled={busy} onClick={() => ref.current.click()}><Upload size={16} />Upload file</Button>
        <input ref={ref} type="file" className="hidden" onChange={onPick} />
      </SectionHeader>
      <Card>
        {trip.documents.length === 0 ? <Empty icon={FileText} title="No documents yet">Upload tickets and booking confirmations so everyone can find them.</Empty> : (
          <ul className="divide-y divide-line">
            {trip.documents.map((d) => (
              <li key={d.documentId} className="p-4 sm:p-5 flex items-center gap-4">
                <span className="grid place-items-center w-10 h-10 rounded-xl bg-lagoon-50 text-lagoon-600"><FileText size={19} /></span>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{d.fileName}</p>
                  <p className="text-xs text-ink-soft">Uploaded by {name(d.uploadedBy)} on {fmtDate(d.uploadedAt?.slice(0, 10))}</p>
                </div>
                {(isAdmin || d.uploadedBy === user.userId) && <Button variant="quiet" size="sm" aria-label={`Delete ${d.fileName}`} onClick={() => confirm('Delete this document?') && run(() => api.deleteDocument(d.documentId))}><Trash2 size={15} /></Button>}
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
