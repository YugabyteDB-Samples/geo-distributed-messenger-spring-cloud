# Geo-Distributed Deployment in Google Cloud

The geo-messenger is designed to function across geogrpahies by definition. The following instruction shows how to deploy multiple application instances across several distant regions in Google Cloud. The regions are as follows - `us-west2`, `us-central1`, `us-east4`, `europe-west3` and `asia-east1`. You're free to follow the instruction precisely by deploy the application instances in all of those locations or skip some of them.

[YugabyteDB Managed](http://cloud.yugabyte.com) or self-managed YugabyteDB instance should be deployed in the regions similar to those selected for the application deployment. 

<!-- vscode-markdown-toc -->

- [Geo-Distributed Deployment in Google Cloud](#geo-distributed-deployment-in-google-cloud)
  - [Prerequisite](#prerequisite)
  - [Architecture](#architecture)
  - [Create Google Project](#create-google-project)
  - [Create Custom Network](#create-custom-network)
  - [Enable Google Cloud Storage](#enable-google-cloud-storage)
  - [Create Instance Templates](#create-instance-templates)
  - [Start Application Instances](#start-application-instances)
  - [Configure Global External Load Balancer](#configure-global-external-load-balancer)
  - [Test Load Balancer](#test-load-balancer)
  - [Play With Application](#play-with-application)
  - [Test Fault Tolerance](#test-fault-tolerance)
  
<!-- vscode-markdown-toc-config
    numbering=false
    autoSave=true
    /vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

## Prerequisite

* [Google Cloud](http://console.cloud.google.com/) account
* Multi-region [YugabyteDB Managed](http://cloud.yugabyte.com) cluster or a self-managed cluster

## Architecture

![google_cloud_deployment_architecture](https://user-images.githubusercontent.com/1537233/201388420-fcf4b8f9-5cbb-489c-91bd-ab43db7abc29.png)

The application and other components it depends on can function across multiple cloud regions. Check the [following article](https://dzone.com/articles/geo-what-a-quick-introduction-to-geo-distributed-a) for details on why this matters.

As the diagram above shows, you can deploy multiple instances of the Messaging, Attachments, Discovery, Config services in several cloud locations - `Region A -> Zone A`, `Region A -> Zone B` and `Region B -> Zone A`. Refer to the [architecture overview of the local deployment option](local_deployment.md) for details on how those services communicate.

YugabyteDB is deployed in a multi-region mode in the regions of choice. Google Cloud Storage runs across multiple locations as well and used to store pictures that the users share via the messenger.

The users connect to the Glogbal External Load Balancer that forwards their requests to an application instance that is closest to the user and healthy.

## Create Google Project

1. Navigate to the `gcloud` directory within the project structure:
    ```shell
    cd gcloud
    ```

2. Log in under your account:
    ```shell
    gcloud auth login
    ```

3. Create a new project for the app (use any other project name if `geo-distributed-messenger` is not available):
    ```shell
    gcloud projects create geo-distributed-messenger --name="Geo-Distributed Messenger"
    ```

4. Set this new project as default:
    ```shell
    gcloud config set project geo-distributed-messenger
    ```

5. Open Google Console and enable a billing account for the project: `https://console.cloud.google.com`

## Create Custom Network

1. Create the custom VPC network:
    ```shell
    gcloud compute networks create geo-messenger-network \
        --subnet-mode=custom
    ```

2. Create subnets in 3 regions of the USA:
    ```shell
    gcloud compute networks subnets create us-central-subnet \
        --network=geo-messenger-network \
        --range=10.1.10.0/24 \
        --region=us-central1
    
    gcloud compute networks subnets create us-west-subnet \
        --network=geo-messenger-network \
        --range=10.1.11.0/24 \
        --region=us-west2

    gcloud compute networks subnets create us-east-subnet \
        --network=geo-messenger-network \
        --range=10.1.12.0/24 \
        --region=us-east4
    ```
3. Create subnets in Asia and Europe:
    ```shell
    gcloud compute networks subnets create europe-west-subnet \
        --network=geo-messenger-network \
        --range=10.2.10.0/24 \
        --region=europe-west3

    gcloud compute networks subnets create asia-east-subnet \
        --network=geo-messenger-network \
        --range=10.3.10.0/24 \
        --region=asia-east1
    ```

3. Create a firewall rule to allow SSH connectivity to VMs within the VPC:
    ```shell
    gcloud compute firewall-rules create allow-ssh \
        --network=geo-messenger-network \
        --action=allow \
        --direction=INGRESS \
        --rules=tcp:22 \
        --target-tags=allow-ssh
    ```
    Note, allow to turn on the `compute.googleapis.com` persmission if requested.

4. Create the healthcheck rule to allow the global load balancer and Google Cloud health checks to communicate with backend instances on port `80` and `443`:
    ```shell
    gcloud compute firewall-rules create allow-health-check-and-proxy \
        --network=geo-messenger-network \
        --action=allow \
        --direction=ingress \
        --target-tags=allow-health-check \
        --source-ranges=130.211.0.0/22,35.191.0.0/16 \
        --rules=tcp:80
    ```
5. (Optional) for dev and testing purpose only, add IPs of your personal laptop and other machines that need to communicate to the backend on port `80` (note, you need to replace `0.0.0.0/0` with your IP):
    ```shell
    gcloud compute firewall-rules create allow-http-my-machines \
        --network=geo-messenger-network \
        --action=allow \
        --direction=ingress \
        --target-tags=allow-http-my-machines \
        --source-ranges=0.0.0.0/0 \
        --rules=tcp:80,tcp:8888,tcp:8761
    ```

## Enable Google Cloud Storage

The Attachments microservice uploads pictures to the [Google Cloud Storage](https://cloud.google.com/storage). Enable the service for this Google project.

## Create Instance Templates

Use the `gcloud/create_instance_template.sh` script to create instance templates for the US West, Central and East regions:
```shell
./create_instance_template.sh \
    -n {INSTANCE_TEMPLATE_NAME} \
    -i {PROJECT_ID} \
    -r {CLOUD_REGION_NAME} \
    -s {NETWORK_SUBNET_NAME} \
    -c {DB_CONNECTION_ENDPOINT} \
    -a {DB_ADDITIONAL_ENDPOINTS} \
    -u {DB_USER} \
    -p {DB_PWD} \
    -m {DB_MODE} \
    -f {DB_SCHEMA_FILE}
```
where
* `DB_CONNECTION_ENDPOINT` - is a YugabyteDB node IP address or DNS name
* `DB_ADDITIONAL_ENDPOINTS` - a comma-separated list of other YugabyteDB nodes to use in the connection pool. The format is `"node1_address:5433,node2_address:5433"`.
* `DB_MODE` can be set to one of these values:
    * 'standard' - the data source is connected to a standard/regular node. 
    * 'replica' - the connection goes via a replica node.
    * 'geo' - the data source is connected to a geo-partitioned cluster.
* `DB_SCHEMA_FILE` can be set to:
    * `classpath:messenger_schema.sql` - a basic database schema with NO tablespaces and partitions
    * `classpath:messenger_schema_partitioned.sql` - a schema with tablespaces belonging to specific cloud regions and geo-partitions.

1. Navigate to the folder with the script:
    ```shell
    cd {project_root_dir}/gcloud
    ```

2. Create a template for the US West, Central and East regions:
    ```shell
    ./create_instance_template.sh \
        -n template-us-west \
        -i geo-distributed-messenger \
        -r us-west2 \
        -s us-west-subnet \
        -c {DB_CONNECTION_ENDPOINT} \
        -a {DB_ADDITIONAL_ENDPOINTS} \
        -u {DB_USER} \
        -p {DB_PWD} \
        -m standard \
        -f "classpath:messenger_schema.sql"

    ./create_instance_template.sh \
        -n template-us-central \
        -i geo-distributed-messenger \
        -r us-central1 \
        -s us-central-subnet \
        -c {DB_CONNECTION_ENDPOINT} \
        -a {DB_ADDITIONAL_ENDPOINTS} \
        -u {DB_USER} \
        -p {DB_PWD} \
        -m standard \
        -f "classpath:messenger_schema.sql"

    ./create_instance_template.sh \
        -n template-us-east \
        -i geo-distributed-messenger \
        -r us-east4 \
        -s us-east-subnet \
        -c {DB_CONNECTION_ENDPOINT} \
        -a {DB_ADDITIONAL_ENDPOINTS} \
        -u {DB_USER} \
        -p {DB_PWD} \
        -m standard \
        -f "classpath:messenger_schema.sql"
    ```
3. Create a template for Europe:
    ```shell
    ./create_instance_template.sh \
        -n template-europe-west \
        -i geo-distributed-messenger \
        -r europe-west3 \
        -s europe-west-subnet \
        -c {DB_CONNECTION_ENDPOINT} \
        -a {DB_ADDITIONAL_ENDPOINTS} \
        -u {DB_USER} \
        -p {DB_PWD} \
        -m standard \
        -f "classpath:messenger_schema.sql"
    ```  
4. Create a template for Asia:
    ```shell
    ./create_instance_template.sh \
        -n template-asia-east \
        -i geo-distributed-messenger \
        -r asia-east1 \
        -s asia-east-subnet \
        -c {DB_CONNECTION_ENDPOINT} \
        -a {DB_ADDITIONAL_ENDPOINTS} \
        -u {DB_USER} \
        -p {DB_PWD} \
        -m standard \
        -f "classpath:messenger_schema.sql"
    ```  

## Start Application Instances

1. Start an application instance in every region:
    ```shell
    gcloud compute instance-groups managed create ig-us-west \
        --template=template-us-west --size=1 --zone=us-west2-b

    gcloud compute instance-groups managed create ig-us-central \
        --template=template-us-central --size=1 --zone=us-central1-b

    gcloud compute instance-groups managed create ig-us-east \
        --template=template-us-east --size=1 --zone=us-east4-b

    gcloud compute instance-groups managed create ig-europe-west \
        --template=template-europe-west --size=1 --zone=europe-west3-b

    gcloud compute instance-groups managed create ig-asia-east \
        --template=template-asia-east --size=1 --zone=asia-east1-b
    ```

2. (YugabyteDB Managed specific) Add VMs external IPs to the [IP Allow list](https://docs.yugabyte.com/preview/yugabyte-cloud/cloud-secure-clusters/add-connections/#assign-an-ip-allow-list-to-a-cluster).

3. Open Google Cloud Logging and wait while the VM finishes executing the `startup_script.sh` that sets up the environment and starts the application. It can take between 5-10 minutes.

    Alternatively, check the status from the terminal:
    ```shell
    # List all the instances
    gcloud compute instances list

    # Pick any instance
    gcloud compute ssh {INSTANCE_NAME}

    sudo journalctl -u google-startup-scripts.service -f
    ```

    The messenger Microservice starts the last and you should see the following ouput in the log that means the app is ready for usage:
    ```java
    google_metadata_script_runner[2767]: startup-script: Preloaded all Profiles to local cache
    ```

    Just in case, disregard the following warnings, they are harmless:
    ```java
    google_metadata_script_runner[2767]: startup-script: 2022-11-11 17:10:56.970  WARN 4475 --- [nnection thread] com.yugabyte.Driver : yb_servers() refresh failed in first attempt itself. Falling back to default behaviour
    google_metadata_script_runner[2767]: startup-script: 2022-11-11 17:10:56.970  WARN 4475 --- [nnection thread] com.yugabyte.Driver                      : Failed to apply load balance. Trying normal connection
    ``` 

4. Open the app by connecting to `http://{INSTANCE_EXTERNAL_IP}` and log in with `test@gmail.com`/`password`. Note, you can find the external address by running this command:
    ```shell
    gcloud compute instances list
    ```
    
5. Try out the app by sending a few messages and uploading a picture:
    ![Messenger](https://user-images.githubusercontent.com/1537233/201394810-b3d123db-4529-4a82-8d96-d367bb4ead1a.png)

## Configure Global External Load Balancer

Once the application instances are up and running, configure a global load balancer that will forward user requests to an instance closest to the user.

### Add Named Ports to Instance Groups

Set named ports for every instance group letting the load balancers know that the instances are capable of processing the HTTP requests on port `80`:

```shell
gcloud compute instance-groups unmanaged set-named-ports ig-us-west \
    --named-ports http:80 \
    --zone us-west2-b

gcloud compute instance-groups unmanaged set-named-ports ig-us-central \
    --named-ports http:80 \
    --zone us-central1-b

gcloud compute instance-groups unmanaged set-named-ports ig-us-east \
    --named-ports http:80 \
    --zone us-east4-b

gcloud compute instance-groups unmanaged set-named-ports ig-europe-west \
    --named-ports http:80 \
    --zone europe-west3-b

gcloud compute instance-groups unmanaged set-named-ports ig-asia-east \
    --named-ports http:80 \
    --zone asia-east1-b
```

### Reserve external IP addresses

Reserve IP addresses that application users will use to reach the load balancer:
    ```shell
    gcloud compute addresses create load-balancer-public-ip \
        --ip-version=IPV4 \
        --network-tier=PREMIUM \
        --global
    ```

### Configure Backend Service

1. Create a [health check](https://cloud.google.com/load-balancing/docs/health-checks) for application instances:
    ```shell
    gcloud compute health-checks create http load-balancer-http-basic-check \
        --check-interval=5s --timeout=3s \
        --healthy-threshold=2 --unhealthy-threshold=2 \
        --request-path=/login \
        --port 80
    ```

2. Create a [backend service](https://cloud.google.com/compute/docs/reference/latest/backendServices) that selects a VM instance for serving a particular user request:
    ```shell
    gcloud compute backend-services create load-balancer-backend-service \
        --load-balancing-scheme=EXTERNAL_MANAGED \
        --protocol=HTTP \
        --port-name=http \
        --health-checks=load-balancer-http-basic-check \
        --global
    ```
3. Add your instance groups as backends to the backend services:
    ```shell
    gcloud compute backend-services add-backend load-balancer-backend-service \
        --balancing-mode=UTILIZATION \
        --max-utilization=0.8 \
        --capacity-scaler=1 \
        --instance-group=ig-us-central \
        --instance-group-zone=us-central1-b \
        --global
    
    gcloud compute backend-services add-backend load-balancer-backend-service \
        --balancing-mode=UTILIZATION \
        --max-utilization=0.8 \
        --capacity-scaler=1 \
        --instance-group=ig-us-east \
        --instance-group-zone=us-east4-b \
        --global
    
    gcloud compute backend-services add-backend load-balancer-backend-service \
        --balancing-mode=UTILIZATION \
        --max-utilization=0.8 \
        --capacity-scaler=1 \
        --instance-group=ig-us-west \
        --instance-group-zone=us-west2-b \
        --global
    
    gcloud compute backend-services add-backend load-balancer-backend-service \
        --balancing-mode=UTILIZATION \
        --max-utilization=0.8 \
        --capacity-scaler=1 \
        --instance-group=ig-europe-west \
        --instance-group-zone=europe-west3-b \
        --global
    
    gcloud compute backend-services add-backend load-balancer-backend-service \
        --balancing-mode=UTILIZATION \
        --max-utilization=0.8 \
        --capacity-scaler=1 \
        --instance-group=ig-asia-east \
        --instance-group-zone=asia-east1-b \
        --global
    ```
4. Create a default URL map to route all the incoming requests to the created backend service (in practice, you can define backend services and URL maps for different microservices):
    ```shell
    gcloud compute url-maps create load-balancer-url-map --default-service load-balancer-backend-service
    ```

### Configure Frontend

Create a user-facing frontend (aka. HTTP(s) proxy) that receives requests and forwards them to the backend service:

1. Create a target HTTP proxy to route user requests to the backend's URL map:
    ```shell
    gcloud compute target-http-proxies create load-balancer-http-frontend \
        --url-map load-balancer-url-map \
        --global
    ```
2. Create a global forwarding rule to route incoming requests to the proxy:
    ```shell
    gcloud compute forwarding-rules create load-balancer-http-frontend-forwarding-rule \
        --load-balancing-scheme=EXTERNAL_MANAGED \
        --network-tier=PREMIUM \
        --address=load-balancer-public-ip  \
        --global \
        --target-http-proxy=load-balancer-http-frontend \
        --ports=80
    ```

After creating the global forwarding rule, it can take several minutes for your configuration to propagate worldwide.

Before proceeding to the next section, confirm the load balancer found all the backends (application instances) and that they are healty. You can find that on the Load Balancing page of `https://console.cloud.google.com`:
![Load Balancing](https://user-images.githubusercontent.com/1537233/201396406-7fefb9ca-d5d4-4b51-bf0b-04b9479e24b6.png)

## Test Load Balancer

1. Find the public IP addresses of the load balancer:
    ```shell
    gcloud compute addresses describe load-balancer-public-ip \
        --format="get(address)" \
        --global
    ```

2. Send a request through the load balancer:
    ```shell
    curl -v http://{LOAD_BALANCER_PUBLIC_IP}
    ```
    Note, it can take several minutes before the load balancer's settings get propogated globally. Until this happens, `curl` will return the following:
    
    ```shell
    ....
    > User-Agent: curl/7.79.1
    > Accept: */*
    > 
    * Empty reply from server
    * Closing connection 0
    curl: (52) Empty reply from server
    ```
    
    Once the load balancer is configured, `curl` will print the following message:
    ```shell
    < HTTP/1.1 302 Found
    < set-cookie: JSESSIONID=52E1989E74BF383666D9AAFFBA63CD0F; Path=/; HttpOnly
    < x-content-type-options: nosniff
    < x-xss-protection: 1; mode=block
    < cache-control: no-cache, no-store, max-age=0, must-revalidate
    < pragma: no-cache
    < expires: 0
    < x-frame-options: DENY
    < location: http://{YOUR_LOAD_BALANCER_IP}/login
    < Content-Length: 0
    < date: Fri, 11 Nov 2022 17:31:53 GMT
    < via: 1.1 google
    < 
    * Connection #0 to host {YOUR_LOAD_BALANCER_IP} left intact
    ```

## Play With Application

Once the cloud load balancer is ready, use its IP address to access the application from the browser.

1. Open the load balancer's address `http://{LOAD_BALANCER_PUBLIC_IP}` in your browser and log in using `test@gmail.com`/`password`. 

2. Send a few messages in the app and upload a picture:
    ![Messages](https://user-images.githubusercontent.com/1537233/201398523-7dae24bc-ab42-48f0-b72f-668070e23533.png)

The load balancer will forward your request to the nearest application instance. To determine your current instance:

1. Go to the `Load Balancing`page of `https://console.cloud.google.com` and open the `Monitoring` tab.

2. Explore the diagram like the one below. In my case, the nearest location was in the US East region `ig-us-east`. Yours might be different depending on where you are in the world:
    ![Screen Shot 2022-11-11 at 12 44 28 PM](https://user-images.githubusercontent.com/1537233/201398951-fa07b0fc-ec5a-4e89-8034-8c8c6a959152.png)
  
## Test Fault Tolerance

If the application instance, that is closest to you, becomes unhealthy the load balancer will automatically forward requests to another instance.

You can simulate an outage by stopping an instance of the Messenger microservice in the location that serves your request: 

1. Get a list of all VMs:
    ```shell
    gcloud compute instances list
    ```
2. Connect to the VM where the load balancer forwards your requests (in my case, that's an instace from the `ig-us-east` group):
    ```shell
    gcloud compute ssh {INSTANCE_NAME}
    ```
3. Switch to `root`:
    ```shell
    sudo su
    ```
4. Find the PID of the Messenger microservice (it listens on port `80`):
    ```shell
    fuser 80/tcp
    ```
5. Stop the instance:
    ```shell
    kill -9 {PID}
    ```

The load balancer runs a healthcheck every 5s (configured earlier in this instruction) and will detect the outage shortly:

1. Open the `http://{LOAD_BALANCER_PUBLIC_IP}` in your browser again

2. The load balancer will redirect your requests to a different application instance. In my case, that was an instance from the `ig-europe-west` region:
    ![Another region](https://user-images.githubusercontent.com/1537233/201405913-b716f35b-1966-4236-bb54-0607a0d4b78a.png)




