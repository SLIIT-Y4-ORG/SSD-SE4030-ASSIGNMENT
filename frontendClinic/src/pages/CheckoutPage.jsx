import { useState, useEffect } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/Toast'
import { getAppointment, initiatePaymentSession } from '../api'

function CheckoutForm({ appointmentId, amount, currency, doctorName, appointmentDate, startTime }) {
    const { user } = useAuth()
    const toast = useToast()
    const navigate = useNavigate()
    const [processing, setProcessing] = useState(false)
    const [cardError, setCardError] = useState('')
    const [formData, setFormData] = useState({
        email: user?.email || '',
        fullName: user?.fullName || '',
        phone: user?.phoneNumber || '',
    })

    const handleFormChange = (e) => {
        const { name, value } = e.target
        setFormData(prev => ({ ...prev, [name]: value }))
    }

    const handleSubmit = async (e) => {
        e.preventDefault()

        if (!formData.email || !formData.fullName || !formData.phone) {
            setCardError('All patient details are required.')
            return
        }

        setProcessing(true)
        setCardError('')

        try {
            // Call backend to initiate payment session
            const response = await initiatePaymentSession(appointmentId)

            if (response.checkoutUrl) {
                // Redirect to Stripe Checkout (Stripe-hosted payment page)
                window.location.href = response.checkoutUrl
                // After payment, Stripe will redirect back to success/cancel URLs
            } else {
                setCardError('Failed to create checkout session')
                setProcessing(false)
            }
        } catch (err) {
            setCardError(err.message || 'Payment failed. Please try again.')
            setProcessing(false)
        }
    }

    return (
        <form onSubmit={handleSubmit} className="checkout-form">
            <div className="form-section">
                <h3 className="form-title">Appointment Details</h3>
                <div className="info-grid">
                    <div className="info-item">
                        <label className="info-label">Doctor</label>
                        <p className="info-value">{doctorName}</p>
                    </div>
                    <div className="info-item">
                        <label className="info-label">Date</label>
                        <p className="info-value">{appointmentDate}</p>
                    </div>
                    <div className="info-item">
                        <label className="info-label">Time</label>
                        <p className="info-value">{startTime}</p>
                    </div>
                    <div className="info-item">
                        <label className="info-label">Amount</label>
                        <p className="info-value" style={{ fontWeight: 'bold', color: 'var(--accent)' }}>
                            {currency} {amount}
                        </p>
                    </div>
                </div>
            </div>

            <div className="form-section">
                <h3 className="form-title">Patient Information</h3>
                <div className="form-group">
                    <label htmlFor="fullName">Full Name *</label>
                    <input
                        type="text"
                        id="fullName"
                        name="fullName"
                        value={formData.fullName}
                        onChange={handleFormChange}
                        required
                        placeholder="Enter your full name"
                        className="form-input"
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="email">Email *</label>
                    <input
                        type="email"
                        id="email"
                        name="email"
                        value={formData.email}
                        onChange={handleFormChange}
                        required
                        placeholder="Enter your email"
                        className="form-input"
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="phone">Phone Number *</label>
                    <input
                        type="tel"
                        id="phone"
                        name="phone"
                        value={formData.phone}
                        onChange={handleFormChange}
                        required
                        placeholder="Enter your phone number"
                        className="form-input"
                    />
                </div>
            </div>

            <div className="form-section">
                <h3 className="form-title">Secure Checkout</h3>
                <p style={{ lineHeight: '1.5', marginBottom: '16px', color: '#666' }}>
                    You will be redirected to Stripe Checkout to securely enter your payment information.
                </p>

                {cardError && <div className="alert alert-error" style={{ marginBottom: '16px' }}>{cardError}</div>}

                <button
                    type="submit"
                    disabled={processing}
                    className="btn btn-primary btn-full"
                    style={{ fontSize: '1rem', padding: '12px 16px' }}
                >
                    {processing ? 'Redirecting to Stripe...' : `Proceed to Payment (${currency} ${amount})`}
                </button>
            </div>
        </form>
    )
}

export default function CheckoutPage() {
    const [searchParams] = useSearchParams()
    const navigate = useNavigate()
    const toast = useToast()
    const [appointment, setAppointment] = useState(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    const appointmentId = searchParams.get('appointmentId')

    useEffect(() => {
        if (!appointmentId) {
            setError('Appointment ID is required')
            setLoading(false)
            return
        }

        getAppointment(appointmentId)
            .then(setAppointment)
            .catch(err => {
                const errorMsg = err?.response?.data?.error || 'Failed to load appointment'
                setError(errorMsg)
                toast(errorMsg, 'error')
            })
            .finally(() => setLoading(false))
    }, [appointmentId, toast])

    if (loading) {
        return <div className="spinner-wrapper"><div className="spinner" /><span className="spinner-text">Loading appointment...</span></div>
    }

    if (error) {
        return (
            <div className="main-content">
                <div className="alert alert-error">{error}</div>
                <button className="btn btn-secondary" onClick={() => navigate('/doctors')}>← Back</button>
            </div>
        )
    }

    if (!appointment) {
        return <div className="main-content"><div className="alert alert-error">Appointment not found</div></div>
    }

    return (
        <div className="main-content fade-in">
            <div style={{ maxWidth: '600px', margin: '0 auto' }}>
                <h1 className="page-title">Complete Payment</h1>
                
                <CheckoutForm
                    appointmentId={appointmentId}
                    amount={appointment.amount}
                    currency={appointment.currency}
                    doctorName={appointment.doctorName}
                    appointmentDate={appointment.appointmentDate}
                    startTime={appointment.startTime}
                />
            </div>
        </div>
    )
}
