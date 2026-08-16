# API Gateway Service

This is a Spring Cloud Gateway that routes requests to the appropriate microservices in both local and production environments.

## 🚀 Deployment Modes

### **Local Development**
- **API Gateway**: http://localhost:8080
- **User Service**: http://localhost:8081  
- **Doctor Service**: http://localhost:8082

### **Production (Google Cloud Run)**
- **API Gateway**: https://api-gateway-xxx.run.app
- **User Service**: https://user-service-268672367192.us-central1.run.app
- **Doctor Service**: https://doctor-service-efc3c5f3xa-uc.a.run.app

## 🛣️ Routes Configuration
- `/api/auth/**` → User Service 
- `/api/users/**` → User Service 
- `/api/doctors/**` → Doctor Service  
- `/api/slots/**` → Doctor Service

## 🔧 Environment Configuration

### Local Development:
```bash
# Start with local profile (default)
./mvnw spring-boot:run

# Or explicitly set local profile
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

### Production Deployment:
```bash
# Deploy to Google Cloud Run
./deploy.sh

# Or manual deployment
SPRING_PROFILES_ACTIVE=production \
USER_SERVICE_URL=https://user-service-268672367192.us-central1.run.app \
DOCTOR_SERVICE_URL=https://doctor-service-efc3c5f3xa-uc.a.run.app \
./mvnw spring-boot:run
```

## 📋 Deployment Steps

### For Separate Cloud Deployment:

1. **Deploy Individual Services First:**
   ```bash
   # Deploy User Service
   cd ../userService && ./deploy.sh
   
   # Deploy Doctor Service  
   cd ../doctorService && ./deploy.sh
   ```

2. **Deploy API Gateway:**
   ```bash
   cd apiGateway
   ./deploy.sh
   ```

3. **Update Frontend to use API Gateway URL:**
   ```javascript
   // In frontend/src/api/index.js
   const API_BASE_URL = 'https://your-api-gateway-url.run.app'
   ```

## ⚙️ Configuration Options

### Environment Variables:
- `SPRING_PROFILES_ACTIVE`: `local` | `production`
- `USER_SERVICE_URL`: URL of deployed user service
- `DOCTOR_SERVICE_URL`: URL of deployed doctor service
- `PORT`: Server port (default: 8080)

### Health Checks:
- Gateway health: `/actuator/health`
- Gateway routes: `/actuator/gateway/routes`

## 🏗️ Architecture Benefits

✅ **Single API Endpoint** - Frontend calls one URL  
✅ **Service Independence** - Services can be deployed separately  
✅ **Environment Flexibility** - Same code works locally and in cloud  
✅ **CORS Centralized** - No CORS config needed in individual services  
✅ **Monitoring Ready** - Centralized logging and health checks