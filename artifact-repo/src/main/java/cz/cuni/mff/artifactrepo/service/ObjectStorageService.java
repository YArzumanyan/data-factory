package cz.cuni.mff.artifactrepo.service;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ObjectStorageService {
    private static final Logger logger = LoggerFactory.getLogger(ObjectStorageService.class);

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @PostConstruct
    private void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                logger.info("Bucket {} created successfully.", bucketName);
            } else {
                logger.info("Bucket {} already exists.", bucketName);
            }
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            logger.error("Error ensuring bucket exists: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize MinIO bucket: " + e.getMessage(), e);
        }
    }

    public String storeObject(MultipartFile file) throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        String objectId = UUID.randomUUID().toString();

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectId)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            logger.info("Object '{}' uploaded successfully to bucket '{}'.", objectId, bucketName);
            return objectId;
        } catch (Exception e) {
            logger.error("Error uploading object with generated ID '{}': {}", objectId, e.getMessage(), e);
            errorHandler(e);
            throw new RuntimeException("Error during object upload: " + e.getMessage(), e);
        }
    }

    public InputStream fetchObject(String objectId) throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectId)
                            .build());
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                logger.warn("Object '{}' not found in bucket '{}'.", objectId, bucketName);
                return null;
            }
            logger.error("Error fetching object '{}': Minio error code {}", objectId, e.errorResponse().code(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching object '{}': {}", objectId, e.getMessage(), e);
            errorHandler(e);
            throw new RuntimeException("Error during object fetch: " + e.getMessage(), e);
        }
    }

    public StatObjectResponse getObjectMetadata(String objectId) throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectId)
                            .build());
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                logger.warn("Metadata requested for non-existent object '{}' in bucket '{}'.", objectId, bucketName);
                return null;
            }
            logger.error("Error getting metadata for object '{}': Minio error code {}", objectId, e.errorResponse().code(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error getting metadata for object '{}': {}", objectId, e.getMessage(), e);
            errorHandler(e);
            throw new RuntimeException("Error during metadata fetch: " + e.getMessage(), e);
        }
    }

    public String getPresignedUrlForGet(String objectId, int expirySeconds) throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectId)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            logger.error("Error generating presigned URL for object '{}': {}", objectId, e.getMessage(), e);
            errorHandler(e);
            throw new RuntimeException("Error during presigned URL generation: " + e.getMessage(), e);
        }
    }

    public void deleteObject(String objectId) throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectId)
                            .build());
            logger.info("Object '{}' deleted successfully from bucket '{}'.", objectId, bucketName);
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                logger.warn("Attempted to delete non-existent object '{}' in bucket '{}'.", objectId, bucketName);
            }
            logger.error("Error deleting object '{}': Minio error code {}", objectId, e.errorResponse().code(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting object '{}': {}", objectId, e.getMessage(), e);
            errorHandler(e);
            throw new RuntimeException("Error during object deletion: " + e.getMessage(), e);
        }
    }

    private void errorHandler(Exception e) throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        switch (e) {
            case MinioException minioException -> {
                logger.error("MinIO error: {}", e.getMessage(), e);
                throw minioException;
            }
            case IOException ioException -> {
                logger.error("IO error: {}", e.getMessage(), e);
                throw ioException;
            }
            case InvalidKeyException invalidKeyException -> {
                logger.error("Invalid key error: {}", e.getMessage(), e);
                throw invalidKeyException;
            }
            case NoSuchAlgorithmException noSuchAlgorithmException -> {
                logger.error("No such algorithm error: {}", e.getMessage(), e);
                throw noSuchAlgorithmException;
            }
            default -> {
                logger.error("Unexpected error: {}", e.getMessage(), e);
                throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
            }
        }
    }
}
