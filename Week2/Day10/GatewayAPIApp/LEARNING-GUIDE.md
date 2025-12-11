# Day 10: Gateway API Fundamentals - Learning Guide

## Overview

This project demonstrates the Kubernetes Gateway API, a modern alternative to Ingress for managing external access to services. We use Traefik as the gateway controller and cert-manager for TLS certificate management.

## Gateway API Concepts

### 1. GatewayClass

**What it is**: A cluster-scoped resource that defines a class of gateways, similar to StorageClass or IngressClass.

**Purpose**: 
- Defines which controller manages the Gateway resources
- Allows multiple gateway implementations to coexist
- Provides configuration parameters for gateway controllers

**Example**:
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: GatewayClass
metadata:
  name: traefik
spec:
  controllerName: traefik.io/gateway-controller
```

### 2. Gateway

**What it is**: A namespace-scoped resource that defines how traffic should be processed at the network boundary.

**Purpose**:
- Configures listeners (HTTP, HTTPS, TCP, etc.)
- Defines ports and protocols
- Manages TLS configuration
- Controls which routes can attach to it

**Key Features**:
- Multiple listeners on different ports
- Protocol-specific configuration
- Namespace-based route selection
- TLS termination

**Example**:
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: app-gateway
spec:
  gatewayClassName: traefik
  listeners:
  - name: http
    protocol: HTTP
    port: 80
  - name: https
    protocol: HTTPS
    port: 443
    tls:
      mode: Terminate
      certificateRefs:
      - name: gateway-demo-tls
```

### 3. HTTPRoute

**What it is**: A resource that defines HTTP routing rules.

**Purpose**:
- Route HTTP traffic to backend services
- Match requests based on path, headers, query parameters
- Apply filters (header modification, redirects, rewrites)
- Configure traffic splitting

**Matching Capabilities**:
- Path-based routing (Exact, PathPrefix, RegularExpression)
- Method matching (GET, POST, etc.)
- Header matching
- Query parameter matching

**Filters**:
- RequestHeaderModifier: Add/remove/set headers
- RequestRedirect: Redirect to different URL
- URLRewrite: Rewrite paths
- RequestMirror: Mirror traffic to another service

**Example**:
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: app-httproute
spec:
  parentRefs:
  - name: app-gateway
  hostnames:
  - "gateway-demo.local"
  rules:
  - matches:
    - path:
        type: PathPrefix
        value: /api
    backendRefs:
    - name: my-service
      port: 80
```

## Traefik as Gateway Implementation

### Why Traefik?

- Native Gateway API support
- Automatic service discovery
- Built-in Let's Encrypt support
- Dynamic configuration
- Comprehensive metrics and tracing
- Dashboard for visualization

### Installation Options

1. **Helm** (Recommended):
   ```bash
   helm install traefik traefik/traefik -n traefik -f values.yaml
   ```

2. **Manifests**:
   ```bash
   kubectl apply -f https://raw.githubusercontent.com/traefik/traefik/master/docs/content/reference/dynamic-configuration/kubernetes-gateway.yml
   ```

## TLS with cert-manager

### cert-manager Components

#### 1. Issuer / ClusterIssuer

Defines how certificates are obtained:

- **SelfSigned**: For development/testing
- **CA**: Use your own Certificate Authority
- **ACME (Let's Encrypt)**: Automated certificate management
- **Vault**: Integration with HashiCorp Vault
- **Venafi**: Enterprise PKI integration

#### 2. Certificate

Requests and manages TLS certificates:

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: gateway-demo-tls
spec:
  secretName: gateway-demo-tls
  dnsNames:
  - gateway-demo.local
  issuerRef:
    name: selfsigned-issuer
    kind: Issuer
```

#### 3. Integration with Gateway

The Certificate resource creates a Secret that's referenced by the Gateway:

```yaml
listeners:
- name: https
  tls:
    certificateRefs:
    - name: gateway-demo-tls  # Created by cert-manager
```

### ACME Challenge Types

1. **HTTP-01**: Places a file at a specific HTTP URL
2. **DNS-01**: Creates a DNS TXT record (supports wildcards)

For Gateway API with HTTP-01:
```yaml
solvers:
- http01:
    gatewayHTTPRoute:
      parentRefs:
      - name: app-gateway
```

## Project Architecture

