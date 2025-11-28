# Day 8: Health Checks - Liveness & Readiness Probes

This project demonstrates **Kubernetes Health Probes** with Spring Boot Actuator:
- **Liveness Probe** - Determines if the application should be restarted
- **Readiness Probe** - Determines if the application can receive traffic
- **Startup Probe** - Handles slow-starting applications

## Project Structure
```
HealthProbesApp/
├── Dockerfile
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/example/healthprobes/
│       │   ├── HealthProbesApplication.java
│       │   ├── DatabaseHealthIndicator.java
│       │   └── ExternalServiceHealthIndicator.java
│       └── resources/
│           └── application.properties
└── kubernetes/
    ├── deployment.yaml
    └── service.yaml
```

## Health Check Endpoints

### Spring Boot Actuator Endpoints
- `/actuator/health` - Overall health status
- `/actuator/health/liveness` - Liveness state (for Kubernetes)
- `/actuator/health/readiness` - Readiness state (for Kubernetes)
- `/actuator/info` - Application information

### Application Endpoints
- `GET /` - Home page
- `GET /info` - Application info with current state
- `GET /status` - Current health status
- `GET /simulate/notready` - Mark app as NOT READY (readiness fails)
- `GET /simulate/ready` - Restore readiness
- `GET /simulate/broken` - Mark app as BROKEN (liveness fails, triggers restart)
- `GET /simulate/slowstart` - Simulate 45-second startup delay

## Understanding the Probes

### 1. Liveness Probe
**Purpose**: Detect if application is in a deadlock or unrecoverable state

**Configuration**:
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3
```

**Behavior**:
- ✅ **Passes**: Application is running normally
- ❌ **Fails**: Kubernetes **RESTARTS** the pod
- Use for: Deadlocks, memory leaks, unresponsive app

### 2. Readiness Probe
**Purpose**: Determine if application can handle requests

**Configuration**:
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 3
```

**Behavior**:
- ✅ **Passes**: Pod receives traffic from service
- ❌ **Fails**: Kubernetes **STOPS** sending traffic (removes from endpoints)
- Use for: Database connection issues, dependency failures, temporary overload

### 3. Startup Probe
**Purpose**: Give slow-starting applications more time before liveness/readiness checks

**Configuration**:
```yaml
startupProbe:
  httpGet:
    path: /actuator/health/liveness
  initialDelaySeconds: 0
  periodSeconds: 10
  failureThreshold: 30  # Allows 300s for startup
```

**Behavior**:
- Disables liveness and readiness probes until startup succeeds
- Prevents premature restarts during slow initialization
- Use for: Large data loading, cache warming, slow dependencies

## Step-by-Step Deployment

### Prerequisites
- Docker Desktop running
- Minikube running (`minikube start`)

### 1. Configure Minikube Docker
```powershell
minikube docker-env | Invoke-Expression
```

### 2. Build Docker Image
```powershell
cd Day8\HealthProbesApp
docker build -t health-probes:1.0 .
```

### 3. Deploy to Kubernetes
```powershell
kubectl apply -f kubernetes/
```

### 4. Verify Deployment
```powershell
kubectl get pods
    NAME                                 READY   STATUS              RESTARTS   AGE
    health-probes-app-57789769dd-5dqrl   0/1     ErrImageNeverPull   0          2m22s
    health-probes-app-57789769dd-nwnxg   0/1     ErrImageNeverPull   0          2m22s
kubectl get svc
    NAME                    TYPE           CLUSTER-IP       EXTERNAL-IP   PORT(S)        AGE
    health-probes-service   LoadBalancer   10.110.223.189   <pending>     80:30568/TCP   2m52s
    kubernetes              ClusterIP      10.96.0.1        <none>        443/TCP        21h
kubectl describe pod health-probes-app-57789769dd-5dqrl
```

### 5. Start Minikube Tunnel
In a separate terminal with admin privileges:
```powershell
minikube tunnel
```

### 6. Test the Application
```powershell
# Home page
curl http://localhost/
    Health Probes Demo - Liveness & Readiness Checks!

# Check health endpoints
curl http://localhost/actuator/health
    {"status":"DOWN","components":{"database":{"status":"DOWN","details":{"database":"Connection failed","error":"Unable to reach database"}},"diskSpace":{"status":"UP","details":{"total":1081101176832,"free":1019449110528,"threshold":10485760,"path":"/app/.","exists":true}},"externalService":{"status":"UP","details":{"externalService":"Available","responseTime":"120ms"}},"livenessState":{"status":"UP"},"ping":{"status":"UP"},"readinessState":{"status":"UP"}},"groups":["liveness","readiness"]}

curl http://localhost/actuator/health/liveness
    {"status":"UP"}

curl http://localhost/actuator/health/readiness
    {"status":"UP"}

# Application info
curl http://localhost/info
    {"application":"Health Probes Demo","ready":true,"version":"1.0.0","live":true,"timestamp":"2025-11-28T11:08:58.531540055"}
curl http://localhost/status
    {"readiness":"READY","liveness":"ALIVE","message":"Use /actuator/health/liveness and /actuator/health/readiness for K8s probes"}
```

