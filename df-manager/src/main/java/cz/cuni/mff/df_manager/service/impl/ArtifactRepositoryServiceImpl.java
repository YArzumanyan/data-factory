package cz.cuni.mff.df_manager.service.impl;

import cz.cuni.mff.df_manager.service.ArtifactRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Implementation of the ArtifactRepositoryService that communicates with the artifact repository via REST.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactRepositoryServiceImpl implements ArtifactRepositoryService {

    private final RestTemplate restTemplate;

    @Value("${artifact-repository.upload-endpoint}")
    private String uploadEndpoint;

    @Value("${artifact-repository.download-endpoint}")
    private String downloadEndpointTemplate;

    @Override
    public String uploadArtifact(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                    uploadEndpoint,
                    requestEntity,
                    String.class
            );
            
            // Assuming the response body contains the artifact ID
            return response.getBody();
        } catch (IOException e) {
            log.error("Error uploading artifact", e);
            throw new RuntimeException("Failed to upload artifact: " + e.getMessage(), e);
        }
    }
}