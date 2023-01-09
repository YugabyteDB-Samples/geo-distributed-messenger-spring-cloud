package com.yugabyte.app.messenger.data.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

@Service
public class FileUploadService {

    private DiscoveryClient discoveryClient;

    private URI attachmentsK8Uri;

    /**
     * If the app runs in a non-Kubernetes environment then an instance of the
     * Spring Cloud Discovery client is initialized and passed in the constructor.
     * 
     * If the app functions in a Kubernetes environment, then the host and port of a
     * Kubernetes Service for Attachments is used instead.
     * 
     * @param discoveryClient Spring Cloud Discovery client
     * @param attachmentsHost The hostname of a Kubernetes Service for Attachments.
     * @param attachmentsPort The port of a Kubernetes Service for Attachments.
     */
    public FileUploadService(
            @Autowired(required = false) DiscoveryClient discoveryClient,
            @Value("${k8.attachments.service.host:}") String attachmentsHost,
            @Value("${k8.attachments.service.port:0}") int attachmentsPort) {

        this.discoveryClient = discoveryClient;

        if (!attachmentsHost.isEmpty() && attachmentsPort != 0) {
            try {
                this.attachmentsK8Uri = new URI("http://" + attachmentsHost + ":" + attachmentsPort);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Optional<String> uploadAttachment(String fileName, String mimeType,
            InputStream inputStream) {

        URI uploadUri;

        if (attachmentsK8Uri != null) {
            uploadUri = attachmentsK8Uri;
        } else {
            uploadUri = getUriFromSpringDiscoveryServer();

            if (uploadUri == null) {
                return Optional.empty();
            }
        }

        HttpClient httpClient = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUri + "/upload?fileName=" + fileName))
                .header("Content-Type", mimeType)
                .POST(HttpRequest.BodyPublishers.ofInputStream(new Supplier<InputStream>() {
                    @Override
                    public InputStream get() {
                        return inputStream;
                    }
                }))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpStatus.SC_OK) {
                String fileURL = response.body();
                System.out.println("Succesfully loaded file: " + fileURL);

                return Optional.of(fileURL);
            }

            System.err.println("File uploading failed [code=" + response.statusCode() +
                    ", body=" + response.body());

            return Optional.empty();
        } catch (IOException | InterruptedException e1) {
            e1.printStackTrace();
        }

        return Optional.empty();
    }

    private URI getUriFromSpringDiscoveryServer() {
        List<ServiceInstance> serviceInstances = discoveryClient.getInstances("ATTACHMENTS");

        if (serviceInstances.isEmpty()) {
            System.err.println("The attachments service is down. No instances are available");
            return null;
        }

        ServiceInstance instance = serviceInstances
                .get(ThreadLocalRandom.current().nextInt(0, serviceInstances.size()));

        System.out.printf("Connected to service %s with URI %s\n", instance.getInstanceId(), instance.getUri());

        return instance.getUri();
    }
}
