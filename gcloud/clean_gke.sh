#! /bin/bash

echo "Cleaning GKE resources..."

kubectl delete services config-server-service
kubectl delete services attachments-service
kubectl delete services messenger-service

kubectl delete deployments config-server-gke
kubectl delete deployments attachments-gke
kubectl delete deployments messenger-gke
