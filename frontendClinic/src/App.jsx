import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ToastProvider } from './components/Toast'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'

import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/doctor/DashboardPage'
import DoctorsPage from './pages/doctor/DoctorsPage'
import DoctorDetailPage from './pages/doctor/DoctorDetailPage'
import AddDoctorPage from './pages/doctor/AddDoctorPage'
import AddSlotPage from './pages/doctor/AddSlotPage'
import MySchedulePage from './pages/doctor/MySchedulePage'
import DoctorApplicationsPage from './pages/doctor/DoctorApplicationsPage'
import BookingPage from './pages/appointment/BookingPage'
import CheckoutPage from './pages/CheckoutPage'
import PaymentSuccessPage from './pages/PaymentSuccessPage'
import PaymentsPage from './pages/PaymentsPage'
import PatientProfilePage from './pages/patient/PatientProfilePage'
import PatientsPage from './pages/patient/PatientsPage'

function Layout({ children }) {
    return (
        <div className="page-wrapper">
            <Navbar />
            {children}
        </div>
    )
}

export default function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <ToastProvider>
                    <Routes>
                        {/* Public routes */}
                        <Route path="/login" element={<LoginPage />} />
                        <Route path="/register" element={<RegisterPage />} />

                        {/* Protected routes - Doctor Service*/}
                        <Route path="/dashboard" element={<ProtectedRoute><Layout><DashboardPage /></Layout></ProtectedRoute> }/>
                        <Route path="/doctors" element={<ProtectedRoute><Layout><DoctorsPage /></Layout></ProtectedRoute>} />
                        <Route path="/doctors/new" element={<ProtectedRoute roles={['ADMIN', 'RECEPTIONIST', 'DOCTOR']}><Layout><AddDoctorPage /></Layout></ProtectedRoute>} />
                        <Route path="/doctors/:id" element={<ProtectedRoute><Layout><DoctorDetailPage /></Layout></ProtectedRoute>} />
                        <Route path="/doctors/:id/slots/new" element={<ProtectedRoute roles={['ADMIN', 'RECEPTIONIST', 'DOCTOR']}><Layout><AddSlotPage /></Layout></ProtectedRoute>} />
                        <Route path="/my-schedule" element={<ProtectedRoute roles={['DOCTOR']}><Layout><MySchedulePage /></Layout></ProtectedRoute>} />
                        <Route path="/doctor-applications" element={<ProtectedRoute roles={['ADMIN', 'RECEPTIONIST']}><Layout><DoctorApplicationsPage /></Layout></ProtectedRoute>} />
                        <Route path="/appointments/new" element={<ProtectedRoute><Layout><BookingPage /></Layout></ProtectedRoute>} />

                        {/* Appointment Booking */}
                        <Route path="/appointments/book" element={<ProtectedRoute roles={['PATIENT']}><Layout><BookingPage /></Layout></ProtectedRoute>} />
                        <Route path="/patients/me" element={<ProtectedRoute><Layout><PatientProfilePage /></Layout></ProtectedRoute>} />
                        <Route path="/patients" element={<ProtectedRoute roles={['ADMIN', 'RECEPTIONIST']}><Layout><PatientsPage /></Layout></ProtectedRoute>} />

                        {/* Payment Routes */}
                        <Route path="/checkout" element={<ProtectedRoute roles={['PATIENT']}><Layout><CheckoutPage /></Layout></ProtectedRoute>} />
                        <Route path="/payment/success" element={<ProtectedRoute roles={['PATIENT']}><Layout><PaymentSuccessPage /></Layout></ProtectedRoute>} />
                        <Route path="/appointments/:appointmentId/payment-success" element={<ProtectedRoute roles={['PATIENT']}><Layout><PaymentSuccessPage /></Layout></ProtectedRoute>} />
                        <Route path="/payments" element={<ProtectedRoute roles={['PATIENT']}><Layout><PaymentsPage /></Layout></ProtectedRoute>} />

                        {/* Fallbacks */ }
                        <Route path="/" element={<Navigate to="/dashboard" replace />} />
                        <Route path="*" element={<Navigate to="/dashboard" replace />} />
                    </Routes>
                </ToastProvider>
            </AuthProvider>
        </BrowserRouter>
    )
}
