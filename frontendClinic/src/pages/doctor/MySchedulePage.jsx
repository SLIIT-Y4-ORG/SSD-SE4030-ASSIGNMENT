import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { getDoctorByUserId, createDoctor } from '../../api'

const SPECIALIZATIONS = [
    'General Practice', 'Cardiology', 'Neurology', 'Pediatrics',
    'Orthopedics', 'Dermatology', 'Ophthalmology', 'Psychiatry',
    'Gynecology', 'Endocrinology', 'Gastroenterology', 'Oncology',
    'Radiology', 'Urology', 'ENT',
]

export default function MySchedulePage() {
    const { user } = useAuth()
    const navigate = useNavigate()

    const [status, setStatus] = useState('loading')
    const [errorMsg, setErrorMsg] = useState('')
    const [applying, setApplying] = useState(false)
    const [applyError, setApplyError] = useState('')
    const [form, setForm] = useState({
        name: '',
        email: '',
        phone: '',
        specialization: '',
        licenseNumber: '',
    })

    useEffect(() => {
        if (!user?.id) return

        // Pre-fill form from user context
        setForm(f => ({
            ...f,
            name: user.name || '',
            email: user.email || '',
            phone: user.phone || '',
        }))

        getDoctorByUserId(user.id)
            .then(doctor => {
                if (!doctor) {
                    setStatus('apply')
                } else if (!doctor.verified) {
                    setStatus('pending')
                } else {
                    // Redirect verified doctor to their public profile / slot management
                    navigate(`/doctors/${doctor.id}`, { replace: true })
                }
            })
            .catch(err => {
                setErrorMsg(err?.response?.data?.error || 'Failed to check your doctor profile.')
                setStatus('error')
            })
    }, [user, navigate])

    const handleApply = async e => {
        e.preventDefault()
        setApplyError('')
        setApplying(true)
        try {
            await createDoctor({
                name: form.name,
                email: form.email,
                phone: form.phone,
                specialization: form.specialization || null,
                licenseNumber: form.licenseNumber,
                userId: user.id,
                verified: false,
            })
            setStatus('pending')
        } catch (err) {
            const data = err?.response?.data
            const msg = data?.details
                ? Object.values(data.details).join(', ')
                : data?.error || 'Failed to submit application.'
            setApplyError(msg)
        } finally {
            setApplying(false)
        }
    }

    if (status === 'loading') {
        return (
            <div className="spinner-wrapper">
                <div className="spinner" />
                <span className="spinner-text">Checking your doctor profile…</span>
            </div>
        )
    }

    if (status === 'error') {
        return (
            <div className="main-content fade-in">
                <div className="alert alert-error">⚠️ {errorMsg}</div>
                <button className="btn btn-secondary" onClick={() => navigate('/dashboard')}>← Dashboard</button>
            </div>
        )
    }

    if (status === 'pending') {
        return (
            <div className="main-content fade-in">
                <div className="hero-banner">
                    <div className="hero-greeting">⏳ Application submitted</div>
                    <h1 className="hero-title">Pending Verification</h1>
                    <p className="hero-subtitle">
                        Your doctor profile has been received and is awaiting review by a Receptionist or Admin.
                        You will have full access to your schedule once verified.
                    </p>
                </div>
                <div className="card" style={{ maxWidth: '520px' }}>
                    <div className="card-header"><div className="card-title">ℹ️ What happens next?</div></div>
                    <ol style={{ paddingLeft: '20px', lineHeight: 2, color: 'var(--text-secondary)', margin: 0 }}>
                        <li>A Receptionist reviews your application.</li>
                        <li>They confirm your specialization and license details.</li>
                        <li>Once approved, you can manage slots and appear for bookings.</li>
                    </ol>
                    <div style={{ marginTop: '16px', padding: '12px', background: 'var(--bg-secondary)', borderRadius: '8px', fontFamily: 'monospace', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        Your User ID: {user?.id}
                    </div>
                    <button className="btn btn-secondary" style={{ marginTop: '16px' }} onClick={() => navigate('/dashboard')}>← Back to Dashboard</button>
                </div>
            </div>
        )
    }

    // status === 'apply'
    return (
        <div className="main-content fade-in">
            <div className="hero-banner">
                <div className="hero-greeting">⚕️ Welcome, Dr. {user?.name?.split(' ')[0]}</div>
                <h1 className="hero-title">Apply as a Doctor</h1>
                <p className="hero-subtitle">
                    Submit your details to be reviewed by our staff. You'll be able to manage
                    your availability slots once a Receptionist verifies your profile.
                </p>
            </div>

            <div className="card" style={{ maxWidth: '520px' }}>
                <div className="card-header"><div className="card-title">📋 Doctor Application</div></div>

                {applyError && <div className="alert alert-error">⚠️ {applyError}</div>}

                <form onSubmit={handleApply}>
                    <div className="form-group">
                        <label>Full Name</label>
                        <input
                            type="text"
                            value={form.name}
                            onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Email Address</label>
                        <input
                            type="email"
                            value={form.email}
                            readOnly
                            style={{ background: 'var(--bg-secondary)', cursor: 'not-allowed' }}
                        />
                    </div>
                    <div className="form-group">
                        <label>Phone Number</label>
                        <input
                            type="tel"
                            placeholder="+94771234567"
                            value={form.phone}
                            onChange={e => setForm(f => ({ ...f, phone: e.target.value }))}
                        />
                    </div>
                    <div className="form-group">
                        <label>License Number</label>
                        <input
                            type="text"
                            placeholder="e.g. SLMC-2024-001"
                            value={form.licenseNumber}
                            onChange={e => setForm(f => ({ ...f, licenseNumber: e.target.value }))}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Specialization <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}>(optional — receptionist will confirm)</span></label>
                        <select
                            value={form.specialization}
                            onChange={e => setForm(f => ({ ...f, specialization: e.target.value }))}
                        >
                            <option value="">Select if known…</option>
                            {SPECIALIZATIONS.map(s => <option key={s} value={s}>{s}</option>)}
                        </select>
                    </div>
                    <div className="alert alert-info" style={{ marginBottom: '16px' }}>
                        ℹ️ Your department and years of experience will be confirmed by the Receptionist during verification.
                    </div>
                    <button type="submit" className="btn btn-primary" disabled={applying}>
                        {applying ? '⏳ Submitting…' : '📨 Submit Application'}
                    </button>
                </form>
            </div>
        </div>
    )
}
