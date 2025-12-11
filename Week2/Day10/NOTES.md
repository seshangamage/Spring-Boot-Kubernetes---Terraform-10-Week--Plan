# Day 10: Gateway API Fundamentals - Quick Reference

## What is Gateway API?

The Kubernetes Gateway API is a collection of resources that model service networking in Kubernetes. It's the successor to Ingress and provides a more expressive, extensible, and role-oriented API for managing traffic.

## Core Resources

### 1. GatewayClass
- **Scope**: Cluster-wide
- **Purpose**: Defines the controller that will handle Gateway resources
- **Analogy**: Like a StorageClass or IngressClass
- **Who manages**: Cluster operator/admin

### 2. Gateway
- **Scope**: Namespaced
- **Purpose**: Defines load balancer configuration and listeners
- **Analogy**: Like a physical load balancer or reverse proxy
- **Who manages**: Platform team

### 3. HTTPRoute (and other Routes)
- **Scope**: Namespaced
- **Purpose**: Defines routing rules for traffic
- **Analogy**: Like virtual hosts and location blocks in Nginx
- **Who manages**: Application developers

## Key Advantages Over Ingress

1. **Role-oriented design**: Separation of concerns between infrastructure and application teams
2. **Expressive**: Built-in support for traffic splitting, header matching, URL rewriting
3. **Extensible**: Custom routes for different protocols (HTTP, TCP, UDP, gRPC)
4. **Portable**: Standardized across different implementations
5. **Type-safe**: Strongly typed API with proper validation

## Resource Relationships

```
GatewayClass (cluster-scoped)
    ↓ references
Gateway (namespaced)
    ↓ attaches
HTTPRoute (namespaced)
    ↓ routes to
Service (namespaced)
    ↓ selects
Pods
```

## Common Implementations

| Implementation | Provider | Notes |
|----------------|----------|-------|
| **Traefik** | Traefik Labs | Easy setup, good dashboard |
| **Contour** | VMware/Bitnami | Production-ready, Envoy-based |
| **Istio** | Google/IBM/Lyft | Full service mesh |
| **Kong** | Kong Inc. | API Gateway features |
| **NGINX** | NGINX Inc. | Traditional reverse proxy |

## This Project Structure

```
Day10/
├── README.md                    # Main documentation
├── GatewayAPIApp/               # Demo application
│   ├── src/                     # Spring Boot source code
│   ├── kubernetes/              # Kubernetes manifests
│   │   ├── namespace.yaml       # Create namespace
│   │   ├── deployment.yaml      # Deploy application
│   │   ├── service.yaml         # Expose pods
│   │   ├── gateway-class.yaml   # Define controller
│   │   ├── gateway.yaml         # Configure listeners
│   │   ├── httproute.yaml       # Define routes
│   │   ├── issuer.yaml          # cert-manager issuers
│   │   └── certificate.yaml     # TLS certificates
│   ├── traefik/                 # Traefik configuration
│   │   └── values.yaml          # Helm values
│   ├── contour/                 # Contour alternative
│   │   ├── README.md            # Contour docs
│   │   └── gateway-contour.yaml # Contour Gateway
│   ├── deploy.sh                # Deployment script
│   ├── cleanup.sh               # Cleanup script
│   ├── LEARNING-GUIDE.md        # Detailed guide
│   └── TEST-COMMANDS.md         # Testing commands
```

## Quick Start

```bash
# 1. Install Traefik
helm install traefik traefik/traefik -n traefik -f traefik/values.yaml

# 2. Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.3/cert-manager.yaml

# 3. Deploy application
kubectl apply -f kubernetes/

# 4. Test
GATEWAY_IP=$(kubectl get svc traefik -n traefik -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
curl http://$GATEWAY_IP/api/info -H "Host: gateway-demo.local"
```

## HTTPRoute Matching

### Path Matching
```yaml
- path:
    type: Exact           # Exact match: /api/info
    type: PathPrefix      # Prefix match: /api/*
    type: RegularExpression  # Regex (implementation-specific)
```

### Header Matching
```yaml
- headers:
  - name: X-Version
    value: v2
```

### Method Matching
```yaml
- method: GET
- method: POST
```

### Query Parameter Matching
```yaml
- queryParams:
  - name: version
    value: beta
```

## HTTPRoute Filters

### Request Header Modifier
```yaml
filters:
- type: RequestHeaderModifier
  requestHeaderModifier:
    add:
    - name: X-Custom
      value: added
    set:
    - name: X-Override
      value: new-value
    remove: ["X-Remove"]
```

### Request Redirect
```yaml
filters:
- type: RequestRedirect
  requestRedirect:
    scheme: https
    statusCode: 301
```

### URL Rewrite
```yaml
filters:
- type: URLRewrite
  urlRewrite:
    path:
      type: ReplacePrefixMatch
      replacePrefixMatch: /v2
```

### Request Mirror
```yaml
filters:
- type: RequestMirror
  requestMirror:
    backendRef:
      name: analytics-service
      port: 80
```

## Traffic Splitting

