#! /bin/bash

while getopts n:i:r:s:c:u:p:m:f: flag
do
    case "${flag}" in
        n) name=${OPTARG};;
        i) project_id=${OPTARG};;
        r) region=${OPTARG};;
        s) subnet=${OPTARG};;
        c) primary_endpoint=${OPTARG};;
        a) additional_endpoints=${OPTARG};;
        u) user=${OPTARG};;
        p) pwd=${OPTARG};;
        m) mode=${OPTARG};;
        f) schema_file=${OPTARG};;
    esac
done

echo "Starting instance $name in zone $zone..."

gcloud compute instances create $name \
        --project=$project_id \
        --scopes=default,storage-full \
        --machine-type=e2-highcpu-4 \
        --boot-disk-type=pd-balanced --boot-disk-size=10GB \
        --network=default --zone=$zone \
        --image-family=ubuntu-1804-lts --image-project=ubuntu-os-cloud \
        --tags=geo-messenger-instance, \
        --metadata-from-file=startup-script=startup_script.sh, \
        --metadata=^::^DB_PRIMARY_ENDPOINT=$primary_endpoint::DB_ADDITIONAL_ENDPOINTS=$additional_endpoints::DB_USER=$user::DB_PWD=$pwd::DB_MODE=$mode::DB_SCHEMA_FILE=$schema_file::REGION=$region::PROJECT_ID=$project_id

if [ $? -eq 0 ]; then
    echo "Instance $name has been created!"
    echo "The will be started on port $port and connect to the database $primary_endpoint"
    echo "Use command below to check the progress: "
    echo "  "
    echo "  gcloud compute ssh $name --project=geo-distributed-messenger"
    echo "  sudo journalctl -u google-startup-scripts.service -f"
else
    echo FAIL
fi