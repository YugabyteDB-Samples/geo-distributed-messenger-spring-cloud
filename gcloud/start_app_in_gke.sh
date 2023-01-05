#! /bin/bash

while getopts r:p: flag
do
    case "${flag}" in
        r) region=${OPTARG};;
        p) project_id=${OPTARG};;
    esac
done

# Starting the Attachments microservice in GKE

echo "Starting an Attachments pod in $region..."

cd ../attachments
cat deployment-gke-template.yaml | sed "s/_REGION/$region/g" | sed "s/_PROJECT_ID/$project_id/g" > deployment-gke.yaml
kubectl apply -f deployment-gke.yaml

echo "Starting an Attachments service in $region..."

kubectl apply -f service-gke.yaml
kubectl get service attachments-service

# Starting the Messenger microservice in GKE

echo "Starting a Messenger pod in $region..."

cd ../messenger
kubectl apply -f secret-gke.yaml

cat deployment-gke-template.yaml | sed "s/_REGION/$region/g" | sed "s/_PROJECT_ID/$project_id/g" > deployment-gke.yaml
kubectl apply -f deployment-gke.yaml

echo "Starting a Messenger service in $region..."

kubectl apply -f service-gke.yaml
kubectl get service messenger-service