import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getDoctors } from '../../api'
import { useAuth } from '../../context/AuthContext'

export default function DoctorsPage() {
    const [doctors, setDoctors] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [search, setSearch] = useState('')
    const navigate = useNavigate()
    const { hasRole } = useAuth()

    useEffect(() => {
        getDoctors()
            .then(setDoctors)
            .catch(err => setError(err?.response?.data?.error || 'Failed to load doctors.'))
            .finally(() => setLoading(false))
    }, [])

    const filtered = doctors.filter(d =>
        d.name.toLowerCase().includes(search.toLowerCase()) ||
        d.specialization.toLowerCase().includes(search.toLowerCase()) ||
        d.email.toLowerCase().includes(search.toLowerCase())
    )

    return (
        <div className="main-content fade-in">
            <div className="page-header">
                <div>
                    <h1 className="page-title">Our Doctors</h1>
                    <p className="page-subtitle">{doctors.length} doctor{doctors.length !== 1 ? 's' : ''} registered</p>
                </div>
                {hasRole('ADMIN', 'RECEPTIONIST') && (
                    <button className="btn btn-primary" onClick={() => navigate('/doctors/new')}>
                        + Add Doctor
                    </button>
                )}
            </div>

            {/* Search */}
            <div className="form-group" style={{ marginBottom: '24px', maxWidth: '400px' }}>
                <input
                    id="doctor-search"
                    type="text"
                    placeholder="Search by name, specialization..."
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                />
            </div>

            {loading && (
                <div className="spinner-wrapper">
                    <div className="spinner" />
                    <span className="spinner-text">Loading doctors...</span>
                </div>
            )}

            {error && <div className="alert alert-error">{error}</div>}

            {!loading && !error && filtered.length === 0 && (
                <div className="empty-state">
                    <div className="empty-title">No doctors found</div>
                    <p className="empty-desc">
                        {search ? `No results for "${search}"` : 'No doctors have been registered yet.'}
                    </p>
                </div>
            )}

            {!loading && (
                <div className="doctors-grid">
                    {filtered.map(doc => (
                        <div
                            key={doc.id}
                            id={`doctor-${doc.id}`}
                            className="doctor-card"
                            onClick={() => navigate(`/doctors/${doc.id}`)}
                        >
                            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                                <div className="doctor-info">
                                    <div className="doctor-name">{doc.name}</div>
                                    <div className="doctor-email">{doc.email}</div>
                                </div>
                            </div>

                            <div className="doctor-meta">
                                <div className="doctor-meta-item">
                                    <span className="doctor-meta-icon">🩺</span>
                                    <span className={`badge badge-blue`}>{doc.specialization}</span>
                                </div>
                                <div className="doctor-meta-item">
                                    <span className="doctor-meta-icon">📞</span>
                                    <span>{doc.phoneNumber}</span>
                                </div>
                                <div className="doctor-meta-item">
                                    <span style={{ fontFamily: 'monospace', fontSize: '0.78rem' }}>{doc.licenseNumber}</span>
                                </div>
                            </div>

                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span className={`badge ${doc.active ? 'badge-green' : 'badge-red'}`}>
                                    {doc.active ? '● Active' : '● Inactive'}
                                </span>
                                <span style={{ fontSize: '0.8rem', color: 'var(--accent-blue)', fontWeight: 600 }}>
                                    View slots →
                                </span>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    )
}
