# Spring Boot ConfigMaps, Secrets & Volumes Demo

This project demonstrates:
- **ConfigMaps** for non-sensitive configuration
- **Secrets** for sensitive data (DB credentials)
- **PersistentVolumeClaims** for durable storage

## Project Structure
```
SpringBootConfigApp/
├── Dockerfile
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/example/demo/
│       │   └── ConfigDemoApplication.java
│       └── resources/
│           └── application.properties
└── kubernetes/
    ├── configmap.yaml
    ├── secret.yaml
    ├── pvc.yaml
    ├── deployment.yaml
    └── service.yaml
```

## API Endpoints

- `GET /` - Home page
- `GET /config` - Display injected configuration values
- `GET /health` - Health check
- `GET /write-log` - Write a log entry to persistent storage
- `GET /read-logs` - Read all logs from persistent storage

## Step-by-Step Deployment

### Prerequisites
- Docker should be running in the Machine - In our case its docker desktop App

### 1. Configure Minikube Docker Environment
```powershell
minikube docker-env | Invoke-Expression
```

### 2. Build Docker Image
```powershell
cd Day5-6-7\SpringBootConfigApp
docker build -t config-demo:1.0 .
```

### 3. Apply Kubernetes Resources
```powershell
# Create ConfigMap
kubectl apply -f kubernetes/configmap.yaml

# Create Secret
kubectl apply -f kubernetes/secret.yaml

# Create PersistentVolumeClaim
kubectl apply -f kubernetes/pvc.yaml

# Create Deployment
kubectl apply -f kubernetes/deployment.yaml

# Create Service
kubectl apply -f kubernetes/service.yaml
```

### 4. Verify Deployment
```powershell
kubectl get configmap
kubectl get secret
kubectl get pvc
kubectl get pods
kubectl get svc
```
#### Sapmle outputs for above comands 

NAME                                   READY   STATUS    RESTARTS   AGE
pod/config-demo-app-785fcd9c9b-7528b   1/1     Running   0          32s
pod/config-demo-app-785fcd9c9b-k4v4p   1/1     Running   0          32s

NAME                          TYPE           CLUSTER-IP   EXTERNAL-IP   PORT(S)        AGE
service/config-demo-service   LoadBalancer   10.107.3.6   <pending>     80:30493/TCP   32s
service/kubernetes            ClusterIP      10.96.0.1    <none>        443/TCP        14m

NAME                              READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/config-demo-app   2/2     2            2           32s

NAME                                         DESIRED   CURRENT   READY   AGE
replicaset.apps/config-demo-app-785fcd9c9b   2         2         2       32s

NAME                         DATA   AGE
configmap/app-config         2      32s
configmap/kube-root-ca.crt   1      14m

NAME                TYPE     DATA   AGE
secret/app-secret   Opaque   2      32s

NAME                                 STATUS   VOLUME                                     CAPACITY   ACCESS MODES STORAGECLASS   VOLUMEATTRIBUTESCLASS   AGE
persistentvolumeclaim/app-logs-pvc   Bound    pvc-69cd8a1c-710b-4fe1-9d7d-39aae9dc852f   1Gi        RWO            standard       <unset>                 32s


### 5. Start Minikube Tunnel (in separate terminal)
```powershell
minikube tunnel
```

### 6. Test the Application
```powershell
# Home page
curl http://localhost/
    --Spamle output - Spring Boot ConfigMap, Secrets & Volumes Demo!

# View configuration (ConfigMap + Secret values)
curl http://localhost/config
    --Spamle output - Application Name: DemoApp Log Level: INFO DB User: spring DB Password: ***ass

# Health check
curl http://localhost/health
    --Spamle output - Service is running!

# Write log to persistent volume
curl http://localhost/write-log
    --Spamle output - Log written successfully to /app/logs/app.log

# Read logs from persistent volume
curl http://localhost/read-logs
    --Spamle output - === Log File Contents === 2025-11-27T12:56:47.358973033 - Log entry from DemoApp

```

## ConfigMap Details
```yaml
APP_NAME: "DemoApp"
LOG_LEVEL: "INFO"
```

## Secret Details (Base64 Encoded)
```yaml
DB_USER: c3ByaW5n  # "spring"
DB_PASS: cGFzcw==  # "pass"
```

## Volume Mount
- **Path**: `/app/logs`
- **Type**: PersistentVolumeClaim
- **Size**: 1Gi
- **Access Mode**: ReadWriteOnce

## Teaching Points

### ConfigMaps
- Store non-sensitive configuration
- Injected via `configMapKeyRef`
- Can be updated without rebuilding the image

### Secrets
- Store sensitive data (base64 encoded)
- Injected via `secretKeyRef`
- More secure than hardcoding credentials

### Volumes
- PersistentVolumeClaim provides durable storage
- Data persists even when pods restart
- Mounted at `/app/logs` in the container

## Cleanup
```powershell
kubectl delete -f kubernetes/
```
