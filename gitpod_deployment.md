# Gitpod Deployment

Follow this instruction if you wish to run the entire application with all the components in Gitpod.

<!-- vscode-markdown-toc -->

- [Docker Compose Deployment](#docker-compose-deployment)
  - [Prerequisite](#prerequisite)
  - [Limitations](#limitations)
  - [Architecture](#architecture)
  - [Start Application](#start-application)
  - [Play With Application](#play-with-application)
  - [Clean Resources](#clean-resources)

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

![docker_compose_deployment_architecture](https://user-images.githubusercontent.com/1537233/201368434-bf5f2ce9-0fe3-49b1-9715-bf6e13dfe44a.png)

The application logic is shared between two microservices.

The main Messenger microservice supports basic messaging capabilities - exchanging messages within various channels. The microservice stores application data (workspaces, users, channels, messages, etc.) in a multi-node YugabyteDB database.

The second Attachments microservice is responsible for storing using pictures (attachements) in MinIO. MinIO is an S3-compliant object store.

Both microservices load configuration settings from the Spring Cloud Config Server. The Config Server stores configurations in the following GitHub repository: https://github.com/YugabyteDB-Samples/geo-distributed-messenger-config-repo

Upon startup, both microservices register with the Spring Cloud Discovery Server. The Discovery Server serves as an API/communication layer. When the Messenger service needs to upload a picture, it discovers an instance of the Attachments microservice via the Discovery Server and sends a request to that instance.

For the sake of simplicity, YugabyteDB and MinIO are deployed in Docker and the other components are started on your host machine as standalone Java applications.

## Fork This Repository


## Start Application

Docker Compose allows to start the entire solution with a single command.

1. Make sure you're located in the project's root directory and run the following command:
    ```shell
    docker-compose run
    ```
2. Wait while all the components are ready by checking the command line output. The Messenger microservice finishes the boostraping the last by preloading the mock data:
    ```java
    messenger_1         | 2022-11-11 14:52:29.839  INFO 185 --- [  restartedMain] c.y.a.m.data.generator.DataGenerator     : Generating Channels
    messenger_1         | 2022-11-11 14:52:30.893  INFO 185 --- [  restartedMain] c.y.a.m.data.generator.DataGenerator     : Generating Users
    messenger_1         | 2022-11-11 14:52:41.515  INFO 185 --- [  restartedMain] c.y.a.m.data.generator.DataGenerator     : Mapping Users to Workspaces
    messenger_1         | 2022-11-11 14:52:49.748  INFO 185 --- [  restartedMain] c.y.a.m.data.generator.DataGenerator     : Generating Messages
    messenger_1         | 2022-11-11 14:53:12.965  INFO 185 --- [  restartedMain] c.y.a.m.data.generator.DataGenerator     : Finished data generation
    messenger_1         | Preloaded all Profiles to local cache
    ```

3. Confirm both microservices are registered with the Discovery Server:
    http://127.0.0.1:8761
    ![Discovery Server](https://user-images.githubusercontent.com/1537233/201366203-2579a073-4f1f-403e-90f1-93a3287643b9.png)

4. Check that a three-node YugabyteDB cluster is started:
    http://127.0.0.1:7001
    ![YugabyteDB](https://user-images.githubusercontent.com/1537233/201366549-19bbbe35-d22c-4fc9-a3c9-5c45412da4db.png)

## Play With Application

Go ahead and try out the application.

1. Open http://localhost:8080 in your browser and log in using the following credentials:
    ```shell
    username: test@gmail.com
    pwd: password
    ```

4. Send a few messages:
    ![Screen Shot 2022-11-11 at 10 12 31 AM](https://user-images.githubusercontent.com/1537233/201368976-74513caa-4834-411d-a020-f5ccc8256989.png)


Note, during the first launch, it might take several minutes for Vaadin to compile the frontend. During this time the http://localhost:8080, should show the following message:
![Vaadin compiling](https://user-images.githubusercontent.com/1537233/201361833-6c303714-d334-40c7-a6ad-578f4b989d07.png)

Vaadin finishes the compilation by printing out the following messages to the log:
```java
messenger_1         | 2022-11-11 14:55:11.760  INFO 185 --- [onPool-worker-2] c.v.f.s.frontend.TaskUpdatePackages      : Frontend dependencies resolved successfully.
messenger_1         | 2022-11-11 14:55:12.568  INFO 185 --- [onPool-worker-2] c.v.f.s.frontend.TaskCopyFrontendFiles   : Copying frontend resources from jar files ...
messenger_1         | 2022-11-11 14:55:12.674  INFO 185 --- [onPool-worker-2] c.v.f.s.frontend.TaskCopyFrontendFiles   : Visited 20 resources. Took 106 ms.
messenger_1         | 2022-11-11 14:55:12.710  INFO 185 --- [onPool-worker-2] c.v.b.devserver.AbstractDevServerRunner  : Starting Webpack
messenger_1         | 
messenger_1         | ------------------ Starting Frontend compilation. ------------------
messenger_1         | 2022-11-11 14:55:13.065  INFO 185 --- [onPool-worker-2] c.v.b.devserver.AbstractDevServerRunner  : Running Webpack to compile frontend resources. This may take a moment, please stand by...
messenger_1         | 2022-11-11 14:55:14.726  INFO 185 --- [v-server-output] c.v.b.devserver.DevServerOutputTracker   : [webpack-dev-server] Project is running at:
messenger_1         | 2022-11-11 14:55:14.726  INFO 185 --- [v-server-output] c.v.b.devserver.DevServerOutputTracker   : [webpack-dev-server] Loopback: http://localhost:41943/
messenger_1         | 2022-11-11 14:55:14.727  INFO 185 --- [v-server-output] c.v.b.devserver.DevServerOutputTracker   : [webpack-dev-server] Content not from webpack is served from '/opt/messenger/messenger/target/classes/META-INF/VAADIN/webapp, /opt/messenger/messenger/src/main/webapp' directory
messenger_1         | 2022-11-11 14:55:18.296  INFO 185 --- [v-server-output] c.v.b.devserver.DevServerOutputTracker   : [build-status] : Compiled.
messenger_1         | 
messenger_1         | ----------------- Frontend compiled successfully. -----------------
messenger_1         | 
```

What's next? Try out the [geo-distributed deployment option](gcloud_deployment.md) of the application that spans accross countries and continents in Google Cloud.

## Clean Resources

If you're done working with the app, then use this command to remove Docker containers and other resources associated with them:

```shell
docker-compose down
```
