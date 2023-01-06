#! /bin/bash

while getopts r:p: flag
do
    case "${flag}" in
        r) region=${OPTARG};;
        p) project_id=${OPTARG};;
    esac
done

# Starting Config Server in GKE

echo "Starting Config Server in $region..."

cd ../config-server
cat deployment-gke-template.yaml | sed "s/_REGION/$region/g" | sed "s/_PROJECT_ID/$project_id/g" > deployment-gke.yaml
kubectl apply -f deployment-gke.yaml

kubectl apply -f service-gke.yaml
kubectl get service config-server-service

# Starting the Attachments microservice in GKE

echo "Starting Attachments in $region..."

cd ../attachments
cat deployment-gke-template.yaml | sed "s/_REGION/$region/g" | sed "s/_PROJECT_ID/$project_id/g" > deployment-gke.yaml
kubectl apply -f deployment-gke.yaml

kubectl apply -f service-gke.yaml
kubectl get service attachments-service

# Starting the Messenger microservice in GKE

echo "Starting Messenger in $region..."

cd ../messenger
kubectl apply -f secret-gke.yaml

cat deployment-gke-template.yaml | sed "s/_REGION/$region/g" | sed "s/_PROJECT_ID/$project_id/g" > deployment-gke.yaml
kubectl apply -f deployment-gke.yaml

kubectl apply -f service-gke.yaml
kubectl get service messenger-service