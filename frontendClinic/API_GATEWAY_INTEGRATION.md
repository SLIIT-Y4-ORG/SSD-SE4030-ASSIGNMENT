# Frontend API Gateway Integration

## Changes Made

The frontend has been successfully updated to use the API Gateway instead of direct service calls.

### Previous Configuration
- **User Service**: `https://user-service-268672367192.us-central1.run.app`
- **Doctor Service**: `https://doctor-service-efc3c5f3xa-uc.a.run.app`

### New Configuration
- **API Gateway**: `https://api-gateway-268672367192.europe-west1.run.app`

## Updated Files

### `/frontend/src/api/index.js`
- Replaced direct service URLs with single API Gateway URL
- Updated all API endpoints to route through the gateway:
  - Authentication endpoints (`/api/auth/*`)
  - Doctor management endpoints (`/api/doctors/*`)
  - Slot management endpoints (`/api/slots/*`)

### Affected Endpoints
1. **Auth Service Endpoints**:
   - `POST /api/auth/register`
   - `POST /api/auth/login`
   - `POST /api/auth/logout`
   - `POST /api/auth/refresh`
   - `GET /api/auth/me`

2. **Doctor Service Endpoints**:
   - `GET /api/doctors`
   - `GET /api/doctors/{id}`
   - `POST /api/doctors`
   - `PUT /api/doctors/{id}`
   - `PATCH /api/doctors/{id}/verify`
   - `PATCH /api/doctors/{id}/link-user`

3. **Slot Service Endpoints**:
   - `GET /api/doctors/{id}/slots`
   - `POST /api/doctors/{id}/slots`
   - `POST /api/slots/{id}/reserve`
   - `POST /api/slots/{id}/release`

## API Gateway Routing

The API Gateway automatically routes requests based on path patterns:
- `/api/auth/**` and `/api/users/**` → User Service
- `/api/doctors/**` and `/api/slots/**` → Doctor Service

## Benefits

1. **Single Entry Point**: All API calls go through one gateway URL
2. **Centralized Configuration**: CORS, authentication, and routing managed in one place
3. **Service Discovery**: Frontend doesn't need to know individual service URLs
4. **Load Balancing**: Gateway can distribute load across service instances
5. **Monitoring**: Centralized logging and monitoring of all API requests
6. **Security**: Single point for implementing security policies

## Deployment Status

- ✅ API Gateway deployed at: `https://api-gateway-268672367192.europe-west1.run.app`
- ✅ Frontend updated to use API Gateway
- ✅ Frontend rebuilt and ready for deployment

## Testing

To test the integration:
1. Deploy the updated frontend
2. Verify all API calls go through the gateway
3. Check browser network tab to confirm URLs use the gateway endpoint
4. Test all functionality (login, doctor management, slot booking)