## Testing Health Probes

### Test 1: Readiness Probe Failure
```powershell
# Mark application as not ready
curl http://localhost/simulate/notready

# Check pod status - should show 1/1 but not receive traffic
kubectl get pods

# Check endpoints - pod should be removed
kubectl get endpoints health-probes-service

# Restore readiness
curl http://localhost/simulate/ready
```

**Expected Result**:
- Pod stays running (1/1 READY)
- Service stops routing traffic to the pod
- Other replicas continue serving traffic

### Test 2: Liveness Probe Failure
```powershell
# Mark application as broken
curl http://localhost/simulate/broken

# Watch pod get restarted
kubectl get pods -w

# Check restart count
kubectl get pods
```

**Expected Result**:
- Liveness probe fails after 3 attempts (30 seconds)
- Kubernetes **restarts** the container
- RESTARTS count increases by 1

### Test 3: Slow Startup
```powershell
# Trigger slow startup endpoint
curl http://localhost/simulate/slowstart

# In another terminal, watch the pod
kubectl get pods -w
```

**Expected Result**:
- Startup probe allows up to 300 seconds
- Liveness/readiness checks are disabled during startup
- Pod doesn't get restarted prematurely

## Monitoring Health Status

### View Pod Events
```powershell
kubectl describe pod <pod-name>
```
Look for events like:
- `Liveness probe failed`
- `Readiness probe failed`
- `Container restarted`

### View Pod Logs
```powershell
kubectl logs <pod-name>
kubectl logs <pod-name> --previous  # Previous container (after restart)
```

### Watch Pods in Real-Time
```powershell
kubectl get pods -w
```

### Check Probe Configuration
```powershell
kubectl get pod <pod-name> -o yaml
```

## Probe Configuration Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `initialDelaySeconds` | Wait before first probe | 0 |
| `periodSeconds` | How often to probe | 10 |
| `timeoutSeconds` | Probe timeout | 1 |
| `successThreshold` | Consecutive successes to pass | 1 |
| `failureThreshold` | Consecutive failures to fail | 3 |

## Best Practices

### Liveness Probe
✅ **Do**:
- Check if app is fundamentally broken (not just slow)
- Use simple, lightweight checks
- Set reasonable `initialDelaySeconds` (allow app to start)

❌ **Don't**:
- Check external dependencies (use readiness instead)
- Make it too sensitive (avoid false restarts)
- Use the same endpoint as readiness

### Readiness Probe
✅ **Do**:
- Check all critical dependencies (DB, cache, external APIs)
- Use more frequently than liveness (`periodSeconds: 5`)
- Return quickly (< 1 second)

❌ **Don't**:
- Leave it out (traffic goes to unavailable pods)
- Make it too strict (don't fail for non-critical issues)

### Startup Probe
✅ **Do**:
- Use for slow-starting applications
- Set high `failureThreshold` (allow enough time)
- Disable it for fast-starting apps

## Custom Health Indicators

The application includes two custom health indicators:

### DatabaseHealthIndicator
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        boolean dbAvailable = checkDatabaseConnection();
        return dbAvailable ? Health.up() : Health.down();
    }
}
```

### ExternalServiceHealthIndicator
```java
@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        boolean serviceAvailable = checkExternalService();
        return serviceAvailable ? Health.up() : Health.down();
    }
}
```

These appear in the `/actuator/health` endpoint response.

## Common Issues & Solutions

### Issue: Pod keeps restarting
**Cause**: Liveness probe failing
**Solution**: 
- Increase `initialDelaySeconds`
- Increase `failureThreshold`
- Check app logs: `kubectl logs <pod-name>`

### Issue: No traffic reaching pod
**Cause**: Readiness probe failing
**Solution**:
- Check dependencies (DB, external services)
- View health endpoint: `curl http://localhost/actuator/health`
- Check endpoints: `kubectl get endpoints`

### Issue: Slow startup causing restarts
**Cause**: Liveness probe starts before app is ready
**Solution**:
- Add/configure startup probe
- Increase liveness `initialDelaySeconds`

## Cleanup
```powershell
kubectl delete -f kubernetes/
```

## Key Takeaways

1. **Liveness Probe** = "Is the app alive?" → Restart if fails
2. **Readiness Probe** = "Can it serve traffic?" → Stop traffic if fails
3. **Startup Probe** = "Is it done starting?" → Protect during startup
4. Use Spring Boot Actuator for easy health endpoint implementation
5. Custom health indicators check specific dependencies
6. Tune probe parameters based on your application's characteristics


