# Geo-Distributed Messenger With Spring Cloud, YugabyteDB and Google Cloud

This application is a Slack-like messenger that is designed to function across geographies. 

Presently the application supports basic messaging features as well as an ability to share pictures in discussion channels. 

![image9](https://user-images.githubusercontent.com/1537233/197895210-5052d681-cd8e-45b2-a621-429b05bce682.png)

The messenger can be deployed as a single instance on your local laptop or function across the world in the public Google Cloud infrastructure.

The software stack is built on Spring Boot, Spring Cloud Config Server, Spring Cloud Discover Server, Vaadin and YugabyteDB database. Google Cloud Storage is used for the Google Cloud deployment and MinIO is used in other deployment options. 

## Deployment Options

![high_level_architecture](https://user-images.githubusercontent.com/1537233/201415768-1c84858d-25f7-41ca-916f-5e660a0f1d4d.png)

The application can be deployed in several environments.

| Deployment Type    | Description   |         
| ------------------ |:--------------|
| [Your Laptop](local_deployment.md)        | Deploy the entire app with all the components (Spring Cloud Config, Spring Cloud Discovery, YugabyteDB, MinIO) on your local machine.|
| [Docker Compose](docker_compose_deployment.md)     | No need to install anything apart from Docker and Docker Compose. Deploy the entire app in Docker with a single Docker Compose command.|
| [Gitpod](gitpod_deployment.md)             | Run the app in your personal Gitpod environment. Requires to fork this repository. |
| [Deployment in Google Kubernetes Engine](gke_deployment.md)       | Deploy a distributed version of the app across two Kubernetes clusters in Google Cloud.|
| [Geo-Distributed Deployment in Google Kubernetes Engine](gke_geo_distributed_deployment.md)       | Deploy a geo-distributed version of the app across five Kubernetes clusters in Google Cloud.|
| [Deployment on Google Cloud Virtual Machines](gcloud_deployment.md)       | Deploy a geo-distributed version of the app across multiple regions using VMs of Google Cloud.|
