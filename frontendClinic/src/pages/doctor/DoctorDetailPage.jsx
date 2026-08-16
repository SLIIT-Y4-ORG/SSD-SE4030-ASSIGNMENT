import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getDoctorById, getSlots, releaseSlot, linkDoctorUser, verifyDoctor } from '../../api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../components/Toast'

function formatTime(t) {
    if (!t) return ''
    return t.substring(0, 5)
}

export default function DoctorDetailPage() {
    const { id } = useParams()
    const { user, hasRole } = useAuth()
    const toast = useToast()
    const navigate = useNavigate()

    const [doctor, setDoctor] = useState(null)
    const [slots, setSlots] = useState([])
    const [loadingDoc, setLoadingDoc] = useState(true)
    const [loadingSlots, setLoadingSlots] = useState(false)
    const [dateFilter, setDateFilter] = useState(new Date().toISOString().split('T')[0])
    const [error, setError] = useState('')
    const [actionLoading, setActionLoading] = useState({})
    const [linkUserId, setLinkUserId] = useState('')
    const [linkLoading, setLinkLoading] = useState(false)
    const [showLinkPanel, setShowLinkPanel] = useState(false)

    useEffect(() => {
        getDoctorById(id)
            .then(setDoctor)
            .catch(err => setError(err?.response?.data?.error || 'Doctor not found.'))
            .finally(() => setLoadingDoc(false))
    }, [id])

    const fetchSlots = () => {
        setLoadingSlots(true)
        getSlots(id, dateFilter || undefined)
            .then(setSlots)
            .catch(err => toast(err?.response?.data?.error || 'Failed to load slots.', 'error'))
            .finally(() => setLoadingSlots(false))
    }

    useEffect(() => {
        if (doctor) fetchSlots()
    }, [doctor, dateFilter])

    const handleReserve = async (slotId) => {
        navigate(`/appointments/book?doctorId=${doctor.id}&slotId=${slotId}`)
    }

    const handleRelease = async (slotId) => {
        setActionLoading(p => ({ ...p, [slotId]: true }))
        try {
            await releaseSlot(slotId)
            toast('Slot released.', 'info')
            fetchSlots()
        } catch (err) {
            toast(err?.response?.data?.error || 'Could not release slot.', 'error')
        } finally {
            setActionLoading(p => ({ ...p, [slotId]: false }))
        }
    }

    const handleLinkUser = async (e) => {
        e.preventDefault()
        if (!linkUserId.trim()) return
        setLinkLoading(true)
        try {
            const updated = await linkDoctorUser(id, linkUserId.trim())
            setDoctor(updated)
            setLinkUserId('')
            setShowLinkPanel(false)
            toast(`Linked user account to Dr. ${updated.name}`, 'success')
        } catch (err) {
            toast(err?.response?.data?.error || 'Failed to link user account.', 'error')
        } finally {
            setLinkLoading(false)
        }
    }

    const handleToggleVerify = async () => {
        const updated = await verifyDoctor(doctor.id, !doctor.verified)
        setDoctor(prev => ({ ...prev, verified: updated.verified }))
    }

    if (loadingDoc) return <div className="spinner-wrapper"><div className="spinner" /><span className="spinner-text">Loading doctor...</span></div>
    if (error) return <div className="main-content"><div className="alert alert-error">{error}</div></div>
    if (!doctor) return null

    const available = slots.filter(s => s.status === 'AVAILABLE')
    const reserved = slots.filter(s => s.status === 'RESERVED')

    return (
        <div className="main-content fade-in">
            {/* Doctor Profile Header */}
            <div className="hero-banner" style={{ marginBottom: '24px' }}>
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: '24px', flexWrap: 'wrap' }}>
                    <div className="doctor-avatar" style={{ width: '72px', height: '72px', fontSize: '2rem', flexShrink: 0 }}>
                        {doctor.name.charAt(0)}
                    </div>
                    <div style={{ flex: 1 }}>
                        <h1 className="hero-title" style={{ marginBottom: '4px' }}>{doctor.name}</h1>
                        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginBottom: '12px' }}>
                            <span className="badge badge-blue">{doctor.specialization}</span>
                            <span className={`badge ${doctor.active ? 'badge-green' : 'badge-red'}`}>
                                {doctor.active ? '● Active' : '● Inactive'}
                            </span>
                        </div>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                            <div className="doctor-meta-item"><span>📧</span><span>{doctor.email}</span></div>
                            <div className="doctor-meta-item"><span>📞</span><span>{doctor.phoneNumber}</span></div>
                            <div className="doctor-meta-item"><span>🪪</span><span style={{ fontFamily: 'monospace' }}>{doctor.licenseNumber}</span></div>
                        </div>
                    </div>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        <button className="btn btn-secondary" onClick={() => navigate('/doctors')}>← Back</button>
                        {(hasRole('ADMIN', 'RECEPTIONIST') || (hasRole('DOCTOR') && doctor.userId === user?.id)) && (
                            <button className="btn btn-primary" onClick={() => navigate(`/doctors/${id}/slots/new`)}>
                                + Add Slot
                            </button>
                        )}
                    </div>
                </div>
            </div>

            {/* Link User Account — ADMIN / RECEPTIONIST only */}
            {hasRole('ADMIN', 'RECEPTIONIST') && (
                <div className="card" style={{ marginBottom: '24px', maxWidth: '560px' }}>
                    <div
                        style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer' }}
                        onClick={() => setShowLinkPanel(p => !p)}
                    >
                        <div className="card-title" style={{ margin: 0 }}>Link User Account</div>
                        <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                            {doctor.userId
                                ? <span className="badge badge-green">Linked</span>
                                : <span className="badge badge-red">Not linked</span>}
                            &nbsp; {showLinkPanel ? '▲' : '▼'}
                        </span>
                    </div>

                    {showLinkPanel && (
                        <div style={{ marginTop: '16px' }}>
                            {doctor.userId && (
                                <div className="alert alert-info" style={{ marginBottom: '12px' }}>
                                    Current linked user ID: <code style={{ fontSize: '0.8rem' }}>{doctor.userId}</code>
                                </div>
                            )}
                            {!doctor.userId && (
                                <div className="alert alert-error" style={{ marginBottom: '12px' }}>
                                    No user account linked. The doctor cannot access their schedule until linked.
                                </div>
                            )}
                            <form onSubmit={handleLinkUser} style={{ display: 'flex', gap: '10px', alignItems: 'flex-end' }}>
                                <div className="form-group" style={{ flex: 1, marginBottom: 0 }}>
                                    <label htmlFor="link-user-id">Doctor's User Account UUID</label>
                                    <input
                                        id="link-user-id"
                                        type="text"
                                        placeholder="e.g. e9d8b4a4-5a87-439f-899b-23deef7c5250"
                                        value={linkUserId}
                                        onChange={e => setLinkUserId(e.target.value)}
                                        required
                                    />
                                </div>
                                <button
                                    type="submit"
                                    className="btn btn-primary"
                                    disabled={linkLoading}
                                    style={{ whiteSpace: 'nowrap' }}
                                >
                                    {linkLoading ? '⏳ Saving…' : '🔗 Link'}
                                </button>
                            </form>
                        </div>
                    )}
                </div>
            )}

            {/* Verification Status — badge for all, toggle for ADMIN/RECEPTIONIST */}
            {hasRole('ADMIN', 'RECEPTIONIST') ? (
                <div className="card" style={{ marginBottom: '24px', maxWidth: '560px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
                        <div>
                            <div className="card-title" style={{ margin: 0 }}>Verification Status</div>
                            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                                {doctor.verified
                                    ? 'This doctor is verified and visible for patient bookings.'
                                    : 'Not yet verified. Doctor will not appear in appointment booking until approved.'}
                            </div>
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexShrink: 0 }}>
                            <span className={`badge ${doctor.verified ? 'badge-green' : 'badge-red'}`}>
                                {doctor.verified ? 'Verified' : 'Pending'}
                            </span>
                            <button
                                className={`btn btn-sm ${doctor.verified ? 'btn-danger' : 'btn-primary'}`}
                                onClick={handleToggleVerify}
                            >
                                {doctor.verified ? 'Revoke' : 'Verify'}
                            </button>
                        </div>
                    </div>
                </div>
            ) : (
                !doctor.verified && (
                    <div className="alert alert-error" style={{ marginBottom: '24px', maxWidth: '560px' }}>
                        ⏳ This doctor profile is <strong>pending verification</strong> and is not yet available for bookings.
                    </div>
                )
            )}

            {/* Slot Filters */}
            <div className="section">
                <div className="section-title">Appointment Slots</div>
                <div style={{ display: 'flex', gap: '12px', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <label style={{ marginBottom: 0, whiteSpace: 'nowrap' }}>Filter by date:</label>
                        <input
                            id="slot-date-filter"
                            type="date"
                            value={dateFilter}
                            onChange={e => setDateFilter(e.target.value)}
                            style={{ width: 'auto' }}
                        />
                    </div>
                    {dateFilter && (
                        <button className="btn btn-secondary btn-sm" onClick={() => setDateFilter('')}>Clear filter</button>
                    )}
                    <div style={{ marginLeft: 'auto', display: 'flex', gap: '12px' }}>
                        <span className="badge badge-green">{available.length} Available</span>
                        <span className="badge badge-red">{reserved.length} Reserved</span>
                    </div>
                </div>

                {loadingSlots && <div className="spinner-wrapper" style={{ padding: '30px' }}><div className="spinner" /></div>}

                {!loadingSlots && slots.length === 0 && (
                    <div className="empty-state">
                        <div className="empty-icon">📭</div>
                        <div className="empty-title">No slots found</div>
                        <p className="empty-desc">
                            {dateFilter ? `No slots for ${dateFilter}.` : 'This doctor has no scheduled slots yet.'}
                        </p>
                        {(hasRole('ADMIN', 'RECEPTIONIST') || (hasRole('DOCTOR') && doctor.userId === user?.id)) && (
                            <button className="btn btn-primary" onClick={() => navigate(`/doctors/${id}/slots/new`)}>
                                + Add Slot
                            </button>
                        )}
                    </div>
                )}

                {!loadingSlots && slots.length > 0 && (
                    <div className="slots-grid">
                        {slots.map(slot => (
                            <div
                                key={slot.id}
                                id={`slot-${slot.id}`}
                                className={`slot-card ${slot.status.toLowerCase()}`}
                            >
                                <div className="slot-time">
                                    {formatTime(slot.startTime)} – {formatTime(slot.endTime)}
                                </div>
                                <div className="slot-date">{slot.date}</div>
                                <div>
                                    <span className={`badge ${slot.status === 'AVAILABLE' ? 'badge-green' : 'badge-red'}`}>
                                        {slot.status}
                                    </span>
                                </div>
                                {slot.status === 'AVAILABLE' && hasRole('PATIENT') && (
                                    <button
                                        className="btn btn-success btn-sm"
                                        disabled={actionLoading[slot.id]}
                                        onClick={() => handleReserve(slot.id)}
                                    >
                                        {actionLoading[slot.id] ? '...' : 'Reserve'}
                                    </button>
                                )}
                                {slot.status === 'RESERVED' && hasRole('ADMIN', 'RECEPTIONIST', 'DOCTOR') && (
                                    <button
                                        className="btn btn-danger btn-sm"
                                        disabled={actionLoading[slot.id]}
                                        onClick={() => handleRelease(slot.id)}
                                    >
                                        {actionLoading[slot.id] ? '...' : 'Release'}
                                    </button>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    )
}
