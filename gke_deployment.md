# Google Kubernetes Engine Deployment

The instruction explains how to deploy the applicaion in Google Kubernetes Engine.

## Create Google Cloud Project

1. Log in under your account:
    ```shell
    gcloud auth login
    ```

2. Create a new project for the app (use any other project name if `geo-distributed-messenger` is not available):
    ```shell
    gcloud projects create geo-distributed-messenger --name="Geo-Distributed Messenger"
    ```

3. Set this new project as default:
    ```shell
    gcloud config set project geo-distributed-messenger
    ```

4. Open Google Console and enable a billing account for the project: `https://console.cloud.google.com`

5. [Enable](https://console.cloud.google.com/flows/enableapi?apiid=artifactregistry.googleapis.com,cloudbuild.googleapis.com,container.googleapis.com&redirect=https://console.cloud.google.com&_ga=2.220829720.1831599196.1672860095-1629291620.1658249275&_gac=1.192717528.1671329959.CjwKCAiA7vWcBhBUEiwAXieItpcBgXS6j-SP2knNZYtSNXNn5f47EGszdv3UbRLZfbWH8alv4pQ9cxoCSG0QAvD_BwE) Artifact Registry, Cloud Build and Kubernetes Engine APIs.

## Enable Google Cloud Storage

The Attachments microservice uploads pictures to the [Google Cloud Storage](https://cloud.google.com/storage). Enable the service for this Google project.

## Create Artifact Registry Repository

First, you need to create a Docker container for each application microservice and load the container
to [Artifact Registry](https://cloud.google.com/artifact-registry).

Create a repository for Docker images in the registry:
```shell
gcloud artifacts repositories create geo-distributed-messenger-repo \
    --repository-format=docker \
    --location=us-east4 \
    --description="Docker repository for geo-distributed messenger containers"
```

## Prepare App Images

1. Build a Config Server's docker image using [Cloud Build](https://cloud.google.com/build):
    ```shell
    cd config-server

    gcloud builds submit --config cloudbuild.yaml \
        --substitutions _REGION=us-east4
    ```

2. Build a Discover Service's image:
    ```shell
    cd ../discovery-server

    gcloud builds submit --config cloudbuild.yaml \
        --substitutions _REGION=us-east4
    ```

3. Build an Attachment's microservice image:
    ```shell
    cd ../attachments

    gcloud builds submit --config cloudbuild.yaml \
        --substitutions _REGION=us-east4
    ```
4. Finally, build the last image for the Messenger microservice:
    ```shell
    cd ../messenger

    gcloud builds submit --config cloudbuild.yaml \
        --substitutions _REGION=us-east4
    ```        

## Start a GKE Cluster

1. Create a Kubernetes cluster within the `us-east4` region:
    ```shell
    gcloud container clusters create-auto geo-distributed-messenger-gke \
        --region us-east4
    ```

2. Verify you have access to the cluster by getting a list of running nodes:
    ```shell
    kubectl get nodes
    ```

## Start Config and Discovery Servers in GKE

First, start two Spring Cloud components (Config and Discovery servers) that are used by the application logic.

1. Navigate to the `gcloud` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud
    ```
2. Start the application instances in GKE using the script below:
    ```shell
    ./start_spring_cloud_servers_in_gke.sh -r us-east4 -p YOUR_PROJECT_ID
    ```

    Replace the `YOUR_PROJECT_ID` placeholder with your Google project id.

3. Wait while all the deployments are ready:
    ```shell
    kubectl get deployments

    # Also, you can follow the logs of a spefic deployment for more details:
    kubectl logs -f deployment/config-server-gke
    ```

4. Verify the pods are running:
    ```shell
    kubectl get pods
    ```

5. Verify the Services are running as well:
    ```shell
    kubectl get services
    ```

6. Access Discovery and Config servers using the `EXTERNAL_IP` of corresponding Services:
    ```shell
    curl http://CONFIG_SERVER_SERVICE_EXTERNAL_IP:8888/messenger/prod
    curl http://DISCOVERY_SERVER_SERVICE_EXTERNAL_IP:8761/
    ```

## Start Attachments and Messenger Services in GKE

Next, start an instance of the Attachments and Messenger microservices.

1. Ensure that you're in the `gcloud` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud
    ```
2. Start the microservices in GKE using the script below:
    ```shell
    ./start_spring_cloud_servers_in_gke.sh -r us-east4 -p YOUR_PROJECT_ID
    ```

    Replace the `YOUR_PROJECT_ID` placeholder with your Google project id.

3. Wait while all the deployments are ready:
    ```shell
    kubectl get deployments

    # Also, you can follow the logs of a spefic deployment for more details:
    kubectl logs -f deployment/config-server-gke
    ```

4. Verify the pods are running:
    ```shell
    kubectl get pods
    ```

5. Verify the Services are running as well:
    ```shell
    kubectl get services
    ```

6. Access Discovery and Config servers using the `EXTERNAL_IP` of corresponding Services:
    ```shell
    curl http://CONFIG_SERVER_SERVICE_EXTERNAL_IP:8888/messenger/prod
    curl http://DISCOVERY_SERVER_SERVICE_EXTERNAL_IP:8761/
    ```