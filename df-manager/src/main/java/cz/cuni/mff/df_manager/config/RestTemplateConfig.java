package cz.cuni.mff.df_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for REST clients.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a RestTemplate bean for making HTTP requests.
     *
     * @return A configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}