```
┌─────────────────────────────────────────────┐
│           External Traffic                   │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
         ┌─────────────────┐
         │  Gateway (LB)   │
         │   Traefik       │
         │  - Port 80      │
         │  - Port 443     │
         └────────┬────────┘
                  │
         ┌────────┴────────┐
         │   HTTPRoute     │
         │  Routing Rules  │
         └────────┬────────┘
                  │
         ┌────────┴────────────┐
         │  Backend Service    │
         │  gateway-api-app    │
         └────────┬────────────┘
                  │
    ┌─────────────┴─────────────┐
    │                           │
    ▼                           ▼
┌─────────┐               ┌─────────┐
│  Pod 1  │               │  Pod 2  │
│ (App)   │               │ (App)   │
└─────────┘               └─────────┘

TLS Certificate Flow:
cert-manager → Certificate → Secret → Gateway
```

## Step-by-Step Deployment

### Phase 1: Install Gateway Controller (Traefik)

```bash
# Create namespace
kubectl create namespace traefik

# Add Helm repo
helm repo add traefik https://traefik.github.io/charts
helm repo update

# Install Traefik
helm install traefik traefik/traefik -n traefik -f traefik/values.yaml

# Verify installation
kubectl get pods -n traefik
kubectl get gatewayclass
```

### Phase 2: Install cert-manager

```bash
# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.3/cert-manager.yaml

# Wait for ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/instance=cert-manager -n cert-manager --timeout=300s

# Verify
kubectl get pods -n cert-manager
```

### Phase 3: Build and Deploy Application

```bash
# Build application
cd GatewayAPIApp
mvn clean package

# Build Docker image
docker build -t gateway-api-app:1.0 .

# Load image to cluster (for Minikube/Kind)
minikube image load gateway-api-app:1.0
# OR for Kind:
kind load docker-image gateway-api-app:1.0

# Create namespace and deploy app
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml

# Verify
kubectl get pods -n gateway-demo
kubectl get svc -n gateway-demo
```

### Phase 4: Configure Gateway API Resources

```bash
# Apply GatewayClass (may already exist from Traefik)
kubectl apply -f kubernetes/gateway-class.yaml

# Create Issuer for certificates
kubectl apply -f kubernetes/issuer.yaml

# Request certificate
kubectl apply -f kubernetes/certificate.yaml

# Wait for certificate to be ready
kubectl wait --for=condition=ready certificate/gateway-demo-tls -n gateway-demo --timeout=300s

# Create Gateway
kubectl apply -f kubernetes/gateway.yaml

# Create HTTPRoute
kubectl apply -f kubernetes/httproute.yaml

# Verify Gateway status
kubectl get gateway -n gateway-demo
kubectl describe gateway app-gateway -n gateway-demo
```

## Testing the Deployment

### Get Gateway Address

```bash
# For LoadBalancer service
kubectl get gateway app-gateway -n gateway-demo -o jsonpath='{.status.addresses[0].value}'

# For Minikube
minikube service traefik -n traefik --url
```

### Test HTTP Endpoints

```bash
# Set variables
GATEWAY_IP=<your-gateway-ip>

# Test info endpoint
curl http://$GATEWAY_IP/api/info -H "Host: gateway-demo.local"

# Test health endpoint
curl http://$GATEWAY_IP/api/health -H "Host: gateway-demo.local"

# Test headers (see Gateway headers)
curl http://$GATEWAY_IP/api/gateway-info -H "Host: gateway-demo.local"

# Test path parameters
curl http://$GATEWAY_IP/api/path/test123 -H "Host: gateway-demo.local"

# Test POST
curl -X POST http://$GATEWAY_IP/api/data \
  -H "Host: gateway-demo.local" \
  -H "Content-Type: application/json" \
  -d '{"key":"value","test":"data"}'
```

### Test HTTPS

```bash
# Test HTTPS (with self-signed cert, use -k to skip verification)
curl -k https://$GATEWAY_IP/api/info -H "Host: gateway-demo.local"

# Test with hostname resolution
curl https://gateway-demo.local/api/info \
  --resolve gateway-demo.local:443:$GATEWAY_IP \
  -k
```

### Using /etc/hosts

Add to `/etc/hosts` (or `C:\Windows\System32\drivers\etc\hosts` on Windows):
```
<GATEWAY_IP> gateway-demo.local api.gateway-demo.local
```

Then access directly:
```bash
curl http://gateway-demo.local/api/info
curl https://gateway-demo.local/api/health -k
```

## Monitoring and Debugging

### Check Gateway Status

