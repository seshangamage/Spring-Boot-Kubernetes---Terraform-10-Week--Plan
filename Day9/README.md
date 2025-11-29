# Day 9: Resource Requests/Limits, QoS & Scheduling

This project demonstrates **Kubernetes Resource Management**:
- **Resource Requests** - Guaranteed minimum resources
- **Resource Limits** - Maximum allowed resources  
- **QoS Classes** - Quality of Service (Guaranteed, Burstable, BestEffort)
- **Scheduling** - Node affinity, pod affinity/anti-affinity

## Project Structure
```
ResourceLimitsApp/
├── Dockerfile
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/example/resources/
│       │   └── ResourceLimitsApplication.java
│       └── resources/
│           └── application.properties
└── kubernetes/
    ├── deployment-guaranteed.yaml
    ├── deployment-burstable.yaml
    ├── deployment-besteffort.yaml
    ├── deployment-scheduled.yaml
    └── service.yaml
```

## Application Endpoints

### Resource Information
- `GET /` - Home page
- `GET /resources` - View current CPU/memory usage
- `GET /health` - Health check

### Resource Testing
- `GET /cpu-load?seconds=5` - Simulate CPU load for N seconds
- `GET /allocate-memory?megabytes=10` - Allocate N MB of memory
- `GET /free-memory` - Free all allocated memory
- `GET /memory-leak?iterations=5` - Simulate memory leak

## Understanding Resource Management

### Resource Requests
**Guaranteed minimum** resources for the container

```yaml
resources:
  requests:
    memory: "256Mi"    # Minimum 256 MB memory
    cpu: "250m"        # Minimum 0.25 CPU cores
```

**What it means**:
- Kubernetes reserves this amount on the node
- Pod won't be scheduled if node doesn't have enough
- Container is guaranteed these resources

### Resource Limits
**Maximum allowed** resources for the container

```yaml
resources:
  limits:
    memory: "512Mi"    # Maximum 512 MB memory
    cpu: "500m"        # Maximum 0.5 CPU cores
```

**What it means**:
- Container cannot exceed these limits
- **CPU**: Throttled if exceeded (slows down)
- **Memory**: Killed (OOMKilled) if exceeded

### CPU Units
- `1000m` = 1 CPU core
- `500m` = 0.5 CPU core
- `250m` = 0.25 CPU core
- `100m` = 0.1 CPU core

### Memory Units
- `128Mi` = 128 Mebibytes
- `256Mi` = 256 Mebibytes
- `1Gi` = 1 Gibibyte
- `1G` = 1 Gigabyte

## Quality of Service (QoS) Classes

### 1. Guaranteed QoS
**Highest priority** - Last to be evicted

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "256Mi"    # Same as requests
    cpu: "250m"        # Same as requests
```

**Characteristics**:
- Requests = Limits for all containers
- Most predictable performance
- Last to be killed under memory pressure
- **Use for**: Critical production workloads

**Deploy**:
```powershell
kubectl apply -f kubernetes/deployment-guaranteed.yaml
```

### 2. Burstable QoS
**Medium priority** - Can use extra resources when available

```yaml
resources:
  requests:
    memory: "128Mi"
    cpu: "100m"
  limits:
    memory: "512Mi"    # Higher than requests
    cpu: "500m"        # Higher than requests
```

**Characteristics**:
- Requests < Limits
- Guaranteed minimum (requests)
- Can burst up to limits when resources available
- **Use for**: Most applications, variable workloads

**Deploy**:
```powershell
kubectl apply -f kubernetes/deployment-burstable.yaml
```

### 3. BestEffort QoS
**Lowest priority** - First to be evicted

```yaml
# No resources specified
resources: {}
```

**Characteristics**:
- No requests or limits
- Can use any available resources
- First to be killed under memory pressure
- **Use for**: Non-critical batch jobs, development

**Deploy**:
```powershell
kubectl apply -f kubernetes/deployment-besteffort.yaml
```

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
cd Day9\ResourceLimitsApp
docker build -t resource-limits:1.0 .
```

