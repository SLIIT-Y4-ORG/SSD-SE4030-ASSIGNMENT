import React, { useState } from 'react'
import { createAppointment, getMyPatient, getPatientById } from '../api'
import { useNavigate } from 'react-router-dom'

const DEFAULT_AMOUNT = Number(import.meta.env.VITE_DEFAULT_APPOINTMENT_AMOUNT || 2500)
const DEFAULT_CURRENCY = import.meta.env.VITE_DEFAULT_APPOINTMENT_CURRENCY || 'LKR'

function getPatientDisplayName(patient) {
    if (!patient) return ''
    const nameFromParts = [patient.firstName, patient.lastName].filter(Boolean).join(' ').trim()
    return nameFromParts || patient.name || patient.email || ''
}

export default function BookingForm({ initialDoctorId = '', initialSlotId = '' }) {
    const [doctorId, setDoctorId] = useState(initialDoctorId)
    const [slotId, setSlotId] = useState(initialSlotId)
    const [patientId, setPatientId] = useState('')
    const [reason, setReason] = useState('Consultation')
    const [notes, setNotes] = useState('')
    const [amount, setAmount] = useState(String(DEFAULT_AMOUNT))
    const [currency, setCurrency] = useState(DEFAULT_CURRENCY)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState(null)
    const [patientLookupLoading, setPatientLookupLoading] = useState(false)
    const [patientName, setPatientName] = useState('')
    const navigate = useNavigate()

    async function handleSubmit(e) {
        e.preventDefault()
        setLoading(true)
        setError(null)
        try {
            if (!patientId) {
                throw new Error('Patient ID is required')
            }
            const payload = {
                doctorId,
                slotId,
                patientId,
                reason,
                notes,
                amount: Number(amount),
                currency: currency.trim().toUpperCase(),
            }
            const appointment = await createAppointment(payload)
            navigate(`/checkout?appointmentId=${appointment.id}`)
        } catch (err) {
            console.error(err)
            setError(err?.response?.data?.message || err.message || 'Failed')
        } finally {
            setLoading(false)
        }
    }

    async function useMyProfile() {
        setPatientLookupLoading(true)
        setError(null)
        try {
            const patient = await getMyPatient()
            setPatientId(patient.id || '')
            setPatientName(getPatientDisplayName(patient) || 'Patient loaded')
        } catch (err) {
            setPatientName('')
            setError(err?.response?.data?.message || err.message || 'Unable to load profile')
        } finally {
            setPatientLookupLoading(false)
        }
    }

    async function validatePatientId() {
        if (!patientId) return
        setPatientLookupLoading(true)
        setError(null)
        try {
            const patient = await getPatientById(patientId)
            setPatientName(getPatientDisplayName(patient) || 'Patient loaded')
        } catch (err) {
            setPatientName('')
            setError(err?.response?.data?.message || 'Patient not found')
        } finally {
            setPatientLookupLoading(false)
        }
    }

    return (
        <form onSubmit={handleSubmit} className="booking-form">
            <div className="form-group">
                <label htmlFor="booking-doctor-id">Doctor ID</label>
                <input
                    id="booking-doctor-id"
                    value={doctorId}
                    onChange={e => setDoctorId(e.target.value)}
                    required
                />
            </div>
            <div className="form-group">
                <label htmlFor="booking-slot-id">Slot ID</label>
                <input
                    id="booking-slot-id"
                    value={slotId}
                    onChange={e => setSlotId(e.target.value)}
                    required
                />
            </div>
            <div className="form-group">
                <label htmlFor="booking-patient-id">Patient ID</label>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <input
                        id="booking-patient-id"
                        value={patientId}
                        onChange={e => setPatientId(e.target.value)}
                        required
                    />
                    <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={validatePatientId}
                        disabled={patientLookupLoading}
                    >
                        Check
                    </button>
                    <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={useMyProfile}
                        disabled={patientLookupLoading}
                    >
                        Use My Profile
                    </button>
                </div>
                {patientLookupLoading && <div className="spinner-text">Loading patient...</div>}
                {patientName && <div className="spinner-text">Selected: {patientName}</div>}
            </div>
            <div className="form-group">
                <label htmlFor="booking-reason">Reason</label>
                <input
                    id="booking-reason"
                    value={reason}
                    onChange={e => setReason(e.target.value)}
                    placeholder="e.g. General checkup"
                />
            </div>
            <div className="form-group">
                <label htmlFor="booking-notes">Notes</label>
                <input
                    id="booking-notes"
                    value={notes}
                    onChange={e => setNotes(e.target.value)}
                    placeholder="Any special requests or details"
                />
            </div>
            <div className="form-group">
                <label htmlFor="booking-amount">Amount</label>
                <input
                    id="booking-amount"
                    type="number"
                    min="1"
                    value={amount}
                    onChange={e => setAmount(e.target.value)}
                    required
                />
            </div>
            <div className="form-group">
                <label htmlFor="booking-currency">Currency</label>
                <input
                    id="booking-currency"
                    value={currency}
                    onChange={e => setCurrency(e.target.value)}
                    required
                />
            </div>
            {error && <div className="alert alert-error">{error}</div>}
            <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Creating appointment...' : 'Continue to checkout'}
            </button>
        </form>
    )
}