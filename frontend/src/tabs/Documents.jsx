import { useRef, useState } from 'react'
import { api } from '../api/client.js'
import { Button, Card, Empty, fmtDate } from '../components/ui.jsx'

export default function Documents({ trip, user, run }) {
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
      <div className="flex justify-between items-center">
        <h2 className="font-semibold text-lg">Documents</h2>
        <Button disabled={busy} onClick={() => ref.current.click()}>⬆ Upload</Button>
        <input ref={ref} type="file" className="hidden" onChange={onPick} />
      </div>
      <Card>
        {trip.documents.length === 0 ? <Empty>No documents uploaded.</Empty> : (
          <ul className="divide-y divide-slate-100">
            {trip.documents.map((d) => (
              <li key={d.documentId} className="p-4 flex items-center gap-3">
                <span className="text-2xl">📄</span>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{d.fileName}</p>
                  <p className="text-xs text-slate-500">Uploaded by {name(d.uploadedBy)} · {fmtDate(d.uploadedAt?.slice(0, 10))}</p>
                </div>
                <button className="text-xs text-red-600 hover:underline" onClick={() => confirm('Delete document?') && run(() => api.deleteDocument(d.documentId))}>Delete</button>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </div>
  )
}
