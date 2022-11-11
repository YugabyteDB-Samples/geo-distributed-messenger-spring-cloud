# Local Application Deployment

Follow this instruction if you wish to run the entire application with all the components on your local laptop or on-premise environment.

<!-- vscode-markdown-toc -->

- [Local Application Deployment](#local-application-deployment)
  - [Prerequisite](#prerequisite)
  - [Architecture](#architecture)
  - [Create Custom Docker Network](#create-custom-docker-network)
  - [Start YugabyteDB](#start-yugabyteDB)
  - [Start Minio](#start-minio)
  - [Start Config Server](#start-config-server)
  - [Start Discovery Server](#start-discovery-server)
  - [Start Attachments Microservice](#start-attachments-microservice)
  - [Start Messenger Microservice](#start-messenger-microservice)
  - [Clean Resources](#clean-resources)

<!-- vscode-markdown-toc-config
    numbering=false
    autoSave=true
    /vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

## Prerequisite

* Java 17+
* Spring Cloud CLI
* Maven 3.8.4+
* Docker 20.10.12+

## Architecture

![local_deployment_architecture](https://user-images.githubusercontent.com/1537233/201354805-e0c67da5-d5d7-4786-bb2c-70f237ec833a.png)

The application logic is shared between two microservices.

The main Messenger microservice supports basic messaging capabilities - exchanging messages within various channels. The microservice stores application data (workspaces, users, channels, messages, etc.) in a multi-node YugabyteDB database.

The second Attachments microservice is responsible for storing using pictures (attachements) in MinIO. MinIO is an S3-compliant object store.

Both microservices load configuration settings from the Spring Cloud Config Server. The Config Server stores configurations in the following GitHub repository: https://github.com/YugabyteDB-Samples/geo-distributed-messenger-config-repo

Upon startup, both microservices register with the Spring Cloud Discovery Server. The Discovery Server serves as an API/communication layer. When the Messenger service needs to upload a picture, it discovers an instance of the Attachments microservice via the Discovery Server and sends a request to that instance.

For the sake of simplicity, YugabyteDB and MinIO are deployed in Docker and the other components are started on your host machine as standalone Java applications.

## Create Custom Docker Network

YugabyteDB and MinIO will be running in Docker containers. 

Create a custom network for them:
```shell
docker network create geo-messenger-net
```

## Start YugabyteDB

1. Start a three-node YugabyteDB cluster:
    ```shell
    rm -R ~/yb_docker_data
    mkdir ~/yb_docker_data

    docker run -d --name yugabytedb_node1 --net geo-messenger-net \
    -p 7001:7000 -p 5433:5433 \
    -v ~/yb_docker_data/node1:/home/yugabyte/yb_data --restart unless-stopped \
    yugabytedb/yugabyte:2.15.3.0-b231 \
    bin/yugabyted start --listen=yugabytedb_node1 \
    --base_dir=/home/yugabyte/yb_data --daemon=false

    docker run -d --name yugabytedb_node2 --net geo-messenger-net \
    -v ~/yb_docker_data/node2:/home/yugabyte/yb_data --restart unless-stopped \
    yugabytedb/yugabyte:2.15.3.0-b231 \
    bin/yugabyted start --listen=yugabytedb_node2 --join=yugabytedb_node1 \
    --base_dir=/home/yugabyte/yb_data --daemon=false


    docker run -d --name yugabytedb_node3 --net geo-messenger-net \
    -v ~/yb_docker_data/node3:/home/yugabyte/yb_data --restart unless-stopped \
    yugabytedb/yugabyte:2.15.3.0-b231 \
    bin/yugabyted start --listen=yugabytedb_node3 --join=yugabytedb_node1 \
    --base_dir=/home/yugabyte/yb_data --daemon=false
    ```

2. Confirm the instance is running:
    http://127.0.0.1:7001

    ![YugabyteDB Cluster](https://user-images.githubusercontent.com/1537233/201357005-7ddf853f-c501-422f-a675-61527e6ec214.png)


## Start MinIO

[Minio](https://min.io) is used in local deployments as an object store for pictures that are loaded via the Attachments microservice. 

1. Start the Minio service in:
    ```shell
    rm -R ~/minio/data
    mkdir -p ~/minio/data

    docker run -d \
    --net geo-messenger-net \
    -p 9000:9000 \
    -p 9001:9001 \
    --name minio1 \
    -v ~/minio/data:/data \
    -e "MINIO_ROOT_USER=minio_user" \
    -e "MINIO_ROOT_PASSWORD=password" \
    quay.io/minio/minio:RELEASE.2022-08-26T19-53-15Z server /data --console-address ":9001"
    ```

2. Open the Minio console and log in using the following credentials - `minio_user` / `password`:
    http://127.0.0.1:9001

    ![MinIO Console](https://user-images.githubusercontent.com/1537233/201357361-ec2c6535-865f-471c-968d-b339fd6bc0b4.png)


## Start Config Server

Spring Cloud Config Server stores configurations in the following public repository:
https://github.com/YugabyteDB-Samples/geo-distributed-messenger-config-repo

1. Start the config server:
    ```shell
    cd {project-root-dir}/config-server

    mvn spring-boot:run
    ```
    Alternatively, you can start the app from your IDE of choice. Just launch the `ConfigServerApplication.java` file.

2. Confirm the server is running by requesting a development configuration for the Messenger microservice:
    http://localhost:8888/messenger/dev

The Config Server clones the configuration repository into the `$HOME/messenger-config` directory. You can override this settings in the server's `application.properties` file.

Presently, the repository includes the following configurations:
* `messenger-dev.properties` - development configuration of the Messenger microservice for local testing (used by default)
* `messenger-prod.properties` - prod configuration of the Messenger microservice, requires to provide several settings via environment variables. Activated with the `-Pprod` maven profile.
* `attachments-dev.properties` - development configuration of the Attachments microservice for local testing (used by default)
* `attachments-prod.properties` - prod configuration of the Attachements microservice, requires to provide several settings via environment variables. Activated by the `-Pprod` maven profile.

## Start Discovery Server

Spring Cloud Discovery Server allows microservices to locate each other easily in a distributed environment. Both Messenger and Attachments microservices register with the Discovery Server. The Attachments service exposes a REST endpoint used by the Messaging one for pictures uploading.

1. Start the discovery server:
    ```shell
    cd {project-root-dir}/discovery-server

    mvn spring-boot:run
    ```
    Alternatively, you can start the app from your IDE of choice. Just launch the `DiscoveryServerApplication.java` file.

2. Confirm the server is running by opening the following address:
    http://localhost:8761
    ![Discovery Server](https://user-images.githubusercontent.com/1537233/201358246-b5c2710b-b75b-418c-ac95-efbf5122a41a.png)

## Start Attachments Microservice

Once the Config and Discover Servers are started, launch the Attachments microservice.

1. Navigate to the microservice directory:
    ```shell
    cd {project-root-dir}/attachments 
    ```

2. Start the service:
    ```shell
    mvn spring-boot:run
    ```
    Alternatively, you can start the app from your IDE. Just run the `AttachmentsApplication.java` class.

The service will start listening on `http://localhost:8081/` for incoming requests.

## Start Messenger Microservice

Once the Config and Discover Servers are started, also start the Messenger microservice.

1. Start the microservice:
    ```shell
    cd {project-root-dir}/messenger
    mvn spring-boot:run
    ```
    Alternatively, you can start the app from your IDE of choice. Just boot the `Application.java` file.

2. Wait while the database is preloaded with the sample data. Check the log for the following output:
    ```java
    INFO 90053 --- [  restartedMain] c.y.a.m.data.generator.DataGenerator     : Generating Messages
    INFO 90053 --- [  restartedMain] c.y.a.m.data.generator.DataGenerator     : Finished data generation
    Preloaded all Profiles to local cache
    ```

3. Open http://localhost:8080 in your browser (if it's not opened automatically) and log in using the following credentials:
    ```shell
    username: test@gmail.com
    pwd: password
    ```

4. Send a few messages and try to upload a picture:
    ![Messenger](https://user-images.githubusercontent.com/1537233/201361113-b21d2095-d7c9-4c20-b4db-d595aa4eb79c.png)


Note, during the first launch, it might take several minutes for Vaadin to compile the frontend. During this time the http://localhost:8080, should show the following message:
![Vaadin compiling](https://user-images.githubusercontent.com/1537233/201361833-6c303714-d334-40c7-a6ad-578f4b989d07.png)

Enjoy and make sure to check the source code! 

Next, try out the [geo-distributed deployment option](gcloud_deployment.md) of the application that spans accross countries and continents in Google Cloud.

## Clean Resources

If you're done working with the app, then use these command to remove Docker containers and other resources associated with them:

```shell
docker kill minio1
docker container rm minio1

docker kill yugabytedb_node1
docker container rm yugabytedb_node1

docker kill yugabytedb_node2
docker container rm yugabytedb_node2

docker kill yugabytedb_node3
docker container rm yugabytedb_node3

docker network rm geo-messenger-net

rm -R ~/postgresql_data/
rm -R ~/yb_docker_data
rm -R ~/minio/data

#remove all unused volumes
docker volume prune 
```
