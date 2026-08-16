import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function ProtectedRoute({ children, roles }) {
    const { isAuthenticated, user, loading } = useAuth()

    if (loading) return <div className="spinner-wrapper"><div className="spinner" /></div>

    if (!isAuthenticated) return <Navigate to="/login" replace />

    if (roles && !roles.includes(user?.role)) {
        return (
            <div className="main-content">
                <div className="empty-state">
                    <div className="empty-icon">🚫</div>
                    <div className="empty-title">Access Denied</div>
                    <p className="empty-desc">You don't have permission to view this page.</p>
                </div>
            </div>
        )
    }

    return children
}
