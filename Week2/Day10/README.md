# Day 10: Gateway API Fundamentals

This project demonstrates Kubernetes Gateway API fundamentals using Traefik as the gateway implementation.

## Topics Covered

- **GatewayClass**: Define the gateway controller
- **Gateway**: Configure listeners and routing
- **HTTPRoute**: Route HTTP traffic to services
- **TLS Setup**: Secure connections with cert-manager

## Prerequisites

- Kubernetes cluster (Minikube, Kind, or cloud provider)
- kubectl installed and configured
- Helm 3.x installed

## Project Structure

```
GatewayAPIApp/
├── src/                          # Spring Boot application source
├── kubernetes/                   # Kubernetes manifests
│   ├── namespace.yaml           # Namespace definition
│   ├── deployment.yaml          # Application deployment
│   ├── service.yaml             # Service definition
│   ├── gateway-class.yaml       # GatewayClass resource
│   ├── gateway.yaml             # Gateway resource
│   ├── httproute.yaml           # HTTPRoute resource
│   ├── certificate.yaml         # TLS certificate
│   └── issuer.yaml              # cert-manager issuer
├── traefik/                     # Traefik installation
│   └── values.yaml              # Helm values for Traefik
└── pom.xml                      # Maven configuration
```

## Installation Steps

### 1. Install Traefik Gateway API Support

```bash
# Add Traefik Helm repository
helm repo add traefik https://traefik.github.io/charts
helm repo update

# Create namespace for Traefik
kubectl create namespace traefik

# Install Traefik with Gateway API enabled
helm install traefik traefik/traefik -n traefik -f traefik/values.yaml
```

### 2. Install cert-manager

```bash
# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.3/cert-manager.yaml

# Wait for cert-manager to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/instance=cert-manager -n cert-manager --timeout=300s
```

### 3. Build and Deploy Application

```bash
# Build the Spring Boot application
mvn clean package

# Build Docker image
docker build -t gateway-api-app:1.0 .

# Apply Kubernetes manifests
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
```

### 4. Configure Gateway API Resources

```bash
# Apply Gateway API resources
kubectl apply -f kubernetes/gateway-class.yaml
kubectl apply -f kubernetes/issuer.yaml
kubectl apply -f kubernetes/gateway.yaml
kubectl apply -f kubernetes/httproute.yaml
kubectl apply -f kubernetes/certificate.yaml
```

## Testing

### Test HTTP Access

```bash
# Get Gateway external IP
kubectl get gateway app-gateway -n gateway-demo

# Test the application
curl http://<GATEWAY_IP>/api/info
curl http://<GATEWAY_IP>/api/health
```

### Test HTTPS Access

```bash
# Test with TLS (after certificate is issued)
curl https://gateway-demo.local/api/info --resolve gateway-demo.local:443:<GATEWAY_IP>
```

### Verify Gateway Resources

```bash
# Check GatewayClass
kubectl get gatewayclass

# Check Gateway status
kubectl get gateway -n gateway-demo

# Check HTTPRoute
kubectl get httproute -n gateway-demo

# Check certificate status
kubectl get certificate -n gateway-demo
```

## Key Concepts

### GatewayClass
Defines which controller manages the Gateway resources. In this example, Traefik is the controller.

### Gateway
Defines how traffic enters the cluster. Configures listeners (HTTP/HTTPS), ports, and protocols.

### HTTPRoute
Defines routing rules for HTTP traffic, including path matching and backend services.

### TLS with cert-manager
- **Issuer**: Defines how certificates are obtained (self-signed, Let's Encrypt, etc.)
- **Certificate**: Requests and manages TLS certificates
- Integration with Gateway for automatic TLS termination

## Cleanup

```bash
# Delete resources
kubectl delete -f kubernetes/
kubectl delete namespace gateway-demo

# Uninstall Traefik
helm uninstall traefik -n traefik
kubectl delete namespace traefik

# Uninstall cert-manager (optional)
kubectl delete -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.3/cert-manager.yaml
```

## References

- [Kubernetes Gateway API](https://gateway-api.sigs.k8s.io/)
- [Traefik Gateway API](https://doc.traefik.io/traefik/routing/providers/kubernetes-gateway/)
- [cert-manager Documentation](https://cert-manager.io/docs/)
