import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/Toast'

const ROLES = ['PATIENT', 'DOCTOR', 'RECEPTIONIST', 'ADMIN']

export default function RegisterPage() {
    const { register } = useAuth()
    const toast = useToast()
    const navigate = useNavigate()
    const [form, setForm] = useState({
        name: '',
        email: '',
        password: '',
        phoneNumber: '',
        role: 'PATIENT',
    })
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    const handleChange = e => setForm({ ...form, [e.target.name]: e.target.value })

    const handleSubmit = async e => {
        e.preventDefault()
        setError('')
        setLoading(true)
        try {
            const { phoneNumber, ...rest } = form
            await register({ ...rest, phone: phoneNumber })
            toast('Account created! Please log in.', 'success')
            navigate('/login')
        } catch (err) {
            const data = err?.response?.data
            const msg = data?.details
                ? Object.values(data.details).join(', ')
                : data?.error || 'Registration failed.'
            setError(msg)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="auth-page">
            <div className="auth-container" style={{ maxWidth: '520px' }}>
                <div className="auth-logo">
                    <span className="auth-logo-icon">🏥</span>
                    <div className="auth-logo-title">ClinicMate</div>
                    <div className="auth-logo-subtitle">Create your account</div>
                </div>

                <div className="form-card fade-in" style={{ maxWidth: '100%' }}>
                    <h1 className="auth-heading">Create Account</h1>
                    <p className="auth-subheading">Join ClinicMate today</p>

                    {error && (
                        <div className="alert alert-error">
                            <span>⚠️</span> {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label htmlFor="name">Full Name</label>
                            <input
                                id="name"
                                name="name"
                                type="text"
                                placeholder="John Perera"
                                value={form.name}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="reg-email">Email Address</label>
                            <input
                                id="reg-email"
                                name="email"
                                type="email"
                                placeholder="john@clinic.com"
                                value={form.email}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="reg-password">Password</label>
                            <input
                                id="reg-password"
                                name="password"
                                type="password"
                                placeholder="Minimum 8 characters"
                                value={form.password}
                                onChange={handleChange}
                                required
                                minLength={8}
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="phoneNumber">Phone Number</label>
                            <input
                                id="phoneNumber"
                                name="phoneNumber"
                                type="tel"
                                placeholder="+94771234567"
                                value={form.phoneNumber}
                                onChange={handleChange}
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="role">Role</label>
                            <select id="role" name="role" value={form.role} onChange={handleChange}>
                                {ROLES.map(r => (
                                    <option key={r} value={r}>{r}</option>
                                ))}
                            </select>
                        </div>

                        <button
                            id="register-submit"
                            type="submit"
                            className="btn btn-primary btn-full btn-lg"
                            disabled={loading}
                            style={{ marginTop: '8px' }}
                        >
                            {loading ? '⏳ Creating account...' : '✨ Create Account'}
                        </button>
                    </form>

                    <div className="auth-footer">
                        Already have an account? <Link to="/login">Sign in</Link>
                    </div>
                </div>
            </div>
        </div>
    )
}