### 3. Deploy All QoS Classes
```powershell
# Deploy all deployments
kubectl apply -f kubernetes/

# Or deploy individually
kubectl apply -f kubernetes/deployment-guaranteed.yaml
kubectl apply -f kubernetes/deployment-burstable.yaml
kubectl apply -f kubernetes/deployment-besteffort.yaml
kubectl apply -f kubernetes/service.yaml
```

### 4. Verify Deployments
```powershell
kubectl get pods
kubectl get deployments
```

### 5. Check QoS Classes
```powershell
# Check QoS class assigned to each pod
kubectl get pods -o custom-columns=NAME:.metadata.name,QOS:.status.qosClass

# Describe pod to see detailed resource info
kubectl describe pod <pod-name>
```

### 6. Start Minikube Tunnel
In a separate terminal with admin privileges:
```powershell
minikube tunnel
```

### 7. Test the Application
```powershell
# Check resources
curl.exe http://localhost/resources
    {"availableProcessors":1,"memory":{"usedMemoryMB":14,"totalMemoryMB":32,"maxMemoryMB":123,"heapMaxMB":123,"freeMemoryMB":18,"heapUsedMB":14},"containerLimits":{"note":"Use kubectl to see actual resource limits"}}

# CPU load test
curl.exe "http://localhost/cpu-load?seconds=10"
    {"result":1.194392538570641E9,"requestedSeconds":10,"message":"CPU load simulation completed","durationMs":10000}

# Memory allocation test
curl.exe "http://localhost/allocate-memory?megabytes=50"
    {"usedMemoryMB":61,"maxMemoryMB":123,"allocatedMB":50,"totalAllocatedMB":50,"message":"Memory allocated successfully"}

# Free memory
curl.exe http://localhost/free-memory
    {"usedMemoryMB":12,"message":"Memory freed","freedMB":50}
```

## Testing Resource Limits

### Test 1: CPU Throttling
```powershell
# Generate CPU load on Guaranteed pod (250m limit)
curl.exe "http://localhost/cpu-load?seconds=30"

# Enable Metrics server
minikube addons enable metrics-server

# Monitor CPU usage
kubectl top pods
    NAME                                       CPU(cores)   MEMORY(bytes)
    resource-app-guaranteed-7b76b96b89-fxvnd   247m         67Mi
```

**Expected Result**:
- CPU usage capped at 250m (0.25 cores)
- Application runs slower but stays running

### Test 2: Memory Limit (OOMKill)
```powershell
# Try to allocate more than limit
# Guaranteed: 256Mi limit
curl.exe "http://localhost/allocate-memory?megabytes=300"

# Watch pod get killed
kubectl get pods -w

    NAME                                       READY   STATUS    RESTARTS        AGE
    health-probes-app-774d5bb786-m4tl6         1/1     Running   1 (14m ago)     26h
    health-probes-app-774d5bb786-svc5x         1/1     Running   1 (14m ago)     26h
    resource-app-besteffort-846f87df5-2gdcl    1/1     Running   1 (5m21s ago)   6m22s
    resource-app-burstable-745984d669-84wb5    1/1     Running   0               6m22s
    resource-app-guaranteed-7b76b96b89-fxvnd   0/1     Running   5 (3s ago)      6m22s
    resource-app-scheduled-5596764b4-87msn     1/1     Running   0               6m22s

```

**Expected Result**:
- Pod exceeds memory limit
- Kubernetes kills the pod (OOMKilled)
- Pod restarts automatically

### Test 3: Burstable Behavior
```powershell
# Deploy burstable pod (128Mi request, 512Mi limit)
kubectl apply -f kubernetes/deployment-burstable.yaml

# Allocate within request (guaranteed)
curl.exe "http://localhost/allocate-memory?megabytes=100"

# Allocate more (burst up to limit)
curl.exe "http://localhost/allocate-memory?megabytes=200"

# Try to exceed limit
curl.exe "http://localhost/allocate-memory?megabytes=600"
```

**Expected Result**:
- 100Mi: Works (within request)
- 200Mi: Works if resources available (bursting)
- 600Mi: Pod gets OOMKilled (exceeds limit)

### Test 4: Pod Eviction Priority
```powershell
# Create memory pressure on node
# BestEffort pods killed first, then Burstable, then Guaranteed

# Deploy all three QoS classes
kubectl apply -f kubernetes/

# Create memory leak on all pods
curl.exe "http://localhost/memory-leak?iterations=100"

# Watch which pods get evicted first
kubectl get pods -w
```

