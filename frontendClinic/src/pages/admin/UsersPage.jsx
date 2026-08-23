import { useEffect, useMemo, useState } from 'react'
import { getUsers, updateUserRole } from '../../api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../components/Toast'

const ROLES = ['PATIENT', 'DOCTOR', 'RECEPTIONIST', 'ADMIN']

export default function UsersPage() {
    const { user: currentUser } = useAuth()
    const showToast = useToast()
    const [users, setUsers] = useState([])
    const [search, setSearch] = useState('')
    const [loading, setLoading] = useState(true)
    const [savingId, setSavingId] = useState(null)
    const [error, setError] = useState('')

    useEffect(() => {
        getUsers()
            .then(data => setUsers(Array.isArray(data) ? data : []))
            .catch(err => setError(err?.response?.data?.message || err?.response?.data?.error || 'Failed to load users.'))
            .finally(() => setLoading(false))
    }, [])

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase()
        if (!query) return users
        return users.filter(user =>
            (user.name || '').toLowerCase().includes(query)
            || (user.email || '').toLowerCase().includes(query)
            || (user.role || '').toLowerCase().includes(query)
        )
    }, [users, search])

    const changeRole = async (target, role) => {
        if (role === target.role) return
        setSavingId(target.id)
        try {
            const updated = await updateUserRole(target.id, role)
            setUsers(items => items.map(item => item.id === updated.id ? updated : item))
            showToast(`${updated.name}'s role is now ${updated.role}.`, 'success')
        } catch (err) {
            showToast(err?.response?.data?.message || err?.response?.data?.error || 'Could not update role.', 'error')
        } finally {
            setSavingId(null)
        }
    }

    return (
        <div className="main-content fade-in">
            <div className="page-header">
                <div>
                    <h1 className="page-title">User Management</h1>
                    <p className="page-subtitle">Assign application roles to registered accounts.</p>
                </div>
            </div>

            <div className="alert alert-info" style={{ marginBottom: '20px' }}>
                Public registration always creates a Patient. Only administrators can grant privileged roles here.
            </div>

            <div className="form-group" style={{ marginBottom: '24px', maxWidth: '420px' }}>
                <input
                    type="search"
                    value={search}
                    onChange={event => setSearch(event.target.value)}
                    placeholder="Search by name, email or role"
                />
            </div>

            {loading && <div className="spinner-wrapper"><div className="spinner" /></div>}
            {error && <div className="alert alert-error">{error}</div>}

            {!loading && !error && (
                <div className="admin-users-list">
                    {filtered.map(account => (
                        <div className="admin-user-row" key={account.id}>
                            <div>
                                <div className="doctor-name">{account.name}</div>
                                <div className="doctor-email">{account.email}</div>
                            </div>
                            <div className="admin-role-control">
                                {account.id === currentUser?.id && <span className="badge badge-gray">You</span>}
                                <select
                                    aria-label={`Role for ${account.name}`}
                                    value={account.role}
                                    disabled={savingId === account.id}
                                    onChange={event => changeRole(account, event.target.value)}
                                >
                                    {ROLES.map(role => <option value={role} key={role}>{role}</option>)}
                                </select>
                            </div>
                        </div>
                    ))}
                    {filtered.length === 0 && <div className="empty-state"><div className="empty-title">No users found</div></div>}
                </div>
            )}
        </div>
    )
}
