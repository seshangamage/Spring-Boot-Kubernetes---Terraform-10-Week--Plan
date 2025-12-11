#!/bin/bash

# Gateway API Demo - Cleanup Script
# This script removes all resources created by the deployment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning "This will delete all Gateway API demo resources."
read -p "Are you sure you want to continue? (yes/no): " -r
echo
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    print_info "Cleanup cancelled."
    exit 0
fi

print_info "Starting cleanup..."

# Delete Gateway API resources
print_info "Deleting Gateway API resources..."
kubectl delete -f kubernetes/httproute.yaml --ignore-not-found=true
kubectl delete -f kubernetes/gateway.yaml --ignore-not-found=true
kubectl delete -f kubernetes/certificate.yaml --ignore-not-found=true
kubectl delete -f kubernetes/issuer.yaml --ignore-not-found=true
kubectl delete -f kubernetes/gateway-class.yaml --ignore-not-found=true

# Delete application
print_info "Deleting application..."
kubectl delete -f kubernetes/service.yaml --ignore-not-found=true
kubectl delete -f kubernetes/deployment.yaml --ignore-not-found=true
kubectl delete -f kubernetes/namespace.yaml --ignore-not-found=true

# Optionally delete Traefik
read -p "Do you want to uninstall Traefik? (yes/no): " -r
echo
if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    print_info "Uninstalling Traefik..."
    helm uninstall traefik -n traefik || print_warning "Traefik uninstall failed"
    kubectl delete namespace traefik --ignore-not-found=true
    print_info "Traefik uninstalled"
fi

# Optionally delete cert-manager
read -p "Do you want to uninstall cert-manager? (yes/no): " -r
echo
if [[ $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    print_info "Uninstalling cert-manager..."
    kubectl delete -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.3/cert-manager.yaml --ignore-not-found=true
    print_info "cert-manager uninstalled"
fi

print_info "Cleanup completed!"
