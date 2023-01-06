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

1. Create a repository for Docker images in the `us-east4` region:
```shell
gcloud artifacts repositories create geo-distributed-messenger-repo \
    --repository-format=docker \
    --location=us-east4 \
    --description="Docker repository for geo-distributed messenger containers"
```

2. Also, create and store the images in the `europe-west1` region:
    ```shell
    gcloud artifacts repositories create geo-distributed-messenger-repo \
        --repository-format=docker \
        --location=europe-west1 \
        --description="Docker repository for geo-distributed messenger containers"
    ```

## Prepare App Images

Build a Docker image for each application microservice and store the images in selected cloud regions.

1. Navigate to the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Build and submit images to the `us-east4` region:
    ```shell
    ./build_docker_images.sh \
        -r us-east4
    ```

3. Repeate the build process for the `europe-west1` region:
    ```shell
    ./build_docker_images.sh \
        -r europe-west1
    ```

## Enable Anthos Pricing

In the guide below, you'll deploy multiple kubernetes clusters that can be accessed via the Multi Region Ingress. 
The ingress will be managed as part of the [Anthos platform](https://cloud.google.com/anthos).

Enable the Anthos pricing for you project:
```shell
gcloud services enable \
    anthos.googleapis.com \
    multiclusteringress.googleapis.com \
    gkehub.googleapis.com \
    container.googleapis.com \
    multiclusterservicediscovery.googleapis.com
```

## Create Service Account

Create a service account that will be used by Kubernetes workloads.

1. Make sure you're in the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Create the service account named `geo-messenger-sa`:
    ```shell
    ./create_gke_service_account.sh -n geo-messenger-sa
    ```

## Start GKE Clusters

Start two GKE clusters in distant cluster locations and register them with the fleet:

1. Make sure you're in the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Start the first cluster in the `us-east4` region:
    ```shell
    ./start_gke_cluster.sh \
        -r us-east4 \
        -n gke-us-east4 \
        -s geo-messenger-sa \
        -a geo-messenger-k8-sa
    ```

    the arguments are:
    * `-r` - the name of a cloud region
    * `-n` - the name of the GKE cluster that will be created by the script
    * `-s` - the name of the IAM service account (created earlier)
    * `-a` - the name of the Kubernetes service account (created by the script) 

3. Start the second cluster in the `europe-west1` region:
    ```shell
    ./start_gke_cluster.sh \
        -r europe-west1 \
        -n gke-europe-west1 \
        -s geo-messenger-sa \
        -a geo-messenger-k8-sa
    ```

4. Verify that both clusters are registered with an Anthos fleet:
    ```shell
    gcloud container fleet memberships list
    ```

5. Enable [Multi Cluster Ingress](https://cloud.google.com/kubernetes-engine/docs/concepts/multi-cluster-ingress) and select `gke-us-east4` as the config cluster:
    ```shell
    gcloud container fleet ingress enable \
        --config-membership=gke-us-east4
    ```

Now, you can open the [Anthos](https://cloud.google.com/anthos) dashboard to observe the clusters and Ingress.

## Start Application

Start an instance of Spring Cloud Config Server, Attachments and Messenger in every GKE cluster.

1. Make sure you're in the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Start the application in the `gke-us-east4` cluster:
    ```shell
    ./start_gke_app.sh \
        -r us-east4 \
        -n gke-us-east4 \
        -a geo-messenger-k8-sa
    ```

    the arguments are:
    * `-r` - the name of the cluster's cloud region
    * `-n` - the cluster name
    * `-a` - the name of the Kubernetes service account

3. Start the app in the `gke-europe-west1` cluster:
    ```shell
    ./start_gke_app.sh \
        -r europe-west1 \
        -n gke-europe-west1 \
        -a geo-messenger-k8-sa
    ```

It will take several minutes to deploy the application. You can monitor the deployment status using the following commands or [GKE Dashboard](https://cloud.google.com/kubernetes-engine).

1. First, select one of the clusters:
    ```shell
    kubectl config use-context gke-us-east4
    # or
    kubectl config use-context gke-europe-west1
    ```

2. Get the deployment status:
    ```shell
    kubectl get deployments --namespace geo-messenger

    # Or, view logs of a particular microservice:
    kubectl logs -f deployment/config-server-gke --namespace geo-messenger
    kubectl logs -f deployment/attachments-gke --namespace geo-messenger
    kubectl logs -f deployment/messenger-gke --namespace geo-messenger
    ```

3. Once the deployments are ready, check the pods and services status: 
    ```shell
    kubectl get pods --namespace geo-messenger
    kubectl get services --namespace geo-messenger
    ```


Lastly, you can connect a Messenger instance directly from any cloud region.

1. First, select one of the clusters:
    ```shell
    kubectl config use-context gke-us-east4
    # or
    kubectl config use-context gke-europe-west1
    ```

2. Find the EXTERNAL_IP of the respective Kubernetes service:
    ```shell
    kubectl get service messenger-service --namespace geo-messenger
    ```

3. Open the address in the browser:
    ```shell
    http://EXTERNAL_IP/
    ```

    use the `test@gmail.com\password` credentials to log in.

## Deploy Multi Cluster Ingress and Service

With the application running across two distant GKE clusters, you can proceed with the configuration of the multi cluster Ingress and Service. 
The configuration has to happen via the `gke-us-east4` cluster that was selected as a config cluster earlier.

1. Make sure you're in the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Set the context to the config cluster:
    ```shell
    kubectl config use-context gke-us-east4
    ```

3. Start the mutli cluster service:
    ```shell
    kubectl apply -f multi-cluster-service.yaml
    ```

4. Verify the service is started:
    ```shell
    kubectl get mcs --namespace geo-messenger
    ```

5. This multi cluster service creates a derived headless Service in every cluster that matches Pods with `app: messenger`:
    ```shell
    kubectl get service --namespace geo-messenger
    ```

Next, deploy a multi cluster Ingress:

1. Deploy the Ingress to the config cluster:
    ```shell
    kubectl apply -f multi-cluster-ingress.yaml
    ```

2. Verify the deployment has succeeded:
    ```shell
    kubectl describe mci geo-messenger-ingress --namespace geo-messenger
    ```

3. Keep executing the previous command until you see the `VIP:` parameter set to a static IP address like this one below:
    ```shell
    VIP:        34.110.218.170
    ```

4. It can take 10+ minutes for the IP address to be fully ready for usage. Once the IP is ready, you'll see the following ouput for the following API call:
    ```shell
    curl http://VIP/login
    ```
