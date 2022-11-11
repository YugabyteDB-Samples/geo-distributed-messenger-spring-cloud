# Gitpod Deployment

Follow this instruction if you wish to run the entire application with all the components in Gitpod.

<!-- vscode-markdown-toc -->

- [Docker Compose Deployment](#docker-compose-deployment)
  - [Prerequisite](#prerequisite)
  - [Limitations](#limitations)
  - [Architecture](#architecture)
  - [Fork This Repository](#fork-this-repository)
  - [Start Application](#start-application)
  - [Play With Application](#play-with-application)

<!-- vscode-markdown-toc-config
    numbering=false
    autoSave=true
    /vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

## Prerequisite

* Gitpod account:

## Limitations

Presently, the current deployment option doesn't support MinIO that fails to start in Gitpod reporting file system-related issues. Thus, it won't be possible to upload pictures via the Attachments microservice.

Try out alternate deployment options that fully support attachments:
* [Local deployment option](local_deployment.md)
* [Cloud-native geo-distributed deployment option](gcloud_deployment.md)


## Architecture

![gitpod_deployment_architecture](https://user-images.githubusercontent.com/1537233/201374026-cb201384-c1e2-4207-8a1a-3fd77f9e8d1e.png)

The application logic is shared between two microservices.

The main Messenger microservice supports basic messaging capabilities - exchanging messages within various channels. The microservice stores application data (workspaces, users, channels, messages, etc.) in a multi-node YugabyteDB database.

The second Attachments microservice is responsible for storing using pictures (attachements) in MinIO. Presently, it's not supported with the Gitpod deployment option.

Both microservices load configuration settings from the Spring Cloud Config Server. The Config Server stores configurations in the following GitHub repository: https://github.com/YugabyteDB-Samples/geo-distributed-messenger-config-repo

Upon startup, both microservices register with the Spring Cloud Discovery Server. The Discovery Server serves as an API/communication layer. When the Messenger service needs to upload a picture, it discovers an instance of the Attachments microservice via the Discovery Server and sends a request to that instance.

## Fork This Repository

In order to deploy this app in Gitpod, you must first fork this repository. The app won't start in the `YugabyteDB-Samples` environment.

So, go ahead and fork this repo!

## Start Application

1. Open the link below to start the application in Gitpod:
    ```shell
    https://gitpod.io/#https://github.com/[your_github_username]/geo-distributed-messenger-spring-cloud
    ```

2. Wait while Gitpod compiles the image and starts the environment
    
3. Once the environment is started, wait while the Messenger microservice completes bootstrapping. Gitpod will open the URL (or request a permission to open) for port `8080`:
    ![Gitpod port 8080](https://user-images.githubusercontent.com/1537233/201378613-3676a759-11e4-48b9-acb7-e5e7636a7fd3.png)

4. Vaadin might continue compiling the frontend when you open the URL for port `8080`. Wait while the compilation finishes:
    ![Vaadin compilation](https://user-images.githubusercontent.com/1537233/201379566-50368ec6-a90e-45d9-af98-2412a2dcf023.png)
    
    You'll see the following in the logs once the compilation is over:
    
    ![Compilation is over](https://user-images.githubusercontent.com/1537233/201380234-7dbfcb9c-64ff-4d63-9aac-80c85c74cb44.png)

## Play With Application

First, check that YugabyteDB and Discovery Server run normally.

1. Confirm both microservices are registered with the Discovery Server by opening URL for port `8761` (see the `Ports` tab of the VS terminal section):
    ![Discovery Server](https://user-images.githubusercontent.com/1537233/201381717-99609962-7646-43e6-94a4-cd6b276cd266.png)


2. Check that a three-node YugabyteDB cluster is started by opening the URL for port `7001`:
    ![YugabyteDB](https://user-images.githubusercontent.com/1537233/201381965-25e804d1-0708-4950-9ce9-dac39edca5da.png)

    
Finally, go ahead and try out the application.

1. Open or refresh the URL for port `8080` and log in using the following credentials:
    ```shell
    username: test@gmail.com
    pwd: password
    ```

2. Send a few messages:
    ![Messenger](https://user-images.githubusercontent.com/1537233/201382336-d4d3adeb-190a-4b5d-8ed6-8baba69a1ca1.png)


What's next? Try out the [geo-distributed deployment option](gcloud_deployment.md) of the application that spans accross countries and continents in Google Cloud.
