package cz.cuni.mff.arzumany.metadata_store;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@OpenAPIDefinition(
	info = @io.swagger.v3.oas.annotations.info.Info(
		title = "Metadata Store API",
		version = "1.0",
		description = "API for storing and retrieving pipeline metadata"
	)
)
@RestController
public class MetadataStoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(MetadataStoreApplication.class, args);
	}
}
