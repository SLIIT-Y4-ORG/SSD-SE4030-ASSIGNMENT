import { useState, createContext, useContext, useCallback } from 'react'

const ToastContext = createContext(null)

let id = 0

export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([])

    const showToast = useCallback((message, type = 'info') => {
        const toastId = ++id
        setToasts(prev => [...prev, { id: toastId, message, type }])
        setTimeout(() => setToasts(prev => prev.filter(t => t.id !== toastId)), 4000)
    }, [])

    const icons = { success: '✅', error: '❌', info: 'ℹ️' }

    return (
        <ToastContext.Provider value={showToast}>
            {children}
            <div className="toast-container">
                {toasts.map(t => (
                    <div key={t.id} className={`toast toast-${t.type}`}>
                        <span>{icons[t.type]}</span>
                        <span>{t.message}</span>
                    </div>
                ))}
            </div>
        </ToastContext.Provider>
    )
}

export function useToast() {
    return useContext(ToastContext)
}
