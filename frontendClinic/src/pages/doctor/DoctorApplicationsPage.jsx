import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getDoctors, updateDoctor } from '../../api'
import { useToast } from '../../components/Toast'

const SPECIALIZATIONS = [
    'General Practice', 'Cardiology', 'Neurology', 'Pediatrics',
    'Orthopedics', 'Dermatology', 'Ophthalmology', 'Psychiatry',
    'Gynecology', 'Endocrinology', 'Gastroenterology', 'Oncology',
    'Radiology', 'Urology', 'ENT',
]

function initForm(doc) {
    return {
        name: doc.name || '',
        email: doc.email || '',
        phone: doc.phone || '',
        specialization: doc.specialization || '',
        licenseNumber: doc.licenseNumber || '',
        department: doc.department || '',
        yearsOfExperience: doc.yearsOfExperience || 0,
        userId: doc.userId || '',
    }
}

export default function DoctorApplicationsPage() {
    const navigate = useNavigate()
    const toast = useToast()

    const [applications, setApplications] = useState([])
    const [loading, setLoading] = useState(true)
    const [expandedId, setExpandedId] = useState(null)
    const [forms, setForms] = useState({})
    const [saving, setSaving] = useState({})

    useEffect(() => {
        getDoctors()
            .then(all => setApplications(all.filter(d => !d.verified)))
            .catch(() => toast('Failed to load applications.', 'error'))
            .finally(() => setLoading(false))
    }, [])

    const handleExpand = doc => {
        if (expandedId === doc.id) { setExpandedId(null); return }
        setExpandedId(doc.id)
        setForms(prev => ({ ...prev, [doc.id]: prev[doc.id] ?? initForm(doc) }))
    }

    const setField = (docId, field, value) =>
        setForms(prev => ({ ...prev, [docId]: { ...prev[docId], [field]: value } }))

    const handleVerify = async docId => {
        const form = forms[docId]
        if (!form.specialization || !form.licenseNumber) {
            toast('Specialization and License Number are required to verify.', 'error')
            return
        }
        setSaving(prev => ({ ...prev, [docId]: true }))
        try {
            await updateDoctor(docId, { ...form, verified: true })
            toast(`Dr. ${form.name} verified successfully ✅`, 'success')
            setApplications(prev => prev.filter(d => d.id !== docId))
            setExpandedId(null)
        } catch (err) {
            const data = err?.response?.data
            const msg = data?.details
                ? Object.values(data.details).join(', ')
                : data?.error || 'Verification failed.'
            toast(msg, 'error')
        } finally {
            setSaving(prev => ({ ...prev, [docId]: false }))
        }
    }

    if (loading) {
        return <div className="spinner-wrapper"><div className="spinner" /><span className="spinner-text">Loading applications…</span></div>
    }

    return (
        <div className="main-content fade-in">
            <div className="page-header">
                <div>
                    <h1 className="page-title">📋 Doctor Applications</h1>
                    <p className="page-subtitle">Review and verify pending doctor registrations</p>
                </div>
                <button className="btn btn-secondary" onClick={() => navigate('/dashboard')}>← Dashboard</button>
            </div>

            {applications.length === 0 && (
                <div className="empty-state">
                    <div className="empty-icon">✅</div>
                    <div className="empty-title">No Pending Applications</div>
                    <p className="empty-desc">All doctor registrations have been verified.</p>
                </div>
            )}

            <div className="doctors-grid">
                {applications.map(doc => (
                    <div key={doc.id} className="doctor-card" style={{ display: 'block', cursor: 'default' }}>

                        {/* Card Header */}
                        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                            <div className="doctor-avatar">{doc.name.charAt(0)}</div>
                            <div style={{ flex: 1 }}>
                                <div className="doctor-name">{doc.name}</div>
                                <div className="doctor-email">{doc.email}</div>
                                {doc.phone && <div className="doctor-email">📞 {doc.phone}</div>}
                            </div>
                            <span className="badge badge-red">⏳ Pending</span>
                        </div>

                        <div style={{ marginTop: '12px', display: 'flex', gap: '8px' }}>
                            <button className="btn btn-primary btn-sm" onClick={() => handleExpand(doc)}>
                                {expandedId === doc.id ? '▲ Close' : '✅ Review & Verify'}
                            </button>
                            <button className="btn btn-secondary btn-sm" onClick={() => navigate(`/doctors/${doc.id}`)}>
                                View Profile
                            </button>
                        </div>

                        {/* Inline Verify Form */}
                        {expandedId === doc.id && forms[doc.id] && (
                            <div style={{ marginTop: '16px', borderTop: '1px solid var(--border)', paddingTop: '16px' }}>
                                <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: '12px' }}>
                                    Confirm or update the doctor's professional details, then click Verify.
                                </p>

                                <div className="form-group">
                                    <label>Full Name</label>
                                    <input type="text" value={forms[doc.id].name}
                                        onChange={e => setField(doc.id, 'name', e.target.value)} />
                                </div>

                                <div className="form-group">
                                    <label>Specialization <span style={{ color: 'red' }}>*</span></label>
                                    <select value={forms[doc.id].specialization}
                                        onChange={e => setField(doc.id, 'specialization', e.target.value)}>
                                        <option value="">Select specialization…</option>
                                        {SPECIALIZATIONS.map(s => <option key={s} value={s}>{s}</option>)}
                                    </select>
                                </div>

                                <div className="form-group">
                                    <label>License Number <span style={{ color: 'red' }}>*</span></label>
                                    <input type="text" placeholder="e.g. SLMC-2024-001"
                                        value={forms[doc.id].licenseNumber}
                                        onChange={e => setField(doc.id, 'licenseNumber', e.target.value)} />
                                </div>

                                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                                    <div className="form-group">
                                        <label>Department</label>
                                        <input type="text" placeholder="e.g. Cardiology Unit"
                                            value={forms[doc.id].department}
                                            onChange={e => setField(doc.id, 'department', e.target.value)} />
                                    </div>
                                    <div className="form-group">
                                        <label>Years of Experience</label>
                                        <input type="number" min={0}
                                            value={forms[doc.id].yearsOfExperience}
                                            onChange={e => setField(doc.id, 'yearsOfExperience', Number(e.target.value))} />
                                    </div>
                                </div>

                                <div style={{ display: 'flex', gap: '8px' }}>
                                    <button
                                        className="btn btn-primary"
                                        onClick={() => handleVerify(doc.id)}
                                        disabled={saving[doc.id]}
                                    >
                                        {saving[doc.id] ? '⏳ Verifying…' : '✅ Verify & Approve'}
                                    </button>
                                    <button className="btn btn-secondary" onClick={() => setExpandedId(null)}>
                                        Cancel
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                ))}
            </div>
        </div>
    )
}
