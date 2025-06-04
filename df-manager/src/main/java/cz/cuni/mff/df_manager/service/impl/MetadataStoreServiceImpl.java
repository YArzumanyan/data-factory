package cz.cuni.mff.df_manager.service.impl;

import cz.cuni.mff.df_manager.service.MetadataStoreService;
import cz.cuni.mff.df_manager.utils.RdfMediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of the MetadataStoreService that communicates with the metadata store via REST.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataStoreServiceImpl implements MetadataStoreService {

    private final RestTemplate restTemplate;

    @Value("${metadata-store.resources-endpoint}")
    private String resourcesEndpoint;

    @Value("${metadata-store.datasets-endpoint}")
    private String datasetsEndpoint;

    @Value("${metadata-store.pipelines-endpoint}")
    private String pipelinesEndpoint;

    @Value("${metadata-store.plugins-endpoint}")
    private String pluginsEndpoint;

    @Override
    public String submitRdf(String resourceType, String rdfData) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(RdfMediaType.TEXT_TURTLE);

        HttpEntity<String> requestEntity = new HttpEntity<>(rdfData, headers);

        String endpoint;
        switch (resourceType) {
            case "ds":
                endpoint = datasetsEndpoint;
                break;
            case "pl":
                endpoint = pluginsEndpoint;
                break;
            case "pipe":
                endpoint = pipelinesEndpoint;
                break;
            default:
                throw new IllegalArgumentException("Unknown resource type: " + resourceType);
        }

        ResponseEntity<String> response = restTemplate.postForEntity(
                endpoint,
                requestEntity,
                String.class
        );

        return response.getBody();
    }

    @Override
    public String getResourceRdf(String resourceType, String uuid) {
        String url;

        // Use specific endpoints for known resource types
        switch (resourceType) {
            case "ds":
                url = datasetsEndpoint + "/" + uuid;
                break;
            case "pl":
                url = pluginsEndpoint + "/" + uuid;
                break;
            case "pipe":
                url = pipelinesEndpoint + "/" + uuid;
                break;
            default:
                // Fall back to generic resources endpoint
                url = resourcesEndpoint + "/" + uuid;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.Collections.singletonList(RdfMediaType.TEXT_TURTLE));

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        return response.getBody();
    }

    @Override
    public boolean resourceExists(String resourceType, String uuid) {
        String url;

        // Use specific endpoints for known resource types
        switch (resourceType) {
            case "ds":
                url = datasetsEndpoint + "/" + uuid;
                break;
            case "pl":
                url = pluginsEndpoint + "/" + uuid;
                break;
            case "pipe":
                url = pipelinesEndpoint + "/" + uuid;
                break;
            default:
                // Fall back to generic resources endpoint
                url = resourcesEndpoint + "/" + uuid;
        }

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.HEAD,
                    null,
                    Void.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking if resource exists: {}", e.getMessage(), e);
            return false;
        }
    }
}
