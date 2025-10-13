package com.company.fxtrading.config;

import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;
import org.springframework.web.context.WebApplicationContext;

/**
 * Configuration for Spring-aware Bean Validation.
 * Allows validators to use dependency injection by using Spring's constraint validator factory.
 */
@Configuration
public class ValidationConfig {

    private final WebApplicationContext applicationContext;

    public ValidationConfig(WebApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Creates a LocalValidatorFactoryBean that allows Spring dependency injection
     * in custom validators using SpringConstraintValidatorFactory.
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.setConstraintValidatorFactory(new SpringConstraintValidatorFactory(
            applicationContext.getAutowireCapableBeanFactory()));
        return factory;
    }

    /**
     * Expose the validator as a Validator bean for autowiring.
     */
    @Bean
    public Validator javaValidator() {
        return validator().getValidator();
    }
}
