import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/Toast'
import { getMyPaymentTransactions, getMyPatient } from '../api'

function statusBadgeClass(status) {
    if (status === 'COMPLETED') return 'badge-green'
    if (status === 'PENDING') return 'badge-cyan'
    if (status === 'FAILED') return 'badge-red'
    if (status === 'EXPIRED') return 'badge-gray'
    return 'badge-gray'
}

export default function PaymentsPage() {
    const { user } = useAuth()
    const toast = useToast()
    const [loading, setLoading] = useState(true)
    const [transactions, setTransactions] = useState([])

    useEffect(() => {
        let mounted = true
        if (!user?.id) {
            setLoading(false)
            return
        }

        ;(async () => {
            try {
                // Payments are recorded against the patient/profile id used when
                // creating the checkout session. Prefer the patient's id if available.
                const patient = await getMyPatient()
                const idToUse = patient?.id || user.id
                const txs = await getMyPaymentTransactions(idToUse)
                if (mounted) setTransactions(txs)
            } catch (err) {
                const msg = err?.response?.data?.message || 'Failed to load payments'
                toast(msg, 'error')
            } finally {
                if (mounted) setLoading(false)
            }
        })()

        return () => { mounted = false }
    }, [user?.id, toast])

    const totals = useMemo(() => {
        const completed = transactions.filter(t => t.status === 'COMPLETED')
        const pending = transactions.filter(t => t.status === 'PENDING')
        const amount = completed.reduce((sum, t) => sum + Number(t.amount || 0), 0)
        return {
            total: transactions.length,
            completed: completed.length,
            pending: pending.length,
            amount: amount.toFixed(2),
        }
    }, [transactions])

    return (
        <div className="main-content fade-in">
            <h1 className="page-title">My Payments</h1>

            <div className="stats-grid">
                <div className="stat-card">
                    <div>
                        <div className="stat-value">{totals.total}</div>
                        <div className="stat-label">Transactions</div>
                    </div>
                </div>
                <div className="stat-card">
                    <div>
                        <div className="stat-value">{totals.completed}</div>
                        <div className="stat-label">Completed</div>
                    </div>
                </div>
                <div className="stat-card">
                    <div>
                        <div className="stat-value">{totals.pending}</div>
                        <div className="stat-label">Pending</div>
                    </div>
                </div>
                <div className="stat-card">
                    <div>
                        <div className="stat-value">{totals.amount}</div>
                        <div className="stat-label">Total Paid</div>
                    </div>
                </div>
            </div>

            <div className="section">
                <div className="card">
                    <div className="card-title">Payment History</div>

                    {loading && <p style={{ color: 'var(--text-muted)' }}>Loading payments...</p>}

                    {!loading && transactions.length === 0 && (
                        <div className="alert alert-warning" style={{ marginTop: '12px' }}>
                            No payments found yet.
                        </div>
                    )}

                    {!loading && transactions.length > 0 && (
                        <div style={{ marginTop: '12px', display: 'grid', gap: '10px' }}>
                            {transactions.map(tx => (
                                <div key={tx.id} className="doctor-card" style={{ cursor: 'default' }}>
                                    <div style={{ display: 'grid', gap: '4px' }}>
                                        <div className="doctor-name" style={{ fontSize: '0.95rem' }}>
                                            {tx.currency?.toUpperCase()} {tx.amount} - {tx.description || 'Appointment Payment'}
                                        </div>
                                        <div className="doctor-email">
                                            Session: <span style={{ fontFamily: 'monospace' }}>{tx.stripeSessionId}</span>
                                        </div>
                                        {tx.appointmentId && (
                                            <div className="doctor-email">
                                                Appointment: <Link to={`/checkout?appointmentId=${tx.appointmentId}`}>{tx.appointmentId}</Link>
                                            </div>
                                        )}
                                        <div className="doctor-email">
                                            Created: {new Date(tx.createdAt).toLocaleString()}
                                        </div>
                                    </div>
                                    <span className={`badge ${statusBadgeClass(tx.status)}`}>{tx.status}</span>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}
