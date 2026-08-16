import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function roleBadgeClass(role) {
    const map = { ADMIN: 'badge-purple', DOCTOR: 'badge-blue', PATIENT: 'badge-green', RECEPTIONIST: 'badge-cyan' }
    return map[role] || 'badge-gray'
}

export default function Navbar() {
    const { user, logout, isAuthenticated } = useAuth()
    const navigate = useNavigate()

    const handleLogout = async () => {
        await logout()
        navigate('/login')
    }

    const initials = user?.name
        ? user.name.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase()
        : '?'

    return (
        <nav className="navbar">
            <div className="navbar-inner">
                <NavLink to="/" className="navbar-brand">
                    <span className="brand-icon">🏥</span>
                    ClinicMate
                </NavLink>

                {isAuthenticated && (
                    <div className="navbar-links">
                        <NavLink to="/dashboard" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                            Dashboard
                        </NavLink>
                        <NavLink to="/doctors" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                            Doctors
                        </NavLink>
                        {user?.role === 'PATIENT' && (
                            <NavLink to="/payments" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                                Payments
                            </NavLink>
                        )}
                        {(user?.role === 'ADMIN' || user?.role === 'RECEPTIONIST') && (
                            <NavLink to="/doctors/new" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
                                + Add Doctor
                            </NavLink>
                        )}
                    </div>
                )}

                <div className="navbar-user">
                    {isAuthenticated ? (
                        <>
                            <div className="user-badge">
                                <div className="user-avatar">{initials}</div>
                                <span>{user?.name?.split(' ')[0]}</span>
                                <span className={`badge ${roleBadgeClass(user?.role)}`} style={{ padding: '2px 8px', fontSize: '0.7rem' }}>
                                    {user?.role}
                                </span>
                            </div>
                            <button className="btn btn-secondary btn-sm" onClick={handleLogout}>
                                Logout
                            </button>
                        </>
                    ) : (
                        <>
                            <NavLink to="/login" className="btn btn-secondary btn-sm">Login</NavLink>
                            <NavLink to="/register" className="btn btn-primary btn-sm">Register</NavLink>
                        </>
                    )}
                </div>
            </div>
        </nav>
    )
}