```yaml
backendRefs:
- name: app-v1
  port: 80
  weight: 90
- name: app-v2
  port: 80
  weight: 10
```

## TLS Configuration

### Gateway TLS Termination
```yaml
listeners:
- name: https
  protocol: HTTPS
  port: 443
  tls:
    mode: Terminate
    certificateRefs:
    - name: my-cert-secret
```

### TLS Modes
- **Terminate**: Decrypt at gateway
- **Passthrough**: Forward encrypted to backend

### cert-manager Integration

```yaml
# Issuer (self-signed)
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: selfsigned-issuer
spec:
  selfSigned: {}

# Certificate
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: my-cert
spec:
  secretName: my-cert-secret
  dnsNames:
  - example.com
  issuerRef:
    name: selfsigned-issuer
```

## Common Use Cases

### 1. Simple HTTP Routing
Gateway + HTTPRoute with path-based routing

### 2. HTTPS with TLS Termination
Gateway with HTTPS listener + cert-manager Certificate

### 3. Blue-Green Deployment
HTTPRoute with traffic splitting between two services

### 4. Canary Deployment
HTTPRoute with weighted traffic distribution

### 5. Header-Based Routing
HTTPRoute with header matching for A/B testing

### 6. Multi-tenancy
Multiple HTTPRoutes with different hostnames

### 7. HTTP to HTTPS Redirect
HTTPRoute with RequestRedirect filter

## Troubleshooting Checklist

- [ ] Check if GatewayClass exists and has correct controller
- [ ] Verify Gateway has addressable IP/hostname
- [ ] Confirm HTTPRoute is attached to Gateway (check status)
- [ ] Ensure Service exists and has endpoints
- [ ] Verify Pods are running and ready
- [ ] Check Certificate is ready (for HTTPS)
- [ ] Review gateway controller logs
- [ ] Test with port-forward to Service (bypass Gateway)
- [ ] Check RBAC permissions for gateway controller

## Best Practices

1. **Use separate Gateways** for different environments (dev, staging, prod)
2. **Implement namespaces** for logical separation
3. **Enable TLS** for all production traffic
4. **Use Let's Encrypt** for automatic certificate management
5. **Monitor Gateway metrics** (requests, latency, errors)
6. **Test certificate renewal** before expiration
7. **Apply rate limiting** to prevent abuse
8. **Use request timeouts** to prevent resource exhaustion
9. **Implement health checks** on backend services
10. **Version your HTTPRoutes** for safe rollbacks

## Monitoring and Observability

### Metrics to Monitor
- Request rate and latency
- Error rate (4xx, 5xx)
- Backend health
- Certificate expiration
- Gateway resource usage

### Tools
- Prometheus + Grafana
- Traefik Dashboard
- kubectl describe gateway
- kubectl get events

## Next Steps

- [ ] Implement authentication/authorization
- [ ] Add rate limiting
- [ ] Configure CORS policies
- [ ] Set up observability (traces, metrics, logs)
- [ ] Explore service mesh integration
- [ ] Test multi-cluster scenarios
- [ ] Implement circuit breakers
- [ ] Add WAF (Web Application Firewall)

## Resources

- **Gateway API Docs**: https://gateway-api.sigs.k8s.io/
- **Traefik Gateway**: https://doc.traefik.io/traefik/routing/providers/kubernetes-gateway/
- **Contour**: https://projectcontour.io/
- **cert-manager**: https://cert-manager.io/
- **Examples**: https://github.com/kubernetes-sigs/gateway-api/tree/main/examples

## Comparison: Ingress vs Gateway API

| Aspect | Ingress | Gateway API |
|--------|---------|-------------|
| API Version | v1 (stable) | v1 (stable as of Oct 2023) |
| Resources | 1 (Ingress) | 3+ (GatewayClass, Gateway, Routes) |
| Protocol Support | HTTP/HTTPS | HTTP, HTTPS, TCP, UDP, gRPC |
| Roles | Combined | Separated (infrastructure vs app) |
| Extensibility | Annotations | Typed fields |
| Traffic Split | Via annotations | Native support |
| Cross-namespace | Limited | First-class support |
| Standardization | Basic | Comprehensive |
| Migration Path | - | Can coexist with Ingress |

## Quick Command Reference

```bash
# Get Gateway info
kubectl get gatewayclass
kubectl get gateway -n <namespace>
kubectl describe gateway <name> -n <namespace>

# Get Route info
kubectl get httproute -n <namespace>
kubectl describe httproute <name> -n <namespace>

# Check certificates
kubectl get certificate -n <namespace>
kubectl describe certificate <name> -n <namespace>

# View logs
kubectl logs -n traefik -l app.kubernetes.io/name=traefik
kubectl logs -n gateway-demo -l app=gateway-api-app

# Port forward
kubectl port-forward -n traefik svc/traefik 8080:80
kubectl port-forward -n gateway-demo svc/gateway-api-app-service 8080:80

# Events
kubectl get events -n gateway-demo --sort-by='.lastTimestamp'
```
