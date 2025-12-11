#!/bin/bash

# Gateway API Demo - Deployment Script
# This script automates the deployment of the Gateway API demonstration

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    print_error "kubectl is not installed. Please install it first."
    exit 1
fi

# Check if helm is installed
if ! command -v helm &> /dev/null; then
    print_error "helm is not installed. Please install it first."
    exit 1
fi

print_info "Starting Gateway API demonstration deployment..."

# Step 1: Install Traefik
print_info "Step 1: Installing Traefik with Gateway API support..."
kubectl create namespace traefik --dry-run=client -o yaml | kubectl apply -f -

helm repo add traefik https://traefik.github.io/charts 2>/dev/null || true
helm repo update

helm upgrade --install traefik traefik/traefik \
    -n traefik \
    -f traefik/values.yaml \
    --wait \
    --timeout 5m

print_info "Traefik installed successfully"

# Step 2: Install cert-manager
print_info "Step 2: Installing cert-manager..."
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.3/cert-manager.yaml

print_info "Waiting for cert-manager to be ready..."
kubectl wait --for=condition=ready pod \
    -l app.kubernetes.io/instance=cert-manager \
    -n cert-manager \
    --timeout=300s

print_info "cert-manager installed successfully"

# Step 3: Build application
print_info "Step 3: Building Spring Boot application..."
if [ -f "pom.xml" ]; then
    mvn clean package -DskipTests
    print_info "Application built successfully"
else
    print_warning "pom.xml not found, skipping build"
fi

# Step 4: Build Docker image
print_info "Step 4: Building Docker image..."
docker build -t gateway-api-app:1.0 . || print_warning "Docker build failed or Docker not available"

# Step 5: Load image to cluster (Minikube/Kind)
print_info "Step 5: Loading image to cluster..."
if command -v minikube &> /dev/null && minikube status &> /dev/null; then
    minikube image load gateway-api-app:1.0
    print_info "Image loaded to Minikube"
elif command -v kind &> /dev/null; then
    kind load docker-image gateway-api-app:1.0
    print_info "Image loaded to Kind"
else
    print_warning "Neither Minikube nor Kind detected. Assuming image is available in cluster."
fi

# Step 6: Deploy application
print_info "Step 6: Deploying application..."
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml

print_info "Waiting for application to be ready..."
kubectl wait --for=condition=ready pod \
    -l app=gateway-api-app \
    -n gateway-demo \
    --timeout=300s

print_info "Application deployed successfully"

# Step 7: Configure Gateway API resources
print_info "Step 7: Configuring Gateway API resources..."

# Check if GatewayClass exists
if ! kubectl get gatewayclass traefik &> /dev/null; then
    kubectl apply -f kubernetes/gateway-class.yaml
    print_info "GatewayClass created"
else
    print_info "GatewayClass already exists"
fi

# Create Issuer
kubectl apply -f kubernetes/issuer.yaml
print_info "Issuer created"

# Request certificate
kubectl apply -f kubernetes/certificate.yaml
print_info "Certificate requested"

print_info "Waiting for certificate to be ready..."
kubectl wait --for=condition=ready certificate/gateway-demo-tls \
    -n gateway-demo \
    --timeout=300s || print_warning "Certificate may not be ready yet"

# Create Gateway
kubectl apply -f kubernetes/gateway.yaml
print_info "Gateway created"

# Create HTTPRoute
kubectl apply -f kubernetes/httproute.yaml
print_info "HTTPRoute created"

# Step 8: Display status
print_info "Deployment completed! Here's the status:"
echo ""

print_info "Gateway status:"
kubectl get gateway -n gateway-demo

echo ""
print_info "HTTPRoute status:"
kubectl get httproute -n gateway-demo

echo ""
print_info "Certificate status:"
kubectl get certificate -n gateway-demo

echo ""
print_info "Application pods:"
kubectl get pods -n gateway-demo

echo ""
print_info "Services:"
kubectl get svc -n gateway-demo

echo ""
# Get Gateway address
print_info "Getting Gateway address..."
GATEWAY_IP=$(kubectl get svc traefik -n traefik -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null)
if [ -z "$GATEWAY_IP" ]; then
    GATEWAY_IP=$(kubectl get svc traefik -n traefik -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null)
fi

if [ -z "$GATEWAY_IP" ]; then
    print_warning "Gateway IP not available yet. Use 'kubectl get svc -n traefik' to check later."
    if command -v minikube &> /dev/null && minikube status &> /dev/null; then
        print_info "For Minikube, use: minikube service traefik -n traefik --url"
    fi
else
    print_info "Gateway IP: $GATEWAY_IP"
    echo ""
    print_info "Test the deployment with:"
    echo "  curl http://$GATEWAY_IP/api/info -H 'Host: gateway-demo.local'"
    echo "  curl http://$GATEWAY_IP/api/health -H 'Host: gateway-demo.local'"
fi

echo ""
print_info "For HTTPS testing, add to /etc/hosts:"
echo "  $GATEWAY_IP gateway-demo.local api.gateway-demo.local"

echo ""
print_info "Access Traefik dashboard with:"
echo "  kubectl port-forward -n traefik \$(kubectl get pods -n traefik -l app.kubernetes.io/name=traefik -o name) 9000:9000"
echo "  Then open: http://localhost:9000/dashboard/"

print_info "Deployment script completed successfully!"
