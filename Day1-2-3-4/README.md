# Day 1-2-3-4: Spring Boot on Kubernetes

## Prerequisites
- Minikube installed
- Docker installed
- kubectl installed

## Verify Cluster
```powershell
kubectl cluster-info
kubectl get nodes
kubectl get pods -A
```

## Deploy Your First Pod (nginx)
```powershell
kubectl create deployment nginx --image=nginx
kubectl get deployments
kubectl get pods
```

## Expose the Deployment (Service)
```powershell
kubectl expose deployment nginx --type=NodePort --port=80
minikube service nginx --url
```

## View Kubernetes Dashboard
```powershell
minikube dashboard
```

## Building the Docker Image from Spring Boot Code
```powershell
docker build -t springboot-microservice:1.0 .
```

## Run the Container Locally
Test locally at http://localhost:8080
```powershell
docker run -p 8080:8080 springboot-microservice:1.0
```

**Note:** For this project we will not be pushing the images to the docker registry

## Start Minikube
Run in a separate command line opened with Administrator privileges
```powershell
minikube start
```

## Configure Docker to Use Minikube's Docker Daemon
```powershell
minikube docker-env | Invoke-Expression
```

## Build Image in Minikube's Docker
```powershell
docker build -t springboot-microservice:1.0 .
```

## Applying YAML Files
```powershell
kubectl apply -f .\kubernetes\spingboot-deployment.yaml
kubectl apply -f .\kubernetes\springboot-service.yaml
```

## Start Minikube Tunnel
Use the separate CMD which was running with admin privilege to run the below
```powershell
minikube tunnel
```
**Important:** Keep this terminal running!

## Check Deployment Status
```powershell
kubectl get deployments
```
Make sure the previous tunnel command is running before running this.

This command will give you something like this:
```
NAME             READY   UP-TO-DATE   AVAILABLE   AGE
nginx            1/1     1            1           111m
springboot-app   2/2     2            2           20m
```

```powershell
kubectl get pods
```
This command will give you something like below:
```
NAME                              READY   STATUS    RESTARTS   AGE
nginx-66686b6766-md5sm            1/1     Running   0          111m
springboot-app-6dbfd88fd9-7dr4l   1/1     Running   0          3m43s
springboot-app-6dbfd88fd9-87lnn   1/1     Running   0          3m41s
```

## Access the Application
After deployment, access the app at:
```
http://127.0.0.1/
```

## Cleanup
```powershell
kubectl delete -f .\kubernetes\
kubectl delete deployment nginx
```
