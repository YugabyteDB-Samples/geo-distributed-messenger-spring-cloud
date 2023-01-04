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

## Store App Containers in Artifact Registry

First, you need to create a Docker container for each application microservice and load the container
to [Artifact Registry](https://cloud.google.com/artifact-registry).

### Create Docker Repository

Create a repository for Docker images in the registry:
```shell
gcloud artifacts repositories create geo-distributed-messenger-repo \
    --repository-format=docker \
    --location=us-east4 \
    --description="Docker repository for geo-distributed messenger containers"
```

### Build and Load App Containers

1. Build a Config Server's docker image using [Cloud Build](https://cloud.google.com/build):
    ```shell
    cd config-server

    gcloud builds submit \
        --tag us-east4-docker.pkg.dev/PROJECT_ID/geo-distributed-messenger-repo/config-server-gke .
    ```
    
    Replace `PROJECT_ID` with your project created earlier.

2. Build a Discover Service's image:
    ```shell
    cd ../discovery-server

    gcloud builds submit \
        --tag us-east4-docker.pkg.dev/PROJECT_ID/geo-distributed-messenger-repo/discovery-server-gke .
    ```

3. Build an Attachment's microservice image:
    ```shell
    cd ../attachments

    gcloud builds submit \
        --tag us-east4-docker.pkg.dev/PROJECT_ID/geo-distributed-messenger-repo/attachments-gke .
    ```
4. Finally, build the last image for the Messenger microservice:
    ```shell
    cd ../messenger

    gcloud builds submit \
        --tag us-east4-docker.pkg.dev/PROJECT_ID/geo-distributed-messenger-repo/messenger-gke .
    ```        
## Create a GKE Cluster

1. Create a Kubernetes cluster within the `us-east4` region:
    ```shell
    gcloud container clusters create-auto geo-distributed-messenger-gke \
        --region us-east4
    ```

2. Verify you have access to the cluster by getting a list of running nodes:
    ```shell
    kubectl get nodes
    ```

## Deploy App to GKE

1. Navigate to the Config Server's directory and prepare GKE deployment files:
    ```shell
    cd PROJECT_ROO_DIR/config-server

    # Substitute the `PROJECT_ID` placeholder with your Google Project ID first:
    cat deployment-gke-template.yaml | sed 's/REGION/us-east4/g' | sed 's/GOOGLE_PROJECT_ID/PROJECT_ID/g' > deployment-gke.yaml
    ```

2. Deploy a server's pod:
    ```shell
    kubectl apply -f deployment-gke.yaml
    ```

3. Track the status of the deployment:
    ```shell
    kubectl get deployments config-server-gke

    # Also, you can follow the logs for more details:
    kubectl logs -f deployment/config-server-gke
    ```

4. Once the deployment is finished, check the status of the pod:
    ```shell
    kubectl get pods --selector=app=config-server
    ```
5. Deploy a Kubernetes service for the Config Server's pods:
    ```shell
    kubectl apply -f service-gke.yaml
    ```
6. Get the external IP address of the service:
    ```shell
    kubectl get service config-server-service
    ```

    It can take up to 60 seconds to allocate the IP address.

7. Make sure you can query the Config Server from your laptop by loading the Messenger's microservice configuration:
    ```shell
    curl http://34.86.153.141:8888/messenger/dev
    ```