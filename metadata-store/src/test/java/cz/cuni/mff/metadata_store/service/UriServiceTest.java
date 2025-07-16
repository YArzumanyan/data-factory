package cz.cuni.mff.metadata_store.service;

import cz.cuni.mff.metadata_store.utils.Vocab;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UriServiceTest {

    private UriService uriService;

    @BeforeEach
    void setUp() {
        uriService = new UriService();
    }

    @Test
    void buildPipelineUri_shouldReturnCorrectUri() {
        String uuid = UUID.randomUUID().toString();
        String expectedUri = Vocab.PIPE_NS + uuid;
        assertEquals(expectedUri, uriService.buildPipelineUri(uuid));
    }

    @Test
    void buildDatasetUri_shouldReturnCorrectUri() {
        String uuid = UUID.randomUUID().toString();
        String expectedUri = Vocab.DS_NS + uuid;
        assertEquals(expectedUri, uriService.buildDatasetUri(uuid));
    }

    @Test
    void buildPluginUri_shouldReturnCorrectUri() {
        String uuid = UUID.randomUUID().toString();
        String expectedUri = Vocab.PL_NS + uuid;
        assertEquals(expectedUri, uriService.buildPluginUri(uuid));
    }
}

