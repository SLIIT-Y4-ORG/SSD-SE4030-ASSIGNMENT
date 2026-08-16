# GitHub + Cloud Run Deployment Guide

## 🚀 **Easy GitHub Integration Deployment**

### **Step 1: Push API Gateway to GitHub**
```bash
cd apiGateway
git add .
git commit -m "Add API Gateway service"
git push origin main
```

### **Step 2: Deploy via Cloud Run Console**

1. **Go to Cloud Run Console:**
   - Visit: https://console.cloud.google.com/run
   - Select your project

2. **Create New Service:**
   - Click "CREATE SERVICE"
   - Choose "Continuously deploy new revisions from a source repository"
   - Click "SET UP WITH CLOUD BUILD"

3. **Connect Repository:**
   - Choose GitHub as source
   - Authenticate and select your repository
   - Branch: `main`
   - Build Type: `Dockerfile`
   - Source location: `/apiGateway/Dockerfile`

4. **Configure Service:**
   - Service name: `api-gateway`
   - Region: `us-central1` (same as your other services)
   - CPU allocation: Container instances only
   - Ingress: All traffic
   - Authentication: Allow unauthenticated invocations

5. **Set Environment Variables:**
   ```
   SPRING_PROFILES_ACTIVE=production
   USER_SERVICE_URL=https://user-service-268672367192.us-central1.run.app
   DOCTOR_SERVICE_URL=https://doctor-service-efc3c5f3xa-uc.a.run.app
   PORT=8080
   ```

### **Step 3: Automatic Deployments** ✨
- Every push to `main` branch automatically triggers new deployment
- No terminal commands needed!
- Cloud Build handles everything

## 🔄 **Deployment Flow:**
```
GitHub Push → Cloud Build → Container Build → Cloud Run Deploy
```

## ⚙️ **Alternative: Cloud Build Configuration**

Create `apiGateway/cloudbuild.yaml`:
```yaml
steps:
  - name: 'gcr.io/cloud-builders/docker'
    args: ['build', '-t', 'gcr.io/$PROJECT_ID/api-gateway', '.']
  - name: 'gcr.io/cloud-builders/docker'  
    args: ['push', 'gcr.io/$PROJECT_ID/api-gateway']
  - name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
    entrypoint: 'gcloud'
    args: [
      'run', 'deploy', 'api-gateway',
      '--image=gcr.io/$PROJECT_ID/api-gateway',
      '--platform=managed',
      '--region=us-central1',
      '--allow-unauthenticated',
      '--set-env-vars=SPRING_PROFILES_ACTIVE=production,USER_SERVICE_URL=https://user-service-268672367192.us-central1.run.app,DOCTOR_SERVICE_URL=https://doctor-service-efc3c5f3xa-uc.a.run.app'
    ]
```

## 🎯 **Benefits of GitHub Integration:**

✅ **Automatic deployments** on every push  
✅ **No terminal commands** needed  
✅ **Build history** and rollback options  
✅ **Team collaboration** - anyone can deploy by pushing  
✅ **CI/CD pipeline** built-in  

## 🔧 **After Deployment:**

1. **Get API Gateway URL:**
   - Cloud Run console will show the service URL
   - Example: `https://api-gateway-xxx-uc.a.run.app`

2. **Update Frontend:**
   ```javascript
   // frontend/src/api/index.js
   const API_BASE_URL = 'https://api-gateway-xxx-uc.a.run.app'
   ```

3. **Test Routes:**
   - `https://api-gateway-xxx.run.app/api/auth/validate`
   - `https://api-gateway-xxx.run.app/api/doctors`

This approach is **much simpler** and more professional than terminal deployments!