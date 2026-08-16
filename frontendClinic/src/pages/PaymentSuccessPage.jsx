import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { confirmCheckoutSession, notifyAppointmentPayment } from '../api'
import { useToast } from '../components/Toast'

export default function PaymentSuccessPage() {
    const navigate = useNavigate()
    const location = useLocation()
    const toast = useToast()
    const state = location.state || {}
    const [confirming, setConfirming] = useState(true)
    const [tx, setTx] = useState(null)

    const sessionId = new URLSearchParams(location.search).get('session_id')

    useEffect(() => {
        if (!sessionId) {
            setConfirming(false)
            return
        }

        confirmCheckoutSession(sessionId)
            .then(async (transaction) => {
                setTx(transaction)

                if (transaction?.appointmentId && transaction?.status === 'COMPLETED') {
                    await notifyAppointmentPayment(transaction.appointmentId, 'success', transaction.id)
                }
            })
            .catch(err => {
                const msg = err?.response?.data?.message || 'Failed to confirm payment status'
                toast(msg, 'error')
            })
            .finally(() => setConfirming(false))
    }, [sessionId, toast])

    return (
        <div className="main-content fade-in">
            <div style={{ maxWidth: '600px', margin: '40px auto', textAlign: 'center' }}>
                <div style={{ marginBottom: '32px' }}>
                    <div style={{
                        width: '80px',
                        height: '80px',
                        margin: '0 auto 24px',
                        background: 'var(--success)',
                        borderRadius: '50%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '2rem'
                    }}>
                        ✓
                    </div>
                    <h1 className="page-title">Payment Successful!</h1>
                    <p style={{ color: 'var(--text-muted)', marginBottom: '24px' }}>
                        Your appointment payment has been processed successfully.
                    </p>
                    {confirming && (
                        <p style={{ color: 'var(--text-muted)', marginBottom: '12px' }}>
                            Finalizing payment record...
                        </p>
                    )}
                </div>

                {state.doctorName && (
                    <div className="card" style={{ marginBottom: '24px' }}>
                        <div className="card-title">Appointment Confirmation</div>
                        <div className="info-grid" style={{ textAlign: 'left', marginTop: '16px' }}>
                            <div className="info-item">
                                <label className="info-label">Doctor</label>
                                <p className="info-value">{state.doctorName}</p>
                            </div>
                            <div className="info-item">
                                <label className="info-label">Date & Time</label>
                                <p className="info-value">{state.appointmentDate} at {state.startTime}</p>
                            </div>
                            <div className="info-item">
                                <label className="info-label">Amount Paid</label>
                                <p className="info-value" style={{ fontWeight: 'bold', color: 'var(--accent)' }}>
                                    {state.currency} {state.amount}
                                </p>
                            </div>
                            <div className="info-item">
                                <label className="info-label">Appointment ID</label>
                                <p className="info-value" style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{tx?.appointmentId || state.appointmentId || '—'}</p>
                            </div>
                            <div className="info-item">
                                <label className="info-label">Payment Status</label>
                                <p className="info-value">{tx?.status || 'COMPLETED'}</p>
                            </div>
                        </div>
                    </div>
                )}

                <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}>
                    <button className="btn btn-primary" onClick={() => navigate('/doctors')}>
                        Browse More Doctors
                    </button>
                    <button className="btn btn-secondary" onClick={() => navigate('/payments')}>
                        View Payments
                    </button>
                    {/* Under Construction */}
                    {/* <button className="btn btn-secondary" onClick={() => navigate('/my-appointments')}>
                        View My Appointments
                    </button> */}
                </div>

                <p style={{ marginTop: '32px', fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                    A confirmation email has been sent to your registered email address.
                </p>
            </div>
        </div>
    )
}
