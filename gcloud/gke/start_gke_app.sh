#! /bin/bash

set -e # stop executing the script if any command fails

while getopts r:n:a:s: flag
do
    case "${flag}" in
        r) region=${OPTARG};;
        n) cluster_name=${OPTARG};;
        a) k8_service_account=${OPTARG};;
        s) db_schema=${OPTARG};;
    esac
done

project_id=$(gcloud config get-value project)

echo "Google Cloud project id: $project_id"

# Starting the Config server

echo "Starting Config Server in $region for cluster $cluster_name..."

kubectl config use-context $cluster_name

cd ../../config-server
cat deployment-gke-template.yaml | sed "s/_REGION/$region/g" | sed "s/_PROJECT_ID/$project_id/g" | sed "s/_SERVICE_ACCOUNT/$k8_service_account/g" > deployment-gke.yaml
kubectl apply -f deployment-gke.yaml

kubectl apply -f service-gke.yaml
kubectl get service config-server-service --namespace geo-messenger

# Starting the Attachments microservice in GKE

echo "Starting Attachments in $region for cluster $cluster_name..."

cd ../attachments
cat deployment-gke-template.yaml | sed "s/_REGION/$region/g" | sed "s/_PROJECT_ID/$project_id/g" | sed "s/_SERVICE_ACCOUNT/$k8_service_account/g" > deployment-gke.yaml
kubectl apply -f deployment-gke.yaml

kubectl apply -f service-gke.yaml
kubectl get service attachments-service --namespace geo-messenger

# Starting the Messenger microservice in GKE

echo "Starting Messenger in $region..."

cd ../messenger
kubectl apply -f secret-gke.yaml

cat deployment-gke-template.yaml | sed "s/_REGION/$region/g" | sed "s/_PROJECT_ID/$project_id/g" | sed "s/_SERVICE_ACCOUNT/$k8_service_account/g" | sed "s/_DB_SCHEMA/$db_schema/g" > deployment-gke.yaml
kubectl apply -f deployment-gke.yaml

kubectl apply -f service-gke.yaml
kubectl get service messenger-service --namespace geo-messenger