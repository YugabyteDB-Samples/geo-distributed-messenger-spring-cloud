#! /bin/bash

while getopts r:p: flag
do
    case "${flag}" in
        r) region=${OPTARG};;
        p) project_id=${OPTARG};;
    esac
done

# Starting Config Server in GKE

echo "Starting an Attachments pod in $region..."

cd ../attachments
cat deployment-gke-template.yaml | sed "s/_REGION/$region/g" | sed "s/_PROJECT_ID/$project_id/g" > deployment-gke.yaml
kubectl apply -f deployment-gke.yaml

echo "Starting an Attachments service in $region..."

kubectl apply -f service-gke.yaml
kubectl get service config-server-service