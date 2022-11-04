package com.yugabyte.app.messenger.data.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

@Service
public class FileUploadService {

    @Autowired
    DiscoveryClient discoveryClient;

    public Optional<String> uploadAttachment(String fileName, String mimeType,
            InputStream inputStream) {

        List<ServiceInstance> serviceInstances = discoveryClient.getInstances("ATTACHMENTS");

        if (serviceInstances.isEmpty()) {
            System.err.println("The attachments service is down. No instances are available");
            return Optional.empty();
        }

        ServiceInstance instance = serviceInstances
                .get(ThreadLocalRandom.current().nextInt(0, serviceInstances.size()));

        System.out.printf("Connected to service %s with URI %s\n", instance.getInstanceId(), instance.getUri());

        HttpClient httpClient = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(instance.getUri() + "/upload?fileName=" + fileName))
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
}
