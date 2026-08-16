import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { getDoctors } from '../../api'

const ROLE_CONFIG = {
    PATIENT: {
        color: 'badge-green',
        desc: 'View doctors, browse available slots, and book appointments.',
        actions: [
            { label: 'My Patient Profile', to: '/patients/me' },
            { label: 'Browse Doctors', to: '/doctors' },
            { label: 'My Payments', to: '/payments' },
        ],
    },
    DOCTOR: {
        color: 'badge-blue',
        desc: 'Manage your schedule and availability slots.',
        actions: [
            { label: '📅 My Schedule', to: '/my-schedule' },
            { label: '👨‍⚕️ All Doctors', to: '/doctors' },
        ],
    },
    RECEPTIONIST: {
        color: 'badge-cyan',
        desc: 'Manage doctor registrations and appointment slots.',
        actions: [
            { label: '📋 Doctor Applications', to: '/doctor-applications' },
            { label: '👨‍⚕️ All Doctors', to: '/doctors' },
            { label: '➕ Add Doctor', to: '/doctors/new' },
            { label: '🧾 Patients', to: '/patients' },
        ],
    },
    ADMIN: {
        color: 'badge-purple',
        desc: 'Full access to manage the entire clinic system.',
        actions: [
            { label: '📋 Doctor Applications', to: '/doctor-applications' },
            { label: '👨‍⚕️ All Doctors', to: '/doctors' },
            { label: '➕ Add Doctor', to: '/doctors/new' },
            { label: '🧾 Patients', to: '/patients' },
        ],
    },
}

export default function DashboardPage() {
    const { user } = useAuth()
    const navigate = useNavigate()
    const [doctors, setDoctors] = useState([])
    const [loadingDoctors, setLoadingDoctors] = useState(true)
    const config = ROLE_CONFIG[user?.role] || ROLE_CONFIG.PATIENT

    useEffect(() => {
        getDoctors()
            .then(setDoctors)
            .catch(() => { })
            .finally(() => setLoadingDoctors(false))
    }, [])

    return (
        <div className="main-content fade-in">
            {/* Hero Banner */}
            <div className="hero-banner">
                <div className="hero-greeting">Good day</div>
                <h1 className="hero-title">Hello, {user?.name?.split(' ')[0]}!</h1>
                <p className="hero-subtitle">
                    {config.desc}
                </p>
            </div>

            {/* Stats */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div>
                        <div className="stat-value">{loadingDoctors ? '—' : doctors.length}</div>
                        <div className="stat-label">Doctors Available</div>
                    </div>
                </div>
                <div className="stat-card">
                    <div>
                        <div className="stat-value">{loadingDoctors ? '—' : doctors.filter(d => d.active).length}</div>
                        <div className="stat-label">Active Doctors</div>
                    </div>
                </div>
                <div className="stat-card">
                    <div>
                        <div className="stat-value">{loadingDoctors ? '—' : new Set(doctors.map(d => d.specialization)).size}</div>
                        <div className="stat-label">Specializations</div>
                    </div>
                </div>
                <div className="stat-card">
                    <div>
                        <div className="stat-value">{user?.role}</div>
                        <div className="stat-label">Your Role</div>
                    </div>
                </div>
            </div>

            {/* Quick Actions */}
            <div className="section">
                <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                    {config.actions.map(action => (
                        <Link key={action.to} to={action.to} className="btn btn-primary">
                            {action.label}
                        </Link>
                    ))}
                    <Link to="/doctors" className="btn btn-secondary">
                        View Slots
                    </Link>
                </div>
            </div>

            {/* My Profile */}
            <div className="section">
                <div className="section-title">👤 My Profile</div>
                <div className="card" style={{ maxWidth: '500px' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                        {[
                            { label: 'Full Name', value: user?.name },
                            { label: 'Email', value: user?.email },
                            { label: 'Phone', value: user?.phone },
                            { label: 'Role', value: user?.role },
                            { label: 'Account ID', value: user?.id?.substring(0, 12) + '...' },
                        ].map(item => (
                            <div key={item.label} style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                                <span style={{ fontSize: '1.1rem', width: '24px' }}>{item.icon}</span>
                                <div>
                                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>{item.label}</div>
                                    <div style={{ fontSize: '0.9rem', color: 'var(--text-primary)', fontFamily: item.label === 'Account ID' ? 'monospace' : undefined }}>
                                        {item.value || '—'}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* Recent Doctors Quick Preview */}
            {!loadingDoctors && doctors.length > 0 && (
                <div className="section">
                    <div className="section-title">
                        🔭 Doctors at a Glance
                        <Link to="/doctors" style={{ marginLeft: 'auto', fontSize: '0.8rem', color: 'var(--accent-blue)', fontWeight: 500 }}>View all →</Link>
                    </div>
                    <div className="doctors-grid">
                        {doctors.slice(0, 3).map(doc => (
                            <div
                                key={doc.id}
                                className="doctor-card"
                                onClick={() => navigate(`/doctors/${doc.id}`)}
                            >
                                <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                                    <div className="doctor-avatar">{doc.name.charAt(0)}</div>
                                    <div>
                                        <div className="doctor-name">{doc.name}</div>
                                        <div className="doctor-email">{doc.specialization}</div>
                                    </div>
                                </div>
                                <span className={`badge badge-blue`}>{doc.specialization}</span>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </div>
    )
}
