import React from 'react'
import { useSearchParams } from 'react-router-dom'
import BookingForm from '../../components/BookingForm'

export default function BookingPage() {
    const [searchParams] = useSearchParams()
    const doctorId = searchParams.get('doctorId') || ''
    const slotId = searchParams.get('slotId') || ''

    return (
        <div className="main-content fade-in">
            <div className="page-header">
                <div>
                    <h1 className="page-title">Book Appointment</h1>
                    <p className="page-subtitle">Confirm appointment details and continue to payment.</p>
                </div>
            </div>
            <div className="card" style={{ maxWidth: '720px' }}>
                <BookingForm initialDoctorId={doctorId} initialSlotId={slotId} />
            </div>
        </div>
    )
}
