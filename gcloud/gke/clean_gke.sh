#! /bin/bash

while getopts n: flag
do
    case "${flag}" in
        n) cluster_name=${OPTARG};;
    esac
done

kubectl config use-context $cluster_name

echo "Cleaning GKE resources..."

kubectl delete services config-server-service --namespace geo-messenger
kubectl delete services attachments-service --namespace geo-messenger
kubectl delete services messenger-service --namespace geo-messenger

kubectl delete deployments config-server-gke --namespace geo-messenger
kubectl delete deployments attachments-gke --namespace geo-messenger
kubectl delete deployments messenger-gke --namespace geo-messenger
