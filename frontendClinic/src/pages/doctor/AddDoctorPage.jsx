import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createDoctor } from '../../api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../components/Toast'

export default function AddDoctorPage() {
    const { user } = useAuth()
    const toast = useToast()
    const navigate = useNavigate()

    const [form, setForm] = useState({
        name: '',
        email: '',
        phoneNumber: '',
        specialization: '',
        licenseNumber: '',
        userId: '',   // intentionally blank — must be the *doctor's* user UUID, not the admin's
    })
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    const handleChange = e => setForm({ ...form, [e.target.name]: e.target.value })

    const handleSubmit = async e => {
        e.preventDefault()
        setError('')
        setLoading(true)
        try {
            const doc = await createDoctor(form)
            toast(`Dr. ${doc.name} added successfully! 🎉`, 'success')
            navigate(`/doctors/${doc.id}`)
        } catch (err) {
            const data = err?.response?.data
            const msg = data?.details
                ? Object.values(data.details).join(', ')
                : data?.error || 'Failed to create doctor.'
            setError(msg)
        } finally {
            setLoading(false)
        }
    }

    const SPECIALIZATIONS = [
        'General Practice', 'Cardiology', 'Neurology', 'Pediatrics',
        'Orthopedics', 'Dermatology', 'Ophthalmology', 'Psychiatry',
        'Gynecology', 'Endocrinology', 'Gastroenterology', 'Oncology',
        'Radiology', 'Urology', 'ENT',
    ]

    return (
        <div className="main-content fade-in">
            <div className="page-header">
                <div>
                    <h1 className="page-title">➕ Add New Doctor</h1>
                    <p className="page-subtitle">Register a new doctor to the clinic</p>
                </div>
                <button className="btn btn-secondary" onClick={() => navigate('/doctors')}>← Back</button>
            </div>

            <div style={{ maxWidth: '560px' }}>
                <div className="card">
                    {error && <div className="alert alert-error">⚠️ {error}</div>}

                    <form onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label htmlFor="add-doctor-name">Full Name</label>
                            <input
                                id="add-doctor-name"
                                name="name"
                                type="text"
                                placeholder="Dr. Kamal Silva"
                                value={form.name}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="add-doctor-email">Email Address</label>
                            <input
                                id="add-doctor-email"
                                name="email"
                                type="email"
                                placeholder="kamal.silva@clinic.com"
                                value={form.email}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="add-doctor-phone">Phone Number</label>
                            <input
                                id="add-doctor-phone"
                                name="phoneNumber"
                                type="tel"
                                placeholder="+94771234567"
                                value={form.phoneNumber}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="add-doctor-spec">Specialization</label>
                            <select
                                id="add-doctor-spec"
                                name="specialization"
                                value={form.specialization}
                                onChange={handleChange}
                                required
                            >
                                <option value="">Select specialization…</option>
                                {SPECIALIZATIONS.map(s => <option key={s} value={s}>{s}</option>)}
                            </select>
                        </div>

                        <div className="form-group">
                            <label htmlFor="add-doctor-license">License Number</label>
                            <input
                                id="add-doctor-license"
                                name="licenseNumber"
                                type="text"
                                placeholder="SLMC-2024-001"
                                value={form.licenseNumber}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="add-doctor-userid">Doctor's User Account ID <span style={{ fontWeight: 400, color: 'var(--text-muted)' }}>(optional)</span></label>
                            <input
                                id="add-doctor-userid"
                                name="userId"
                                type="text"
                                placeholder="e.g. e9d8b4a4-5a87-439f-899b-23deef7c5250"
                                value={form.userId}
                                onChange={handleChange}
                            />
                            <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '6px' }}>
                                Enter the UUID of the doctor's registered user account (role = DOCTOR) from the User Service.
                                Leave blank to link later.
                            </div>
                        </div>

                        <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
                            <button
                                id="add-doctor-submit"
                                type="submit"
                                className="btn btn-primary"
                                disabled={loading}
                            >
                                {loading ? '⏳ Adding…' : '➕ Add Doctor'}
                            </button>
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => navigate('/doctors')}
                            >
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    )
}
