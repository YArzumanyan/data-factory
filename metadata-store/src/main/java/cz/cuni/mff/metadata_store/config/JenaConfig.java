package cz.cuni.mff.metadata_store.config;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.tdb2.TDB2Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration class for setting up the Apache Jena TDB2 Dataset bean.
 * Reads the database location from application properties, initializes the dataset,
 * loads the custom vocabulary, and ensures proper shutdown.
 */
@Configuration
public class JenaConfig {

    private static final Logger log = LoggerFactory.getLogger(JenaConfig.class);

    @Value("${jena.tdb2.location}")
    private String tdb2Location;

    private Dataset dataset;

    /**
     * Creates the Jena TDB2 Dataset bean.
     * Ensures the database directory exists and connects to the TDB2 dataset.
     * Specifies 'close' as the destroy method for proper resource release on shutdown.
     *
     * @return The configured Jena Dataset instance.
     * @throws IOException If the directory cannot be created.
     */
    @Bean(destroyMethod = "close")
    public Dataset dataset() throws IOException {
        log.info("Initializing Jena TDB2 dataset at location: {}", tdb2Location);
        Path locationPath = Paths.get(tdb2Location);

        if (!Files.exists(locationPath)) {
            log.info("TDB2 directory does not exist, creating: {}", locationPath);
            Files.createDirectories(locationPath);
        } else if (!Files.isDirectory(locationPath)) {
            log.error("TDB2 location exists but is not a directory: {}", locationPath);
            throw new IllegalStateException("TDB2 location must be a directory: " + tdb2Location);
        }

        this.dataset = TDB2Factory.connectDataset(tdb2Location);
        log.info("Jena TDB2 Dataset initialized successfully.");
        return this.dataset;
    }
}