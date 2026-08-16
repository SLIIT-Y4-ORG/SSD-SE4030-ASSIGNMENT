import { useEffect, useMemo, useState } from 'react'
import { createPatientProfile, getMyPatientProfile, updatePatientProfile } from '../../api'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../components/Toast'

function toDateInput(value) {
    if (!value) return ''
    return String(value).split('T')[0]
}

export default function PatientProfilePage() {
    const { user } = useAuth()
    const toast = useToast()

    const [profileId, setProfileId] = useState(null)
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState('')

    const [form, setForm] = useState({
        name: '',
        email: '',
        phone: '',
        dateOfBirth: '',
        gender: '',
        address: '',
        bloodGroup: '',
        allergies: '',
    })

    const isCreateMode = useMemo(() => !profileId, [profileId])

    useEffect(() => {
        setLoading(true)
        setError('')

        getMyPatientProfile()
            .then(data => {
                setProfileId(data.id)
                setForm({
                    name: data.name || '',
                    email: data.email || '',
                    phone: data.phone || '',
                    dateOfBirth: toDateInput(data.dateOfBirth),
                    gender: data.gender || '',
                    address: data.address || '',
                    bloodGroup: data.bloodGroup || '',
                    allergies: data.allergies || '',
                })
            })
            .catch(err => {
                if (err?.response?.status === 404) {
                    // No profile yet: initialize with auth user data.
                    setProfileId(null)
                    setForm(prev => ({
                        ...prev,
                        name: user?.name || '',
                        email: user?.email || '',
                    }))
                    return
                }
                setError(err?.response?.data?.error || 'Failed to load patient profile.')
            })
            .finally(() => setLoading(false))
    }, [user?.name, user?.email])

    const handleChange = e => {
        const { name, value } = e.target
        setForm(prev => ({ ...prev, [name]: value }))
    }

    const handleSubmit = async e => {
        e.preventDefault()
        setSaving(true)
        setError('')

        const payload = {
            name: form.name,
            email: form.email,
            phone: form.phone,
            dateOfBirth: form.dateOfBirth || null,
            gender: form.gender || null,
            address: form.address || null,
            bloodGroup: form.bloodGroup || null,
            allergies: form.allergies || null,
        }

        try {
            if (isCreateMode) {
                const created = await createPatientProfile(payload)
                setProfileId(created.id)
                toast('Patient profile created successfully.', 'success')
            } else {
                await updatePatientProfile(profileId, payload)
                toast('Patient profile updated successfully.', 'success')
            }
        } catch (err) {
            const msg = err?.response?.data?.error || 'Failed to save patient profile.'
            setError(msg)
            toast(msg, 'error')
        } finally {
            setSaving(false)
        }
    }

    if (loading) {
        return (
            <div className="main-content">
                <div className="spinner-wrapper">
                    <div className="spinner" />
                    <span className="spinner-text">Loading patient profile...</span>
                </div>
            </div>
        )
    }

    return (
        <div className="main-content fade-in">
            <div className="page-header">
                <div>
                    <h1 className="page-title">My Patient Profile</h1>
                    <p className="page-subtitle">
                        {isCreateMode ? 'Create your patient record for faster clinic service.' : 'Keep your patient information up to date.'}
                    </p>
                </div>
                <span className={`badge ${isCreateMode ? 'badge-yellow' : 'badge-green'}`}>
                    {isCreateMode ? 'Profile Not Created' : 'Profile Active'}
                </span>
            </div>

            {error && <div className="alert alert-error">{error}</div>}

            <div className="card" style={{ maxWidth: '900px' }}>
                <form onSubmit={handleSubmit}>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
                        <div className="form-group">
                            <label htmlFor="name">Full Name</label>
                            <input id="name" name="name" value={form.name} onChange={handleChange} required />
                        </div>

                        <div className="form-group">
                            <label htmlFor="email">Email</label>
                            <input id="email" name="email" type="email" value={form.email} onChange={handleChange} required disabled={!isCreateMode} />
                        </div>

                        <div className="form-group">
                            <label htmlFor="phone">Phone</label>
                            <input id="phone" name="phone" value={form.phone} onChange={handleChange} placeholder="+94771234567" />
                        </div>

                        <div className="form-group">
                            <label htmlFor="dateOfBirth">Date of Birth</label>
                            <input id="dateOfBirth" name="dateOfBirth" type="date" value={form.dateOfBirth} onChange={handleChange} />
                        </div>

                        <div className="form-group">
                            <label htmlFor="gender">Gender</label>
                            <select id="gender" name="gender" value={form.gender} onChange={handleChange}>
                                <option value="">Select</option>
                                <option value="MALE">Male</option>
                                <option value="FEMALE">Female</option>
                                <option value="OTHER">Other</option>
                            </select>
                        </div>

                        <div className="form-group">
                            <label htmlFor="bloodGroup">Blood Group</label>
                            <select id="bloodGroup" name="bloodGroup" value={form.bloodGroup} onChange={handleChange}>
                                <option value="">Select</option>
                                <option value="A+">A+</option>
                                <option value="A-">A-</option>
                                <option value="B+">B+</option>
                                <option value="B-">B-</option>
                                <option value="AB+">AB+</option>
                                <option value="AB-">AB-</option>
                                <option value="O+">O+</option>
                                <option value="O-">O-</option>
                            </select>
                        </div>
                    </div>

                    <div className="form-group">
                        <label htmlFor="address">Address</label>
                        <input id="address" name="address" value={form.address} onChange={handleChange} placeholder="Street, city, postal code" />
                    </div>

                    <div className="form-group">
                        <label htmlFor="allergies">Allergies</label>
                        <input id="allergies" name="allergies" value={form.allergies} onChange={handleChange} placeholder="e.g. Penicillin, peanuts" />
                    </div>

                    <button type="submit" className="btn btn-primary" disabled={saving}>
                        {saving ? 'Saving...' : isCreateMode ? 'Create Profile' : 'Update Profile'}
                    </button>
                </form>
            </div>
        </div>
    )
}