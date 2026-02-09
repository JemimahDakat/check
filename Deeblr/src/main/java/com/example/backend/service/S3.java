package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Service
public class S3 {

    private final S3Client s3Client;
    private final String bucketName;

    // had to manually build clien as theree were version conflics

    public S3(
            //these values are pulled from 'application.properties'.
            // inject them here so we can change buckets/keys without recompiling code.
            @Value("${spring.cloud.aws.credentials.access-key}") String accessKey,
            @Value("${spring.cloud.aws.credentials.secret-key}") String secretKey,
            @Value("${spring.cloud.aws.s3.bucket}") String bucketName,
            @Value("${spring.cloud.aws.region.static}") String region
    ) {
        this.bucketName = bucketName;

        // build the S3 client using the AWS SDK builders.
        // avoids dependency conflicts i had difficult within Spring Cloud's auto-config.
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }

    public String uploadFile(MultipartFile file) throws IOException {
        //If two users upload "image.jpg", one would overwrite the other
        // i used a uuid to make sure every filename is unique.
        String key = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // prepare for the upload request
        // upload file to bucket
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        // send file to aws
        // use inputstream so diesnt need to load the whole file into ram. makes it memeory efficent

        s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // return the public url
        // use the AWS utility to generate the correct URL format
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(key)).toExternalForm();
    }
}