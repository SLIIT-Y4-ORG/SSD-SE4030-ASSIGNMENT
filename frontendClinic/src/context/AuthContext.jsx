import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { getMe, login as apiLogin, loginWithGoogle as apiLoginWithGoogle, logout as apiLogout, register as apiRegister } from '../api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null)
    const [token, setToken] = useState(() => sessionStorage.getItem('accessToken'))
    const [refreshToken, setRefreshToken] = useState(() => sessionStorage.getItem('refreshToken'))
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        // Remove credentials written by older builds. Persistent browser storage
        // must never be trusted as proof of an authenticated identity.
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')

        const restoreSession = async () => {
            const storedToken = sessionStorage.getItem('accessToken')
            if (!storedToken) {
                setLoading(false)
                return
            }

            try {
                const authenticatedUser = await getMe()
                sessionStorage.setItem('user', JSON.stringify(authenticatedUser))
                setUser(authenticatedUser)
            } catch {
                sessionStorage.removeItem('accessToken')
                sessionStorage.removeItem('refreshToken')
                sessionStorage.removeItem('user')
                setToken(null)
                setRefreshToken(null)
                setUser(null)
            } finally {
                setLoading(false)
            }
        }

        restoreSession()
    }, [])

    const login = useCallback(async (email, password) => {
        const data = await apiLogin({ email, password })
        sessionStorage.setItem('accessToken', data.accessToken)
        sessionStorage.setItem('refreshToken', data.refreshToken)
        sessionStorage.setItem('user', JSON.stringify(data.user))
        setToken(data.accessToken)
        setRefreshToken(data.refreshToken)
        setUser(data.user)
        return data.user
    }, [])

    const loginWithGoogle = useCallback(async (code, redirectUri) => {
        const data = await apiLoginWithGoogle(code, redirectUri)
        sessionStorage.setItem('accessToken', data.accessToken)
        sessionStorage.setItem('refreshToken', data.refreshToken)
        sessionStorage.setItem('user', JSON.stringify(data.user))
        setToken(data.accessToken)
        setRefreshToken(data.refreshToken)
        setUser(data.user)
        return data.user
    }, [])

    const register = useCallback(async (payload) => {
        const data = await apiRegister(payload)
        return data
    }, [])

    const refreshUser = useCallback(async () => {
        const authenticatedUser = await getMe()
        sessionStorage.setItem('user', JSON.stringify(authenticatedUser))
        setUser(authenticatedUser)
        return authenticatedUser
    }, [])

    const logout = useCallback(async () => {
        try { await apiLogout() } catch { }
        sessionStorage.removeItem('accessToken')
        sessionStorage.removeItem('refreshToken')
        sessionStorage.removeItem('user')
        setToken(null)
        setRefreshToken(null)
        setUser(null)
    }, [])

    const isAuthenticated = !!token && !!user

    const hasRole = useCallback(
        (...roles) => user && roles.includes(user.role),
        [user]
    )

    return (
        <AuthContext.Provider value={{ user, token, refreshToken, login, loginWithGoogle, logout, register, refreshUser, isAuthenticated, hasRole, loading }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    return useContext(AuthContext)
}
