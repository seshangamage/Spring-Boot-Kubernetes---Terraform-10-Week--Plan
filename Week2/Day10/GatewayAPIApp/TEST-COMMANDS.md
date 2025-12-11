# Gateway API Test Commands

## Prerequisites
```bash
# Get Gateway IP
GATEWAY_IP=$(kubectl get svc traefik -n traefik -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# For Minikube
# GATEWAY_IP=$(minikube ip)

# For Kind (use localhost with port-forward)
# kubectl port-forward -n traefik svc/traefik 8080:80 8443:443
# GATEWAY_IP=localhost:8080
```

## Basic HTTP Tests

### 1. Info Endpoint
```bash
curl http://$GATEWAY_IP/api/info \
  -H "Host: gateway-demo.local" \
  -w "\nHTTP Status: %{http_code}\n"
```

Expected response:
```json
{
  "name": "Gateway API Demo",
  "version": "1.0.0",
  "timestamp": "2024-01-15T10:30:00",
  "description": "Spring Boot application exposed via Kubernetes Gateway API",
  "gatewayType": "Traefik"
}
```

### 2. Health Endpoint
```bash
curl http://$GATEWAY_IP/api/health \
  -H "Host: gateway-demo.local" \
  -w "\nHTTP Status: %{http_code}\n"
```

### 3. Headers Inspection
```bash
curl http://$GATEWAY_IP/api/headers \
  -H "Host: gateway-demo.local" \
  -H "X-Custom-Header: test-value"
```

### 4. Gateway Info (X-Forwarded headers)
```bash
curl http://$GATEWAY_IP/api/gateway-info \
  -H "Host: gateway-demo.local"
```

### 5. Path Parameters
```bash
curl http://$GATEWAY_IP/api/path/test-value-123 \
  -H "Host: gateway-demo.local"
```

### 6. POST Request
```bash
curl -X POST http://$GATEWAY_IP/api/data \
  -H "Host: gateway-demo.local" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "action": "test-gateway"
  }'
```

## HTTPS Tests (with TLS)

### 1. HTTPS Info Endpoint (skip cert verification)
```bash
curl -k https://$GATEWAY_IP/api/info \
  -H "Host: gateway-demo.local" \
  -w "\nHTTP Status: %{http_code}\n"
```

### 2. HTTPS with hostname resolution
```bash
curl https://gateway-demo.local/api/health \
  --resolve gateway-demo.local:443:$GATEWAY_IP \
  -k \
  -w "\nHTTP Status: %{http_code}\n"
```

### 3. Check certificate
```bash
curl -vk https://$GATEWAY_IP/api/info \
  -H "Host: gateway-demo.local" 2>&1 | grep -A 10 "Server certificate"
```

## Testing Different Hostnames

### Using api.gateway-demo.local
```bash
curl http://$GATEWAY_IP/api/info \
  -H "Host: api.gateway-demo.local"
```

## Load Testing

### Using Apache Bench
```bash
ab -n 1000 -c 10 \
  -H "Host: gateway-demo.local" \
  http://$GATEWAY_IP/api/health
```

### Using hey
```bash
hey -n 1000 -c 10 \
  -H "Host: gateway-demo.local" \
  http://$GATEWAY_IP/api/info
```

## Actuator Endpoints

### Health
```bash
curl http://$GATEWAY_IP/actuator/health \
  -H "Host: gateway-demo.local"
```

### Info
```bash
curl http://$GATEWAY_IP/actuator/info \
  -H "Host: gateway-demo.local"
```

### Metrics
```bash
curl http://$GATEWAY_IP/actuator/metrics \
  -H "Host: gateway-demo.local"
```

## Debugging Commands

### 1. Check Gateway Status
```bash
kubectl get gateway -n gateway-demo
kubectl describe gateway app-gateway -n gateway-demo
```

