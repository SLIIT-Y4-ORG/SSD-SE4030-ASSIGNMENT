import { useEffect, useMemo, useState } from 'react'
import { getPatients } from '../../api'

export default function PatientsPage() {
    const [patients, setPatients] = useState([])
    const [search, setSearch] = useState('')
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {
        getPatients()
            .then(data => setPatients(Array.isArray(data) ? data : []))
            .catch(err => setError(err?.response?.data?.error || 'Failed to load patients.'))
            .finally(() => setLoading(false))
    }, [])

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase()
        if (!query) return patients
        return patients.filter(p =>
            (p.name || '').toLowerCase().includes(query)
            || (p.email || '').toLowerCase().includes(query)
            || (p.phone || '').toLowerCase().includes(query)
            || (p.bloodGroup || '').toLowerCase().includes(query)
        )
    }, [patients, search])

    return (
        <div className="main-content fade-in">
            <div className="page-header">
                <div>
                    <h1 className="page-title">Patients</h1>
                    <p className="page-subtitle">
                        {patients.length} patient{patients.length === 1 ? '' : 's'} registered
                    </p>
                </div>
            </div>

            <div className="form-group" style={{ marginBottom: '24px', maxWidth: '420px' }}>
                <input
                    type="text"
                    value={search}
                    onChange={e => setSearch(e.target.value)}
                    placeholder="Search by name, email, phone or blood group"
                />
            </div>

            {loading && (
                <div className="spinner-wrapper">
                    <div className="spinner" />
                    <span className="spinner-text">Loading patients...</span>
                </div>
            )}

            {error && <div className="alert alert-error">{error}</div>}

            {!loading && !error && filtered.length === 0 && (
                <div className="empty-state">
                    <div className="empty-title">No patients found</div>
                    <p className="empty-desc">Try a different search term.</p>
                </div>
            )}

            {!loading && !error && filtered.length > 0 && (
                <div className="doctors-grid">
                    {filtered.map(patient => (
                        <div key={patient.id} className="doctor-card" style={{ cursor: 'default' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                                <div className="doctor-avatar">{(patient.name || '?').charAt(0).toUpperCase()}</div>
                                <div>
                                    <div className="doctor-name">{patient.name || 'Unknown'}</div>
                                    <div className="doctor-email">{patient.email || 'No email'}</div>
                                </div>
                            </div>

                            <div className="doctor-meta">
                                <div className="doctor-meta-item">
                                    <span className="doctor-meta-icon">📞</span>
                                    <span>{patient.phone || '—'}</span>
                                </div>
                                <div className="doctor-meta-item">
                                    <span className="doctor-meta-icon">🩸</span>
                                    <span>{patient.bloodGroup || '—'}</span>
                                </div>
                                <div className="doctor-meta-item">
                                    <span className="doctor-meta-icon">⚧</span>
                                    <span>{patient.gender || '—'}</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    )
}