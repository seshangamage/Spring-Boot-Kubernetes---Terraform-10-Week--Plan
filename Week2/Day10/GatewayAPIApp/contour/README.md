# Contour Gateway API Configuration

Contour is another popular Gateway API implementation. This folder contains configuration for using Contour instead of Traefik.

## Installation

### Install Contour with Gateway API Support

```bash
# Install Contour
kubectl apply -f https://projectcontour.io/quickstart/contour-gateway-provisioner.yaml

# Wait for Contour to be ready
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/name=contour \
  -n projectcontour \
  --timeout=300s
```

### Verify Installation

```bash
# Check Contour pods
kubectl get pods -n projectcontour

# Check GatewayClass
kubectl get gatewayclass
```

## Using Contour with the Demo Application

The Gateway and HTTPRoute resources are compatible with Contour. Simply update the `gatewayClassName` in the Gateway resource:

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: app-gateway
  namespace: gateway-demo
spec:
  gatewayClassName: contour  # Changed from 'traefik' to 'contour'
  # ... rest of the configuration
```

## Key Differences from Traefik

| Feature | Traefik | Contour |
|---------|---------|---------|
| Upstream | Traefik Labs | VMware/Bitnami |
| Config Format | Dynamic | Declarative |
| Dashboard | Built-in web UI | Metrics only |
| Protocol Support | HTTP, TCP, UDP | HTTP, TCP, gRPC |
| TLS | Native + cert-manager | Native + cert-manager |

## Contour-Specific Features

### HTTPProxy (Contour Native)

Contour also supports its own HTTPProxy CRD which offers more features:

```yaml
apiVersion: projectcontour.io/v1
kind: HTTPProxy
metadata:
  name: app-proxy
  namespace: gateway-demo
spec:
  virtualhost:
    fqdn: gateway-demo.local
    tls:
      secretName: gateway-demo-tls
  routes:
  - conditions:
    - prefix: /api
    services:
    - name: gateway-api-app-service
      port: 80
```

### TLS Delegation

```yaml
apiVersion: projectcontour.io/v1
kind: TLSCertificateDelegation
metadata:
  name: delegation
  namespace: gateway-demo
spec:
  delegations:
  - secretName: gateway-demo-tls
    targetNamespaces:
    - "*"
```

## Deployment with Contour

```bash
# Apply the Contour Gateway configuration
kubectl apply -f contour/gateway-contour.yaml

# Apply HTTPRoute (same as Traefik version)
kubectl apply -f ../kubernetes/httproute.yaml

# Verify
kubectl get gateway -n gateway-demo
kubectl describe gateway app-gateway -n gateway-demo
```

## Accessing the Application

```bash
# Get Gateway address
GATEWAY_IP=$(kubectl get svc envoy -n projectcontour -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Test
curl http://$GATEWAY_IP/api/info -H "Host: gateway-demo.local"
```

## Monitoring

### Prometheus Metrics

Contour exposes Prometheus metrics on port 8000:

```bash
kubectl port-forward -n projectcontour \
  $(kubectl get pods -n projectcontour -l app=contour -o name | head -1) \
  8000:8000

# Access metrics at http://localhost:8000/metrics
```

### Envoy Admin Interface

```bash
kubectl port-forward -n projectcontour \
  $(kubectl get pods -n projectcontour -l app=envoy -o name | head -1) \
  9001:9001

# Access admin at http://localhost:9001
```

## Cleanup

```bash
# Delete Gateway resources
kubectl delete gateway app-gateway -n gateway-demo

# Uninstall Contour
kubectl delete -f https://projectcontour.io/quickstart/contour-gateway-provisioner.yaml
```

## References

- [Contour Documentation](https://projectcontour.io/)
- [Contour Gateway API Guide](https://projectcontour.io/docs/main/config/gateway-api/)
- [Contour GitHub](https://github.com/projectcontour/contour)
