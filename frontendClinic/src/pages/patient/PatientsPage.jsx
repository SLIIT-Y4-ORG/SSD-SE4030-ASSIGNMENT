import { useEffect, useMemo, useState } from 'react'
import { getPatients, getPatientUsers } from '../../api'
import { useAuth } from '../../context/AuthContext'

export default function PatientsPage() {
    const { user } = useAuth()
    const [patients, setPatients] = useState([])
    const [search, setSearch] = useState('')
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {
        const loadPatients = async () => {
            const profiles = await getPatients()
            const patientProfiles = Array.isArray(profiles) ? profiles : []

            // Patient accounts and patient medical profiles live in separate
            // services. Staff should still see a newly registered account
            // before its owner completes the optional medical profile.
            const users = await getPatientUsers()
            const patientUsers = (Array.isArray(users) ? users : [])
                .filter(account => account.role === 'PATIENT')

            const profilesByUserId = new Map(
                patientProfiles
                    .filter(profile => profile.userId)
                    .map(profile => [profile.userId, profile])
            )
            const profilesByEmail = new Map(
                patientProfiles
                    .filter(profile => profile.email)
                    .map(profile => [profile.email.toLowerCase(), profile])
            )

            const merged = patientUsers.map(account => {
                const profile = profilesByUserId.get(account.id)
                    || profilesByEmail.get((account.email || '').toLowerCase())

                return profile
                    ? { ...profile, profileComplete: true }
                    : {
                        id: `account-${account.id}`,
                        userId: account.id,
                        name: account.name,
                        email: account.email,
                        phone: account.phone,
                        profileComplete: false,
                    }
            })

            // Preserve orphaned/legacy profiles that have no matching user.
            const matchedProfileIds = new Set(merged.filter(item => item.profileComplete).map(item => item.id))
            return [...merged, ...patientProfiles.filter(profile => !matchedProfileIds.has(profile.id))]
        }

        loadPatients()
            .then(setPatients)
            .catch(err => setError(err?.response?.data?.error || 'Failed to load patients.'))
            .finally(() => setLoading(false))
    }, [user?.role])

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
                                    {patient.profileComplete === false && (
                                        <div className="badge badge-warning" style={{ marginTop: '8px' }}>
                                            Medical profile incomplete
                                        </div>
                                    )}
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