package com.pgconnect.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@ConditionalOnProperty(prefix = "pgconnect.storage", name = "enabled", havingValue = "true")
public class StorageConfiguration {
    @Bean(destroyMethod = "close")
    S3Client s3Client(@Value("${pgconnect.storage.region}") String region,
                      @Value("${pgconnect.storage.endpoint:}") String endpoint,
                      @Value("${pgconnect.storage.path-style:false}") boolean pathStyle) {
        var builder = S3Client.builder().region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        if (!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint));
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    S3Presigner s3Presigner(@Value("${pgconnect.storage.region}") String region,
                            @Value("${pgconnect.storage.endpoint:}") String endpoint,
                            @Value("${pgconnect.storage.path-style:false}") boolean pathStyle) {
        var builder = S3Presigner.builder().region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        if (!endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint));
        return builder.build();
    }
}
