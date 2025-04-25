package cz.cuni.mff.artifactrepo.controller;

import cz.cuni.mff.artifactrepo.service.ObjectStorageService;
import io.minio.StatObjectResponse;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/objects")
public class ObjectStorageController {

    @Autowired
    private ObjectStorageService objectStorageService;

    @PostMapping
    public ResponseEntity<String> storeObjectAndGenerateId(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File cannot be empty");
        }
        try {
            String generatedObjectId = objectStorageService.storeObject(file);

            return ResponseEntity.status(HttpStatus.CREATED).body(generatedObjectId);
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error storing object: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{objectId}")
    public ResponseEntity<InputStreamResource> fetchObject(@PathVariable String objectId) {
        try {
            StatObjectResponse metadata = objectStorageService.getObjectMetadata(objectId);
            if (metadata == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Object not found");
            }

            InputStream inputStream = objectStorageService.fetchObject(objectId);
            if (inputStream == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Object not found");
            }

            InputStreamResource resource = new InputStreamResource(inputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(metadata.contentType()));
            headers.setContentLength(metadata.size());
            headers.setContentDispositionFormData("attachment", objectId);

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);

        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            if (e.getMessage() != null && e.getMessage().contains("NoSuchKey")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Object not found", e);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching object: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/{objectId}")
    public ResponseEntity<Void> deleteObject(@PathVariable String objectId) {
        try {
            objectStorageService.deleteObject(objectId);
            return ResponseEntity.noContent().build();
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            if (e.getMessage() != null && e.getMessage().contains("NoSuchKey")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Object not found", e);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting object: " + e.getMessage(), e);
        }
    }

    @GetMapping("/{objectId}/url")
    public ResponseEntity<String> getDownloadUrl(@PathVariable String objectId) {
        try {
            StatObjectResponse metadata = objectStorageService.getObjectMetadata(objectId);
            if (metadata == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Object not found");
            }

            String url = objectStorageService.getPresignedUrlForGet(objectId, 3600);
            return ResponseEntity.ok(url);
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            if (e.getMessage() != null && e.getMessage().contains("NoSuchKey")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Object not found", e);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error generating URL: " + e.getMessage(), e);
        }
    }
}