### 2. Check HTTPRoute Status
```bash
kubectl get httproute -n gateway-demo
kubectl describe httproute app-httproute -n gateway-demo
```

### 3. Check Certificate
```bash
kubectl get certificate -n gateway-demo
kubectl describe certificate gateway-demo-tls -n gateway-demo
```

### 4. View Application Logs
```bash
kubectl logs -n gateway-demo -l app=gateway-api-app --tail=50 -f
```

### 5. View Traefik Logs
```bash
kubectl logs -n traefik -l app.kubernetes.io/name=traefik --tail=50 -f
```

### 6. Check Service Endpoints
```bash
kubectl get endpoints -n gateway-demo
```

### 7. Port Forward to App (bypass Gateway)
```bash
kubectl port-forward -n gateway-demo svc/gateway-api-app-service 8080:80
# Then: curl http://localhost:8080/api/info
```

## Testing with /etc/hosts

Add to `/etc/hosts` (Linux/Mac) or `C:\Windows\System32\drivers\etc\hosts` (Windows):
```
<GATEWAY_IP> gateway-demo.local api.gateway-demo.local
```

Then test without Host header:
```bash
curl http://gateway-demo.local/api/info
curl https://gateway-demo.local/api/health -k
```

## Advanced Testing

### 1. Test Redirect (HTTP to HTTPS)
```bash
curl -L http://$GATEWAY_IP/secure \
  -H "Host: gateway-demo.local" \
  -w "\nHTTP Status: %{http_code}\n"
```

### 2. Test with Different Methods
```bash
# GET
curl -X GET http://$GATEWAY_IP/api/info -H "Host: gateway-demo.local"

# POST
curl -X POST http://$GATEWAY_IP/api/data \
  -H "Host: gateway-demo.local" \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'

# OPTIONS (CORS preflight)
curl -X OPTIONS http://$GATEWAY_IP/api/info \
  -H "Host: gateway-demo.local" \
  -H "Origin: http://example.com"
```

### 3. Test Response Headers
```bash
curl -I http://$GATEWAY_IP/api/info \
  -H "Host: gateway-demo.local"
```

### 4. Test Timing
```bash
curl -w "\nTime Total: %{time_total}s\nTime Connect: %{time_connect}s\n" \
  -o /dev/null -s \
  http://$GATEWAY_IP/api/health \
  -H "Host: gateway-demo.local"
```

## Monitoring

### 1. Watch Gateway Events
```bash
kubectl get events -n gateway-demo --watch
```

### 2. Monitor Pod Status
```bash
watch kubectl get pods -n gateway-demo
```

### 3. Monitor Service
```bash
watch kubectl get svc -n gateway-demo
```

## Traefik Dashboard

### Access Dashboard
```bash
kubectl port-forward -n traefik \
  $(kubectl get pods -n traefik -l app.kubernetes.io/name=traefik -o name) \
  9000:9000

# Open browser: http://localhost:9000/dashboard/
```

## Cleanup Test Resources

### Delete Test Requests
```bash
# No cleanup needed for HTTP requests
```

### Reset Application
```bash
kubectl rollout restart deployment/gateway-api-app -n gateway-demo
```

## Troubleshooting Tests

### 1. Connection Refused
```bash
# Check if Gateway has external IP
kubectl get svc -n traefik

# Check Gateway status
kubectl get gateway -n gateway-demo -o yaml
```

### 2. 404 Not Found
```bash
# Check HTTPRoute
kubectl describe httproute app-httproute -n gateway-demo

# Check if service exists
kubectl get svc -n gateway-demo
```

### 3. 503 Service Unavailable
```bash
# Check if pods are running
kubectl get pods -n gateway-demo

# Check pod readiness
kubectl describe pod -n gateway-demo -l app=gateway-api-app
```

### 4. Certificate Issues
```bash
# Check certificate status
kubectl get certificate -n gateway-demo

# Check cert-manager logs
kubectl logs -n cert-manager -l app=cert-manager
```
