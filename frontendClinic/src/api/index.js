import axios from 'axios'

// API Gateway URL - all requests will go through the gateway
// when hosted

// development
const API_GATEWAY = (import.meta.env.VITE_API_GATEWAY || 'http://localhost:8080').replace(/\/+$/, '')
// const API_GATEWAY = 'http://localhost:8080'

function getToken() {
    return localStorage.getItem('accessToken')
}

function authHeaders() {
    return { Authorization: `Bearer ${getToken()}` }
}

// ── Auth / User Service ─────────────────────────────────────────────────────

export async function register(data) {
    const res = await axios.post(`${API_GATEWAY}/api/auth/register`, data)
    return res.data
}

export async function login(data) {
    const res = await axios.post(`${API_GATEWAY}/api/auth/login`, data)
    return res.data
}

export async function logout() {
    const res = await axios.post(
        `${API_GATEWAY}/api/auth/logout`,
        {},
        { headers: authHeaders() }
    )
    return res.data
}

export async function refreshTokens(refreshToken) {
    const res = await axios.post(`${API_GATEWAY}/api/auth/refresh`, { refreshToken })
    return res.data
}

export async function getMe() {
    const res = await axios.get(`${API_GATEWAY}/api/auth/me`, {
        headers: authHeaders(),
    })
    return res.data
}

// ── Doctor Service ──────────────────────────────────────────────────────────

export async function getDoctors(params = {}) {
    const res = await axios.get(`${API_GATEWAY}/api/doctors`, {
        headers: authHeaders(),
        params,
    })
    return res.data
}

/**
 * Find the doctor profile linked to a given user-service userId.
 * There is no /api/doctors/me endpoint, so we fetch all doctors
 * and filter by userId on the client side.
 */
export async function getDoctorByUserId(userId) {
    const doctors = await getDoctors()
    return doctors.find(d => d.userId === userId) ?? null
}

export async function getDoctorById(id) {
    const res = await axios.get(`${API_GATEWAY}/api/doctors/${id}`, {
        headers: authHeaders(),
    })
    return res.data
}

export async function createDoctor(data) {
    const res = await axios.post(`${API_GATEWAY}/api/doctors`, data, {
        headers: authHeaders(),
    })
    return res.data
}

export async function updateDoctor(doctorId, data) {
    const res = await axios.put(`${API_GATEWAY}/api/doctors/${doctorId}`, data, {
        headers: authHeaders(),
    })
    return res.data
}

export async function verifyDoctor(doctorId, verified) {
    const res = await axios.patch(
        `${API_GATEWAY}/api/doctors/${doctorId}/verify`,
        { verified },
        { headers: authHeaders() }
    )
    return res.data
}

export async function getSlots(doctorId, date) {
    const url = (date && date !== 'undefined' && date !== undefined) ? `${API_GATEWAY}/api/doctors/${doctorId}/slots?date=${date}` : `${API_GATEWAY}/api/doctors/${doctorId}/slots`
    const response = await fetch(url, {
        headers: authHeaders(),
    })
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const data = await response.json()
    return data
}

export async function createSlot(doctorId, data) {
    const res = await axios.post(
        `${API_GATEWAY}/api/doctors/${doctorId}/slots`,
        [data],
        { headers: authHeaders() }
    )
    return res.data
}

export async function reserveSlot(slotId, patientId, appointmentId = null) {
    const res = await axios.post(
        `${API_GATEWAY}/api/slots/${slotId}/reserve`,
        { patientId, appointmentId },
        { headers: authHeaders() }
    )
    return res.data
}

export async function releaseSlot(slotId) {
    const res = await axios.post(
        `${API_GATEWAY}/api/slots/${slotId}/release`,
        {},
        { headers: authHeaders() }
    )
    return res.data
}

export async function linkDoctorUser(doctorId, userId) {
    const res = await axios.patch(
        `${API_GATEWAY}/api/doctors/${doctorId}/link-user`,
        null,
        {
            headers: authHeaders(),
            params: { userId },
        }
    )
    return res.data
}

