package cz.cuni.mff.df_manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Configuration for validation.
 */
@Configuration
public class ValidationConfig {

    /**
     * Creates a validator factory bean for validating request objects.
     *
     * @return A configured validator factory bean
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }
}