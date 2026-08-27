import { useEffect, useState, useRef } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../components/Toast'

export default function OAuthCallbackPage() {
    const [searchParams] = useSearchParams()
    const navigate = useNavigate()
    const { loginWithGoogle } = useAuth()
    const toast = useToast()

    const [error, setError] = useState('')
    const [processing, setProcessing] = useState(true)
    const processedRef = useRef(false)

    useEffect(() => {
        if (processedRef.current) return
        processedRef.current = true

        const code = searchParams.get('code')
        const state = searchParams.get('state')
        const errorParam = searchParams.get('error')

        if (errorParam) {
            setProcessing(false)
            setError(`Google sign-in was cancelled or denied: ${errorParam}`)
            return
        }

        if (!code) {
            setProcessing(false)
            setError('Missing authorization code from Google authentication callback.')
            return
        }

        // Verify CSRF state token against sessionStorage if present
        const savedState = sessionStorage.getItem('oauth_state')
        if (savedState && state && savedState !== state) {
            setProcessing(false)
            setError('Security validation failed: state parameter mismatch. Please try signing in again.')
            sessionStorage.removeItem('oauth_state')
            return
        }
        sessionStorage.removeItem('oauth_state')

        const completeLogin = async () => {
            try {
                // Exact redirect URI used during initial authorization request
                const redirectUri = window.location.origin + '/auth/callback'
                const user = await loginWithGoogle(code, redirectUri)
                toast(`Welcome, ${(user.name || user.email).split(' ')[0]}! Signed in with Google 🎉`, 'success')
                navigate('/dashboard', { replace: true })
            } catch (err) {
                const msg = err?.response?.data?.error || err?.response?.data?.message || err.message || 'Google sign-in failed. Please try again.'
                setError(msg)
            } finally {
                setProcessing(false)
            }
        }

        completeLogin()
    }, [searchParams, loginWithGoogle, navigate, toast])

    return (
        <div className="auth-page">
            <div className="auth-container">
                <div className="auth-logo">
                    <span className="auth-logo-icon">🏥</span>
                    <div className="auth-logo-title">ClinicMate</div>
                    <div className="auth-logo-subtitle">Authenticating with Google</div>
                </div>

                <div className="form-card fade-in" style={{ textAlign: 'center' }}>
                    {processing && (
                        <div className="spinner-wrapper" style={{ padding: '20px 0' }}>
                            <div className="spinner" />
                            <h2 style={{ fontSize: '1.25rem', marginTop: '16px', fontWeight: 600 }}>
                                Completing Google Sign-In...
                            </h2>
                            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '8px' }}>
                                Verifying your OpenID credentials and issuing a secure session.
                            </p>
                        </div>
                    )}

                    {error && (
                        <div>
                            <div className="alert alert-error" style={{ textAlign: 'left', marginBottom: '20px' }}>
                                <span>⚠️</span> {error}
                            </div>
                            <Link to="/login" className="btn btn-primary btn-full">
                                ← Return to Sign In
                            </Link>
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}