// ── Appointment Service ─────────────────────────────────────────────────────

export async function createAppointment(data) {
    const res = await axios.post(
        `${API_GATEWAY}/api/appointments`,
        data,
        { headers: authHeaders() }
    )
    return res.data
}

export async function getAppointment(appointmentId) {
    const res = await axios.get(
        `${API_GATEWAY}/api/appointments/${appointmentId}`,
        { headers: authHeaders() }
    )
    return res.data
}

export async function getMyAppointments() {
    const res = await axios.get(
        `${API_GATEWAY}/api/appointments`,
        { headers: authHeaders() }
    )
    return res.data
}

export async function getAppointments(params = {}) {
    const res = await axios.get(`${API_GATEWAY}/api/appointments`, {
        headers: authHeaders(),
        params,
    })
    return res.data
}

export async function cancelAppointment(appointmentId) {
    const res = await axios.patch(
        `${API_GATEWAY}/api/appointments/${appointmentId}/cancel`,
        {},
        { headers: authHeaders() }
    )
    return res.data
}

export async function getAppointmentStatus(appointmentId) {
    const res = await axios.get(
        `${API_GATEWAY}/api/appointments/${appointmentId}/status`,
        { headers: authHeaders() }
    )
    return res.data
}

export async function createPaymentSession(appointmentId) {
    const res = await axios.post(
        `${API_GATEWAY}/api/appointments/${appointmentId}/payment-session`,
        {},
        { headers: authHeaders() }
    )
    return res.data
}

// ── Payment Service ────────────────────────────────────────────────────────

export async function initiatePaymentSession(appointmentId) {
    const res = await axios.post(
        `${API_GATEWAY}/api/appointments/${appointmentId}/payment-session`,
        {},
        { headers: authHeaders() }
    )
    return res.data
}

export async function createPaymentIntent(appointmentId, paymentMethodId) {
    const res = await axios.post(
        `${API_GATEWAY}/api/payments/intent`,
        { appointmentId, paymentMethodId },
        { headers: authHeaders() }
    )
    return res.data
}

export async function getMyPaymentTransactions(userId) {
    const res = await axios.get(
        `${API_GATEWAY}/api/payments/users/${userId}/transactions`,
        { headers: authHeaders() }
    )
    return res.data
}

export async function confirmCheckoutSession(sessionId) {
    const res = await axios.post(
        `${API_GATEWAY}/api/payments/checkout-session/${sessionId}/confirm`,
        {},
        { headers: authHeaders() }
    )
    return res.data
}

export async function notifyAppointmentPayment(appointmentId, paymentStatus, transactionId = null) {
    const res = await axios.post(
        `${API_GATEWAY}/api/appointments/payment-callback`,
        { appointmentId, paymentStatus, transactionId },
        { headers: authHeaders() }
    )
    return res.data
}

// ── Patient helpers ───────────────────────────────────────────────────────
export async function createPatientProfile(data) {
    const res = await axios.post(`${API_GATEWAY}/api/patients`, data, {
        headers: authHeaders(),
    })
    return res.data
}

export async function getMyPatientProfile() {
    const res = await axios.get(`${API_GATEWAY}/api/patients/me`, {
        headers: authHeaders(),
    })
    return res.data
}

export async function updatePatientProfile(id, data) {
    const res = await axios.put(`${API_GATEWAY}/api/patients/${id}`, data, {
        headers: authHeaders(),
    })
    return res.data
}

export async function getPatients() {
    const res = await axios.get(`${API_GATEWAY}/api/patients`, {
        headers: authHeaders(),
    })
    return res.data
}

export async function getMyPatient() {
    const res = await axios.get(`${API_GATEWAY}/api/patients/me`, {
        headers: authHeaders(),
    })
    return res.data
}

export async function getPatientById(id) {
    const res = await axios.get(`${API_GATEWAY}/api/patients/${id}`, {
        headers: authHeaders(),
    })
    return res.data
}