```bash
# Gateway status
kubectl get gateway -n gateway-demo
kubectl describe gateway app-gateway -n gateway-demo

# Check listeners
kubectl get gateway app-gateway -n gateway-demo -o yaml

# Gateway events
kubectl get events -n gateway-demo --sort-by='.lastTimestamp'
```

### Check HTTPRoute Status

```bash
# HTTPRoute status
kubectl get httproute -n gateway-demo
kubectl describe httproute app-httproute -n gateway-demo

# Check route binding
kubectl get httproute app-httproute -n gateway-demo -o yaml
```

### Check Certificate Status

```bash
# Certificate status
kubectl get certificate -n gateway-demo
kubectl describe certificate gateway-demo-tls -n gateway-demo

# Check secret
kubectl get secret gateway-demo-tls -n gateway-demo

# Certificate details
kubectl get certificate gateway-demo-tls -n gateway-demo -o yaml
```

### Traefik Dashboard

```bash
# Port forward to dashboard
kubectl port-forward -n traefik $(kubectl get pods -n traefik -l app.kubernetes.io/name=traefik -o name) 9000:9000

# Access at http://localhost:9000/dashboard/
```

### Logs

```bash
# Traefik logs
kubectl logs -n traefik -l app.kubernetes.io/name=traefik -f

# Application logs
kubectl logs -n gateway-demo -l app=gateway-api-app -f

# cert-manager logs
kubectl logs -n cert-manager -l app=cert-manager -f
```

## Advanced Concepts

### Traffic Splitting

```yaml
backendRefs:
- name: app-v1
  port: 80
  weight: 90
- name: app-v2
  port: 80
  weight: 10
```

### Header-Based Routing

```yaml
matches:
- headers:
  - name: X-Version
    value: v2
```

### Query Parameter Matching

```yaml
matches:
- queryParams:
  - name: version
    value: beta
```

### Request Mirroring

```yaml
filters:
- type: RequestMirror
  requestMirror:
    backendRef:
      name: analytics-service
      port: 80
```

## Troubleshooting

### Gateway Not Ready

```bash
# Check Traefik pods
kubectl get pods -n traefik

# Check GatewayClass
kubectl get gatewayclass

# Check events
kubectl get events -n gateway-demo
```

### Certificate Not Issued

```bash
# Check certificate status
kubectl describe certificate gateway-demo-tls -n gateway-demo

# Check cert-manager logs
kubectl logs -n cert-manager -l app=cert-manager

# Check issuer
kubectl describe issuer selfsigned-issuer -n gateway-demo
```

### Route Not Working

```bash
# Check HTTPRoute status
kubectl describe httproute app-httproute -n gateway-demo

# Check if route is attached to gateway
kubectl get httproute app-httproute -n gateway-demo -o yaml

# Check service exists
kubectl get svc -n gateway-demo
```

## Comparison: Gateway API vs Ingress

| Feature | Ingress | Gateway API |
|---------|---------|-------------|
| Resource Model | Single resource | Multiple resources (GatewayClass, Gateway, Route) |
| Protocol Support | HTTP/HTTPS only | HTTP, HTTPS, TCP, UDP, gRPC |
| Role-based | No | Yes (separation of concerns) |
| Traffic Splitting | Extension-dependent | Built-in |
| Header Manipulation | Extension-dependent | Built-in |
| Multi-protocol | No | Yes |
| Standardization | Limited | Comprehensive |

## Best Practices

1. **Use separate Gateways** for different traffic types (public, internal, admin)
2. **Implement TLS** for all external-facing services
3. **Use namespaces** to separate concerns
4. **Monitor Gateway metrics** for performance and errors
5. **Test certificate renewal** before expiration
6. **Use Let's Encrypt staging** for testing before production
7. **Implement proper RBAC** for Gateway resources
8. **Version your HTTPRoutes** for safe updates

## Next Steps

1. Explore Contour as an alternative gateway implementation
2. Implement rate limiting and authentication
3. Configure observability (metrics, traces, logs)
4. Set up multi-cluster gateway configurations
5. Implement advanced traffic management patterns

## References

- [Gateway API Documentation](https://gateway-api.sigs.k8s.io/)
- [Traefik Gateway Provider](https://doc.traefik.io/traefik/providers/kubernetes-gateway/)
- [cert-manager Documentation](https://cert-manager.io/docs/)
- [Gateway API GitHub](https://github.com/kubernetes-sigs/gateway-api)
