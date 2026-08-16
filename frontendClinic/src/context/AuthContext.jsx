import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { login as apiLogin, logout as apiLogout, register as apiRegister } from '../api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null)
    const [token, setToken] = useState(() => localStorage.getItem('accessToken'))
    const [refreshToken, setRefreshToken] = useState(() => localStorage.getItem('refreshToken'))
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        // Restore user from localStorage if token exists
        const stored = localStorage.getItem('user')
        if (stored) {
            try { setUser(JSON.parse(stored)) } catch { }
        }
        setLoading(false)
    }, [])

    const login = useCallback(async (email, password) => {
        const data = await apiLogin({ email, password })
        localStorage.setItem('accessToken', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken)
        localStorage.setItem('user', JSON.stringify(data.user))
        setToken(data.accessToken)
        setRefreshToken(data.refreshToken)
        setUser(data.user)
        return data.user
    }, [])

    const register = useCallback(async (payload) => {
        const data = await apiRegister(payload)
        return data
    }, [])

    const logout = useCallback(async () => {
        try { await apiLogout() } catch { }
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
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
        <AuthContext.Provider value={{ user, token, refreshToken, login, logout, register, isAuthenticated, hasRole, loading }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    return useContext(AuthContext)
}
