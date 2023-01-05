#! /bin/bash

while getopts r:p: flag
do
    case "${flag}" in
        r) region=${OPTARG};;
        p) project_id=${OPTARG};;
    esac
done

# Starting Config Server in GKE

echo "Starting a Config Server pod in $region..."

cd ../config-server
cat deployment-gke-template.yaml | sed "s/_REGION/$region/g" | sed "s/_PROJECT_ID/$project_id/g" > deployment-gke.yaml
kubectl apply -f deployment-gke.yaml

echo "Starting a Config Server service in $region..."

kubectl apply -f service-gke.yaml
kubectl get service config-server-service

# Starting Discovery Server in GKE

echo "Starting a Discovery Server pod in $region..."

cd ../discovery-server
cat deployment-gke-template.yaml | sed "s/_REGION/$region/g" | sed "s/_PROJECT_ID/$project_id/g" > deployment-gke.yaml
kubectl apply -f deployment-gke.yaml

echo "Starting a Config Server service in $region..."

kubectl apply -f service-gke.yaml
kubectl get service discovery-server-service