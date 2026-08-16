import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { createSlot } from '../../api'
import { useToast } from '../../components/Toast'

export default function AddSlotPage() {
    const { id: doctorId } = useParams()
    const toast = useToast()
    const navigate = useNavigate()

    const [form, setForm] = useState({
        date: '',
        startTime: '',
        endTime: '',
    })
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')
    const [added, setAdded] = useState([])

    const handleChange = e => setForm({ ...form, [e.target.name]: e.target.value })

    const handleSubmit = async e => {
        e.preventDefault()
        setError('')
        setLoading(true)
        try {
            const slots = await createSlot(doctorId, form)
            const slot = slots[0]
            toast(`Slot ${slot.startTime?.substring(0, 5)}–${slot.endTime?.substring(0, 5)} added! ✅`, 'success')
            setAdded(prev => [...prev, slot])
            setForm(f => ({ ...f, startTime: '', endTime: '' }))
        } catch (err) {
            const data = err?.response?.data
            const msg = data?.details
                ? Object.values(data.details).join(', ')
                : data?.error || 'Failed to create slot.'
            setError(msg)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="main-content fade-in">
            <div className="page-header">
                <div>
                    <h1 className="page-title">📅 Add Appointment Slot</h1>
                    <p className="page-subtitle">Create a new availability slot for this doctor</p>
                </div>
                <button className="btn btn-secondary" onClick={() => navigate(`/doctors/${doctorId}`)}>← Back to Doctor</button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', maxWidth: '800px' }}>
                {/* Form */}
                <div className="card">
                    <div className="card-header">
                        <div className="card-title">New Slot</div>
                    </div>

                    {error && <div className="alert alert-error">⚠️ {error}</div>}

                    <div className="alert alert-info" style={{ marginBottom: '20px' }}>
                        ℹ️ Add one slot per time range (e.g. 09:00–09:30, then 09:30–10:00).
                    </div>

                    <form onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label htmlFor="slot-date">Date</label>
                            <input
                                id="slot-date"
                                name="date"
                                type="date"
                                value={form.date}
                                onChange={handleChange}
                                min={new Date().toISOString().split('T')[0]}
                                required
                            />
                        </div>

                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                            <div className="form-group">
                                <label htmlFor="slot-start">Start Time</label>
                                <input
                                    id="slot-start"
                                    name="startTime"
                                    type="time"
                                    value={form.startTime}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                            <div className="form-group">
                                <label htmlFor="slot-end">End Time</label>
                                <input
                                    id="slot-end"
                                    name="endTime"
                                    type="time"
                                    value={form.endTime}
                                    onChange={handleChange}
                                    required
                                />
                            </div>
                        </div>

                        <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
                            <button
                                id="add-slot-submit"
                                type="submit"
                                className="btn btn-primary"
                                disabled={loading}
                            >
                                {loading ? '⏳ Adding…' : '➕ Add Slot'}
                            </button>
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => navigate(`/doctors/${doctorId}`)}
                            >
                                Done
                            </button>
                        </div>
                    </form>
                </div>

                {/* Added slots in this session */}
                <div>
                    <div className="section-title">✅ Added This Session</div>
                    {added.length === 0 ? (
                        <div className="empty-state" style={{ padding: '30px' }}>
                            <div className="empty-icon" style={{ fontSize: '2rem' }}>📭</div>
                            <p className="empty-desc">No slots added yet.</p>
                        </div>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                            {added.map(slot => (
                                <div key={slot.id} className="slot-card available">
                                    <div className="slot-time">
                                        {slot.startTime?.substring(0, 5)} – {slot.endTime?.substring(0, 5)}
                                    </div>
                                    <div className="slot-date">📅 {slot.date}</div>
                                    <span className="badge badge-green">AVAILABLE</span>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}
