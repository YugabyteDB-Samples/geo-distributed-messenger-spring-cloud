#! /bin/bash

echo "Cleaning GKE resources..."

kubectl delete services config-server-service
kubectl delete services discovery-server-service

kubectl delete deployments config-server-gke
kubectl delete deployments discovery-server-gke

