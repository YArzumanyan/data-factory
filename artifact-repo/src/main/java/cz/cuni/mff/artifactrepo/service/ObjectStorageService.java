package cz.cuni.mff.artifactrepo.service;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import io.minio.messages.Item;
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

    /**
     * Ensures that the configured bucket exists. If it does not exist, it creates the bucket.
     * This method is called after the service is initialized.
     */
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

    /**
     * Stores an object in the configured bucket and generates a unique ID for it.
     *
     * @param file The file to store.
     * @return The generated object ID.
     * @throws MinioException If there is an error communicating with MinIO.
     * @throws IOException If there is an IO error.
     * @throws InvalidKeyException If the provided key is invalid.
     * @throws NoSuchAlgorithmException If the specified algorithm does not exist.
     */
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

    /**
     * Fetches an object from the configured bucket.
     *
     * @param objectId The ID of the object to fetch.
     * @return An InputStream for the object, or null if the object does not exist.
     * @throws MinioException If there is an error communicating with MinIO.
     * @throws IOException If there is an IO error.
     * @throws InvalidKeyException If the provided key is invalid.
     * @throws NoSuchAlgorithmException If the specified algorithm does not exist.
     */
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

    /**
     * Retrieves metadata for an object in the configured bucket.
     *
     * @param objectId The ID of the object to retrieve metadata for.
     * @return The metadata of the object, or null if the object does not exist.
     * @throws MinioException If there is an error communicating with MinIO.
     * @throws IOException If there is an IO error.
     * @throws InvalidKeyException If the provided key is invalid.
     * @throws NoSuchAlgorithmException If the specified algorithm does not exist.
     */
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

    /**
     * Generates a presigned URL for downloading an object.
     *
     * @param objectId The ID of the object to generate the URL for.
     * @param expirySeconds The number of seconds until the URL expires.
     * @return A presigned URL for the object.
     * @throws MinioException If there is an error communicating with MinIO.
     * @throws IOException If there is an IO error.
     * @throws InvalidKeyException If the provided key is invalid.
     * @throws NoSuchAlgorithmException If the specified algorithm does not exist.
     */
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

    /**
     * Deletes an object from the configured bucket.
     *
     * @param objectId The ID of the object to delete.
     * @throws MinioException If there is an error communicating with MinIO.
     * @throws IOException If there is an IO error.
     * @throws InvalidKeyException If the provided key is invalid.
     * @throws NoSuchAlgorithmException If the specified algorithm does not exist.
     */
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

    // dumpObjectStorage();
    /**
     * Dumps the contents of the configured bucket.
     *
     * @return A string representation of the bucket contents.
     * @throws MinioException If there is an error communicating with MinIO.
     * @throws IOException If there is an IO error.
     * @throws InvalidKeyException If the provided key is invalid.
     * @throws NoSuchAlgorithmException If the specified algorithm does not exist.
     */
    public String dumpObjectStorage() throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        StringBuilder dump = new StringBuilder("Bucket: " + bucketName + "\n");
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                dump.append("Object ID: ").append(item.objectName())
                        .append(", Size: ").append(item.size())
                        .append(", Last Modified: ").append(item.lastModified())
                        .append("\n");
            }
            return dump.toString();
        } catch (Exception e) {
            logger.error("Error dumping object storage: {}", e.getMessage(), e);
            throw new RuntimeException("Error during object storage dump: " + e.getMessage(), e);
        }
    }

    /**
     * Handles exceptions by logging them and throwing appropriate exceptions.
     *
     * @param e The exception to handle.
     * @throws MinioException If the exception is a MinIO-specific error.
     * @throws IOException If the exception is an IO error.
     * @throws InvalidKeyException If the exception is due to an invalid key.
     * @throws NoSuchAlgorithmException If the exception is due to a non-existent algorithm.
     */
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