**Expected Order**:
1. BestEffort pods evicted first
2. Burstable pods evicted next
3. Guaranteed pods evicted last (if at all)

## Node Affinity & Scheduling

### Node Affinity
Schedule pods on specific nodes based on labels

```yaml
affinity:
  nodeAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
      nodeSelectorTerms:
        - matchExpressions:
            - key: disktype
              operator: In
              values:
                - ssd
```

**Deploy**:
```powershell
kubectl apply -f kubernetes/deployment-scheduled.yaml
```

### Pod Anti-Affinity
Spread pods across nodes

```yaml
podAntiAffinity:
  preferredDuringSchedulingIgnoredDuringExecution:
    - weight: 100
      podAffinityTerm:
        labelSelector:
          matchExpressions:
            - key: app
              operator: In
              values:
                - resource-app
        topologyKey: kubernetes.io/hostname
```

### Label Nodes
```powershell
# Add label to node
kubectl label nodes <node-name> disktype=ssd

# View node labels
kubectl get nodes --show-labels

# Remove label
kubectl label nodes <node-name> disktype-
```

## Monitoring Resources

### View Pod Resource Usage
```powershell
# Enable metrics-server (if not enabled)
minikube addons enable metrics-server

# View current resource usage
kubectl top pods

# View node resource usage
kubectl top nodes
```

### Describe Pod Resources
```powershell
kubectl describe pod <pod-name>
```

Look for:
- `Requests` - Guaranteed resources
- `Limits` - Maximum allowed
- `QoS Class` - Guaranteed/Burstable/BestEffort

### View Events
```powershell
kubectl get events --sort-by='.lastTimestamp'
```

Look for:
- `OOMKilled` - Memory limit exceeded
- `FailedScheduling` - Not enough resources
- `Evicted` - Pod evicted due to resource pressure

## Best Practices

### Resource Requests
✅ **Do**:
- Always set requests for production workloads
- Base on actual application needs (test and measure)
- Set requests = average usage

❌ **Don't**:
- Set requests too high (wastes resources)
- Leave requests empty for critical apps

### Resource Limits
✅ **Do**:
- Set limits for memory (prevent OOM)
- Set limits higher than requests for burstable workloads
- Monitor and adjust based on actual usage

❌ **Don't**:
- Set CPU limits too low (causes throttling)
- Set memory limits too low (causes OOMKills)
- Leave limits unset for untrusted workloads

### QoS Class Selection
- **Guaranteed**: Critical services, databases, stateful apps
- **Burstable**: Most applications, web services
- **BestEffort**: Development, batch jobs, non-critical tasks

## Common Issues & Solutions

### Issue: Pod stuck in Pending
**Cause**: Not enough resources on any node
**Solution**:
```powershell
kubectl describe pod <pod-name>
# Look for "Insufficient cpu" or "Insufficient memory"

# Reduce requests or add more nodes
```

### Issue: Pod getting OOMKilled
**Cause**: Memory limit exceeded
**Solution**:
```powershell
# Check memory usage
kubectl top pod <pod-name>

# Increase memory limit
# Or fix memory leak in application
```

### Issue: Application running slow
**Cause**: CPU throttling (limit exceeded)
**Solution**:
```powershell
# Check CPU usage
kubectl top pod <pod-name>

# Increase CPU limit or optimize application
```

### Issue: Pods not spreading across nodes
**Cause**: No anti-affinity configured
**Solution**:
- Add pod anti-affinity rules
- Use topology spread constraints

## Cleanup
```powershell
kubectl delete -f kubernetes/
```

## Key Takeaways

1. **Always set resource requests** for production workloads
2. **Set memory limits** to prevent runaway processes
3. **Be careful with CPU limits** - can cause unnecessary throttling
4. **Guaranteed QoS** = requests = limits (most predictable)
5. **Burstable QoS** = requests < limits (most flexible)
6. **BestEffort QoS** = no resources (first to be evicted)
7. **Node affinity** controls pod placement
8. **Pod anti-affinity** spreads pods for high availability
9. Monitor with `kubectl top` and adjust based on actual usage
10. Test resource limits before production deployment
