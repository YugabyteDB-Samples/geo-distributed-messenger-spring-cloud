# Local Application Deployment

Follow this instruction if you wish to run the entire application with all the components on your local laptop or on-premise environment.

<!-- vscode-markdown-toc -->

- [Local Application Deployment](#local-application-deployment)
  - [Prerequisite](#prerequisite)
  - [Architecture](#architecture)
  - [Create Custom Network](#create-custom-network)
  - [Start YugabyteDB](#start-yugabyteDB)
  - [Start Minio](#start-minio)
  - [Start Attachments Microservice](#start-attachments-microservice)
  - [Start Messenging Microservice](#start-messenging-microservice)
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

![architecture_local_deployment](https://user-images.githubusercontent.com/1537233/197897660-cc063e29-7f6e-4da2-8754-97548c879cc3.png)

The application logic is shared between two microservices.

The main Messaging microservice implements basic messaging capabilities letting exchange messages and content across messenger's channels. The microservices stores application data (workspaces, users, channels, messages, etc.) in YugabyteDB database.

The second Attachments microservice is responsible for storing using pictures (attachements) in an object storage. MinIO is used as that storage for the local deployment.

The Messaging microservice communicates to the Attachments one via the Kong Gateway. If the user wants to share a picture, the Messaging service triggers a special API endpoint on the Kong end and that endpoint routes the request to the Attachments instance.


## Create Custom Docker Network

YugabyteDB and Minio will be running in Docker containers. 

Create a custom network for them:
```shell
docker network create geo-messenger-net
```

## Start YugabyteDB

1. Start a YugabyteDB instance:
    ```shell
    rm -R ~/yb_docker_data
    mkdir ~/yb_docker_data

    docker run -d --name yugabytedb_node1 --net geo-messenger-net \
    -p 7001:7000 -p 9000:9000 -p 5433:5433 \
    -v ~/yb_docker_data/node1:/home/yugabyte/yb_data --restart unless-stopped \
    yugabytedb/yugabyte:2.15.3.0-b231 \
    bin/yugabyted start --listen yugabytedb_node1 \
    --base_dir=/home/yugabyte/yb_data --daemon=false
    ```

2. Confirm the instance is running: http://127.0.0.1:7001

## Start Minio

[Minio](https://min.io) is used in local deployments as an object store for pictures that are loaded through the Attachments service. 

1. Start the Minio service in:
    ```shell
    mkdir -p ~/minio/data

    docker run -d \
    --net geo-messenger-net \
    -p 9100:9000 \
    -p 9101:9001 \
    --name minio1 \
    -v ~/minio/data:/data \
    -e "MINIO_ROOT_USER=minio_user" \
    -e "MINIO_ROOT_PASSWORD=password" \
    quay.io/minio/minio:RELEASE.2022-08-26T19-53-15Z server /data --console-address ":9001"
    ```

2. Open the Minio console and log in using the `minio_user` as the user and `password` as the password:
    http://127.0.0.1:9101

## Start Config Server

Spring Cloud Config Server pulls configuration from the following public repository:
https://github.com/YugabyteDB-Samples/geo-distributed-messenger-config-repo

1. Start the config server:
    ```shell
    cd {project-root-dir}/config-server

    mvn spring-boot:run
    ```
    Alternatively, you can start the app from your IDE of choice. Just launch the `ConfigServerApplication.java` file.

2. Confirm the server is running by requesting a development configuration of the Messaging microservice:
    http://localhost:8888/messenger/dev

The config server clones the repo into the `$HOME/messenger-config` directory. You can override this settings in the server's `application.properties` file.

The repository includes settings for all presently implemented microservices:
* `messenger-dev.properties` - development configuration of the Messaging microservice for local testing (used by default)
* `messenger-prod.properties` - prod configuration of the Messaging microservice, requires to provide several settings via environment variables. Activated by the `-Pprod` maven profile.

## Start Attachments Microservice

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

## Start Messaging Microservice

1. Start the messaging microservice:
    ```shell
    cd {project-root-dir}/messenger
    ```

3. Start the app from command line:
    ```shell
    mvn spring-boot:run
    ```
    Alternatively, you can start the app from your IDE of choice. Just boot the `Application.java` file.

4. Open http://localhost:8080 in your browser (if it's not opened automatically)

5. Log in under a test user:
    ```shell
    username: test@gmail.com
    pwd: password
    ```

Enjoy and have fun! 

Next, try out the [cloud-native geo-distributed deployment option](gcloud_deployment.md) of the messenger that spans accross countries and continents.

## Clean Resources

If you're done working with the app, then use these command to remove Docker containers and other resources associated with them:

```shell
docker kill kong-gateway
docker container rm kong-gateway

docker kill minio1
docker container rm minio1

docker kill postgresql
docker container rm postgresql

docker kill yugabytedb_node1
docker container rm yugabytedb_node1

docker kill yugabytedb_node2
docker container rm yugabytedb_node1

docker kill yugabytedb_node2
docker container rm yugabytedb_node1

docker network rm geo-messenger-net

rm -R ~/postgresql_data/
rm -R ~/yb_docker_data
rm -R ~/minio/data

#remove all unused volumes
docker volume prune 
```
