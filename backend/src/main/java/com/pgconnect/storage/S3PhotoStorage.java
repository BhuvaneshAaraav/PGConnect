package com.pgconnect.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "pgconnect.storage", name = "enabled", havingValue = "true")
public class S3PhotoStorage implements PhotoStorage {
    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3PhotoStorage(S3Client client, S3Presigner presigner, @Value("${pgconnect.storage.bucket}") String bucket) {
        this.client = client; this.presigner = presigner; this.bucket = bucket;
    }

    @Override
    public void upload(String key, byte[] bytes, String contentType) {
        client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType)
                .serverSideEncryption("AES256").build(), RequestBody.fromBytes(bytes));
    }

    @Override
    public void delete(String key) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public String privateReadUrl(String key) {
        var request = GetObjectRequest.builder().bucket(bucket).key(key).build();
        var presign = GetObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(15)).getObjectRequest(request).build();
        return presigner.presignGetObject(presign).url().toExternalForm();
    }
}
