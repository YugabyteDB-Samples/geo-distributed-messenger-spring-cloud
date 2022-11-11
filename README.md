# Geo-Distributed Messenger With Spring Cloud, YugabyteDB and Google Cloud

This application is a Slack-like messenger that is designed to function across geographies. 

Presently the application supports basic messaging features as well as an ability to load pictures in discussion channels. 

![image9](https://user-images.githubusercontent.com/1537233/197895210-5052d681-cd8e-45b2-a621-429b05bce682.png)

The messenger can be deployed as a single instance on your local laptop or function across the world in the public Google Cloud infrastructure.

The software stack is built on Spring Boot, Spring Cloud Config Server, Spring Cloud Discover Server, Vaadin and YugabyteDB database. Google Cloud Storage is used for the Google Cloud deployment and MinIO is used in other deployment options. 

## Deployment Options

![architecture-geo-distributed](https://user-images.githubusercontent.com/1537233/197904658-1ce99812-bcfd-4de9-b782-41bc677545ba.png)

The application can be deployed in several environments.

| Deployment Type    | Description   |         
| ------------------ |:--------------|
| [Your Laptop](local_deployment.md)        | Deploy the entire app with all the components (Spring Cloud Config, Spring Cloud Discovery, YugabyteDB, Minio) on your local machine.|
| [Docker Compose](docker_compose_deployment.md)     | No need to install anything apart from Docker and Docker Compose. Deploy the entire app in Docker with a single Docker Compose command.|
| [Gitpod](gitpod_deployment.md)             | Run the app in your personal Gitpod environment. Requires to fork this repository. |
| [Geo-Distributed Deployment in Google Cloud](gcloud_deployment.md)       | Deploy a true geo-distributed version of the app across multiple regions in Google Cloud.|
