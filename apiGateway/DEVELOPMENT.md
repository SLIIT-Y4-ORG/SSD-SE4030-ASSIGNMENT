# API Gateway Development Setup

## Update Frontend Configuration

To use the API Gateway in development, update your `frontend/src/api/index.js`:

```javascript
// For development with API Gateway
const API_BASE_URL = 'http://localhost:8080'

// For production (keep your existing URLs)
// const USER_SERVICE = 'https://user-service-268672367192.us-central1.run.app'
// const DOCTOR_SERVICE = 'https://doctor-service-efc3c5f3xa-uc.a.run.app'

// Replace all service calls to use single base URL:
export async function login(data) {
    const res = await axios.post(`${API_BASE_URL}/api/auth/login`, data)
    return res.data
}

export async function getDoctors() {
    const res = await axios.get(`${API_BASE_URL}/api/doctors`, { headers: authHeaders() })
    return res.data
}
```

## Start Services in Order:

1. **User Service**
   ```bash
   cd userService
   ./mvnw spring-boot:run
   # Runs on http://localhost:8081
   ```

2. **Doctor Service**  
   ```bash
   cd doctorService
   ./mvnw spring-boot:run
   # Runs on http://localhost:8082
   ```

3. **API Gateway**
   ```bash
   cd apiGateway
   ./mvnw spring-boot:run
   # Runs on http://localhost:8080
   ```

4. **Frontend**
   ```bash
   cd frontend
   npm run dev
   # Calls API Gateway at http://localhost:8080
   ```

## Request Flow:
```
Frontend (React) 
    ↓ 
API Gateway (8080)
    ↓ 
User Service (8081) OR Doctor Service (8082)
```

## Benefits You Get:
- ✅ Single API endpoint for frontend
- ✅ CORS handling centralized  
- ✅ Request logging and monitoring
- ✅ Easy to add authentication middleware later
- ✅ Load balancing ready for production