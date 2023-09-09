# Deploying Geo-Distributed Applications on Google Kubernetes Engine with YugabyteDB

This guide shows how to deploy a geo-distributed version of the messenger across five Google Cloud regions in several countries.
The application instances are deployed to Google Kubernetes Engine. YugabyteDB is configured in a geo-distributed mode and functions as a system-of-records. Google Cloud Storage is used as a distributed storage for application attachments.

<!-- vscode-markdown-toc -->

- [Deploying Geo-Distributed Applications on Google Kubernetes Engine with YugabyteDB](#deploying-geo-distributed-applications-on-google-kubernetes-engine-with-yugabytedb)
  - [Prerequisite](#prerequisite)
  - [Create Google Cloud Project](#create-google-cloud-project)
  - [Enable Anthos Pricing](#enable-anthos-pricing)
  - [Create Service Account](#create-service-account)
  - [Start GKE Cluster](#start-gke-clusters)
  - [Enable Google Cloud Storage](#enable-google-cloud-storage)
  - [Deploy YugabyteDB Managed Instance](#deploy-yugabytedb-managed-instance)
  - [Create Artifact Repositories](#create-artifact-repositories)
  - [Prepare Docker Images](#prepare-docker-images)
  - [Start Application](#start-application)
  - [Deploy Multi Cluster Ingress](#deploy-multi-cluster-ingress)
  - [Playing with the App](#playing-with-the-app)
  - [Clean Project](#clean-project)
  
<!-- vscode-markdown-toc-config
    numbering=false
    autoSave=true
    /vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

## Prerequisite 

* [Google Cloud Platform](http://console.cloud.google.com/) account.

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

## Enable Anthos Pricing

The application instances will be deployed in Google Kubernetes Engine (GKE) clusters. The Multi Region Ingress will forward user traffic to a spefic GKE cluster. The ingress will be managed as part of the [Anthos platform](https://cloud.google.com/anthos).

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

Create a service account that will be used by GKE workloads.

1. Make sure you're in the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Create the service account named `geo-messenger-sa`:
    ```shell
    ./create_gke_service_account.sh -n geo-messenger-sa
    ```

## Start GKE Clusters

Start GKE clusters in all five cloud locations:

1. Make sure you're in the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Start the first three clusters in North America:
    ```shell
    ./start_gke_cluster.sh \
        -r us-west1 \
        -n gke-us-west1 \
        -s geo-messenger-sa \
        -a geo-messenger-k8-sa

    ./start_gke_cluster.sh \
        -r us-central1 \
        -n gke-us-central1 \
        -s geo-messenger-sa \
        -a geo-messenger-k8-sa
    
    ./start_gke_cluster.sh \
        -r us-east1 \
        -n gke-us-east1 \
        -s geo-messenger-sa \
        -a geo-messenger-k8-sa
    ```

    the arguments are:
    * `-r` - the name of a cloud region
    * `-n` - the name of the GKE cluster that will be created by the script
    * `-s` - the name of the IAM service account (created earlier)
    * `-a` - the name of the Kubernetes service account (created by the script) 

3. Start two remaining clusters in Europe and Asia:
    ```shell
    ./start_gke_cluster.sh \
        -r europe-west3 \
        -n gke-europe-west3 \
        -s geo-messenger-sa \
        -a geo-messenger-k8-sa
    
    ./start_gke_cluster.sh \
        -r asia-east1 \
        -n gke-asia-east1 \
        -s geo-messenger-sa \
        -a geo-messenger-k8-sa
    ```

4. Verify that all the clusters are registered with an Anthos fleet:
    ```shell
    gcloud container fleet memberships list
    ```

5. Enable [Multi Cluster Ingress](https://cloud.google.com/kubernetes-engine/docs/concepts/multi-cluster-ingress) and select `gke-us-east1` as the config cluster:
    ```shell
    gcloud container fleet ingress enable \
        --location=us-east1 --config-membership=gke-us-east1
    ```

Now, you can open the [Anthos](https://cloud.google.com/anthos) dashboard to observe the clusters and Ingress.

## Enable Google Cloud Storage

The Attachments microservice uploads pictures to [Google Cloud Storage](https://cloud.google.com/storage). Enable the service for tyour Google Project.

## Deploy YugabyteDB Managed Instance

YugabyteDB will be deployed in a geo-partitioned mode, placing the database nodes in all the regions where the GKE clusters are already located.

1. [Create](https://cloud.yugabyte.com/login) a YugabyteDB Managed account.

3. [Create a VPC network](https://docs.yugabyte.com/preview/yugabyte-cloud/cloud-basics/cloud-vpcs/cloud-add-vpc-gcp/) under your Google account. Enable five regions - `us-west1, us-central1, us-east1, europe-west3, asia-east1`

2. [Deploy a geo-partitioned cluster](https://docs.yugabyte.com/preview/yugabyte-cloud/cloud-basics/create-clusters/create-clusters-geopartition/#create-a-partition-by-region-cluster) with the cluster nodes in five cloud locations - `us-west1, us-central1, us-east1, europe-west3, asia-east1`:
    * Use the VPC network created earlier
    * Label the `us-east1` region as a primary one
    * For testing purpose, you can have a single node per region or three nodes per region.

3. [Allow access](https://docs.yugabyte.com/preview/yugabyte-cloud/cloud-secure-clusters/add-connections/) to the cluster from the IP addresses used by the GKE cluster.


## Create Artifact Repositories

The application instances will be deployed in the following five cloud regions:
* Three regions are in North America — `us-west1`, `us-central1` and `us-east1`.
* One is in Europe — `europe-west3`.
* And the last one is in Asia - `asia-east1`

First, create artifact repositories in all the cloud regions where the application instances will be deployed. Use [Artifact Registry](https://cloud.google.com/artifact-registry) for that:
1. Create artifact repositories in North America:
    ```shell
    gcloud artifacts repositories create geo-distributed-messenger-repo \
        --repository-format=docker \
        --location=us-west1 \
        --description="Docker repository for geo-distributed messenger containers"

    gcloud artifacts repositories create geo-distributed-messenger-repo \
        --repository-format=docker \
        --location=us-central1 \
        --description="Docker repository for geo-distributed messenger containers"

    gcloud artifacts repositories create geo-distributed-messenger-repo \
        --repository-format=docker \
        --location=us-east1 \
        --description="Docker repository for geo-distributed messenger containers"
    ```

2. Create repositories in Europe and Asia:
    ```shell
    gcloud artifacts repositories create geo-distributed-messenger-repo \
        --repository-format=docker \
        --location=europe-west3 \
        --description="Docker repository for geo-distributed messenger containers"

    gcloud artifacts repositories create geo-distributed-messenger-repo \
        --repository-format=docker \
        --location=asia-east1 \
        --description="Docker repository for geo-distributed messenger containers"
    ```

## Prepare Docker Images

Build a Docker image for each application microservice and load the images to the selected cloud regions.

1. Navigate to the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Build and load the images to the regions in North America:
    ```shell
    ./build_docker_images.sh \
        -r us-west1
    
    ./build_docker_images.sh \
        -r us-central1

    ./build_docker_images.sh \
        -r us-east1
    ```

3. Repeate the process for the regions in Europe and Asia:
    ```shell
    ./build_docker_images.sh \
        -r europe-west3

    ./build_docker_images.sh \
        -r asia-east1
    ```
## Start Application

Start an instance of Spring Cloud Config Server, Attachments and Messenger in every GKE location.

1. Make sure you're in the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Create a copy of the `PROJECT_ROO_DIR/messenger/secret-gke-template.yaml` file and name the copy as `PROJECT_ROO_DIR/messenger/secret-gke.yaml`

3. Open the `PROJECT_ROO_DIR/messenger/secret-gke.yaml` and provide connectivity settings for your YugabyteDB Managed instance. Don't forget to update the [IP Allow List](https://docs.yugabyte.com/preview/yugabyte-cloud/cloud-secure-clusters/add-connections/) on the YugabyteDB Managed side. For development and learning, you can use the range `0.0.0.0/0` to allow connections from any GKE pod.

4. Start the application instances in North America:
    ```shell
    ./start_gke_app.sh \
        -r us-west1 \
        -n gke-us-west1 \
        -a geo-messenger-k8-sa \
        -s "classpath:messenger_schema_partitioned_rf_3.sql"
    
    ./start_gke_app.sh \
        -r us-central1 \
        -n gke-us-central1 \
        -a geo-messenger-k8-sa \
        -s "classpath:messenger_schema_partitioned_rf_3.sql"
    
    ./start_gke_app.sh \
        -r us-east1 \
        -n gke-us-east1 \
        -a geo-messenger-k8-sa \
        -s "classpath:messenger_schema_partitioned_rf_3.sql"
    ```

    the arguments are:
    * `-r` - the name of the cluster's cloud region
    * `-n` - the cluster name
    * `-a` - the name of the Kubernetes service account
    * `-s` - a database schema name. Use `classpath:messenger_schema_partitioned_rf_3.sql`.

5. Start the app instances in Europe and Asia:
    ```shell
    ./start_gke_app.sh \
        -r europe-west3 \
        -n gke-europe-west3 \
        -a geo-messenger-k8-sa \
        -s "classpath:messenger_schema_partitioned_rf_3.sql"

    ./start_gke_app.sh \
        -r asia-east1 \
        -n gke-asia-east1 \
        -a geo-messenger-k8-sa \
        -s "classpath:messenger_schema_partitioned_rf_3.sql"
    ```

It will take several minutes to deploy the application instances. You can monitor the deployment status using the following commands or [GKE Dashboard](https://cloud.google.com/kubernetes-engine).

1. First, select one of the clusters by switching between Kubernetes contexts:
    ```shell
    kubectl config use-context gke-us-east1
    # or
    kubectl config use-context gke-europe-west3
    ```

2. Get the deployment status:
    ```shell
    kubectl get deployments

    # Or, view logs of a particular microservice:
    kubectl logs -f deployment/config-server-gke
    kubectl logs -f deployment/attachments-gke
    kubectl logs -f deployment/messenger-gke
    ```

3. Once the deployments are ready, check the pods and services status: 
    ```shell
    kubectl get pods
    kubectl get services
    ```

Lastly, you can connect to a Messenger instance directly from any cloud region.

1. First, select one of the clusters:
    ```shell
    kubectl config use-context gke-us-east1
    # or
    kubectl config use-context gke-europe-west3
    ```

2. Find the EXTERNAL_IP of the respective Kubernetes service:
    ```shell
    kubectl get service messenger-service
    ```

3. Open the address in the browser and send a few messeges and pictures:
    ```shell
    http://EXTERNAL_IP/
    ```

    use the `test@gmail.com\password` credentials to log in.

![messenger_view](https://user-images.githubusercontent.com/1537233/211407944-c50ae7af-20d4-4f90-9753-d2379f9290df.png)


## Deploy Multi Cluster Ingress

With the application running across five standalone GKE clusters, you can proceed with the [configuration of the Multi Cluster Ingress](https://cloud.google.com/kubernetes-engine/docs/how-to/multi-cluster-ingress) and Service. The Ingress needs to be configured via the config cluster - the `gke-us-east1` one.

1. Make sure you're in the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```
2. Deploy multi cluster service and ingress:
    ```shell
    ./deploy_multi_cluster_ingress.sh -n gke-us-east1
    ```

    where `-n` is the name of the config cluster.

3. The multi cluster service creates a derived headless Service (might take several minutes) in every cluster that matches pods with `app: messenger`:
    ```shell
    kubectl get service

    # the name of the service should look as follows
    mci-geo-messenger-mcs-svc-d3tnpay37ltoop2o
    ```

4. Verify the deployment has succeeded:
    ```shell
    kubectl describe mci geo-messenger-ingress | grep VIP
    ```

5. Keep executing the previous command until you see the `VIP:` parameter set to a static IP address like this one below:
    ```shell
    VIP:        34.110.218.170
    ```

6. It can take 10+ minutes for the IP address to be ready for usage. You can see various errors while the IP is being configured. Keep checking the IP readiness using this call:
    ```shell
    curl http://VIP/login

    # once the IP is ready, you'll get an HTML page that starts with:
    <!doctype html><html lang="en"><head><script initial="">window.Vaadin = window.Vaadin || {};window.Vaadin.TypeScript= {};
    ```

Finally, open the VIP address in the browser!
http://VIP/


## Playing With the App

1. Open the app in the browser using the VIP address of the Multi Cluster Ingress.

2. Log in using `test@gmail.com\password` account

3. Send a few messages and pictures in any channel.

4. Go to the GCP "Load Balancing" page and select the load balancer which name starts with `mci-`

5. Open the "Monitoring" tab and confirm the load balancer forwarded app requests to a GKE cluster that is closest to your physical location (in my case, that's `gke-us-east4`).

![us-only-traffic](https://user-images.githubusercontent.com/1537233/211407459-83372922-3cd1-41a2-95ac-819f6c823989.png)

## Clean Project

1. Go to the `gcloud/gke` directory of the project:
    ```shell
    cd PROJECT_ROO_DIR/gcloud/gke
    ```

2. Run the script that removes GKE clusters for each used cloud region:
    ```shell
    ./clean_gke.sh \
        -n gke-us-west1 \
        -r us-west1
    
    ./clean_gke.sh \
        -n gke-us-central1 \
        -r us-central1
    
    ./clean_gke.sh \
        -n gke-us-east1 \
        -r us-east1

    ./clean_gke.sh \
        -n gke-europe-west3 \
        -r europe-west3
    
    ./clean_gke.sh \
        -n gke-asia-east1 \
        -r us-asia-east1
    ```

    the arguments are:
    * `-r` - the name of the cluster's cloud region
    * `-n` - the cluster name
    
3. Use Google Console (or respective gcloud commands) to stop the MCI load balancer (on the "Load Balancing" page), service account and project.

