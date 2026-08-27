import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/Toast'
import { getGoogleAuthUrl } from '../api'

export default function RegisterPage() {
    const { register } = useAuth()
    const toast = useToast()
    const navigate = useNavigate()
    const [form, setForm] = useState({
        name: '',
        email: '',
        password: '',
        phoneNumber: '',
    })
    const [loading, setLoading] = useState(false)
    const [googleLoading, setGoogleLoading] = useState(false)
    const [error, setError] = useState('')

    const handleChange = e => setForm({ ...form, [e.target.name]: e.target.value })

    const handleGoogleLogin = async () => {
        try {
            setGoogleLoading(true)
            setError('')
            const state = Math.random().toString(36).substring(2) + Date.now().toString(36)
            sessionStorage.setItem('oauth_state', state)
            const redirectUri = window.location.origin + '/auth/callback'
            const authUrl = await getGoogleAuthUrl(state, redirectUri)
            window.location.href = authUrl
        } catch (err) {
            setError('Failed to initialize Google authentication. Please try again.')
            setGoogleLoading(false)
        }
    }

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
                                placeholder="12–128 characters"
                                value={form.password}
                                onChange={handleChange}
                                required
                                minLength={12}
                                maxLength={128}
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

                        <button
                            id="register-submit"
                            type="submit"
                            className="btn btn-primary btn-full btn-lg"
                            disabled={loading || googleLoading}
                            style={{ marginTop: '8px' }}
                        >
                            {loading ? '⏳ Creating account...' : '✨ Create Account'}
                        </button>
                    </form>

                    <div className="oauth-divider">
                        <div className="oauth-divider-line" />
                        <span className="oauth-divider-text">or continue with</span>
                        <div className="oauth-divider-line" />
                    </div>

                    <button
                        type="button"
                        id="google-register-btn"
                        onClick={handleGoogleLogin}
                        disabled={googleLoading || loading}
                        className="btn-google"
                    >
                        <svg width="20" height="20" viewBox="0 0 24 24">
                            <path
                                fill="#4285F4"
                                d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17z"
                            />
                            <path
                                fill="#34A853"
                                d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.33 24 12 24z"
                            />
                            <path
                                fill="#FBBC05"
                                d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.18 0 10.02 0 12s.45 3.82 1.25 5.42l4.03-3.15z"
                            />
                            <path
                                fill="#EA4335"
                                d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"
                            />
                        </svg>
                        <span>{googleLoading ? 'Connecting to Google...' : 'Sign in with Google'}</span>
                    </button>

                    <div className="auth-footer">
                        Already have an account? <Link to="/login">Sign in</Link>
                    </div>
                </div>
            </div>
        </div>
    )
}
