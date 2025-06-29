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

import static java.util.Collections.singletonList;

/**
 * Implementation of the MetadataStoreService that communicates with the
 * metadata store via REST.
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
    public String submitRdf(String resourceType, String rdfData, String uuid) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(RdfMediaType.TEXT_TURTLE);

        HttpEntity<String> requestEntity = new HttpEntity<>(rdfData, headers);
        String uuidPath = uuid != null ? "/" + uuid : "";

        String endpoint = switch (resourceType) {
            case "ds" -> datasetsEndpoint;
            case "pl" -> pluginsEndpoint;
            case "pipe" -> pipelinesEndpoint;
            default -> throw new IllegalArgumentException("Unknown resource type: " + resourceType);
        } + uuidPath;

        ResponseEntity<String> response = restTemplate.postForEntity(
                endpoint,
                requestEntity,
                String.class);

        return response.getBody();
    }

    @Override
    public String getResourceRdf(String resourceType, String uuid) {
        String url = switch (resourceType) {
            case "ds" -> datasetsEndpoint + "/" + uuid;
            case "pl" -> pluginsEndpoint + "/" + uuid;
            case "pipe" -> pipelinesEndpoint + "/" + uuid;
            default ->
                // Fall back to generic resources endpoint
                resourcesEndpoint + "/" + uuid;
        };

        // Use specific endpoints for known resource types

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(singletonList(RdfMediaType.TEXT_TURTLE));

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                String.class);

        return response.getBody();
    }

    @Override
    public boolean resourceExists(String resourceType, String uuid) {
        String url = switch (resourceType) {
            case "ds" -> datasetsEndpoint + "/" + uuid;
            case "pl" -> pluginsEndpoint + "/" + uuid;
            case "pipe" -> pipelinesEndpoint + "/" + uuid;
            default ->
                // Fall back to generic resources endpoint
                resourcesEndpoint + "/" + uuid;
        };

        // Use specific endpoints for known resource types

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.HEAD,
                    new HttpEntity<>(new HttpHeaders() {
                        {
                            setAccept(singletonList(RdfMediaType.TEXT_TURTLE));
                        }
                    }),
                    Void.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking if resource exists: {}", e.getMessage(), e);
            return false;
        }
    }
}
