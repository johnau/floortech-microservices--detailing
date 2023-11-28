package tech.jmcs.floortech.detailing;

import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import tech.jmcs.floortech.detailing.infrastructure.persistence.config.AuditorAwareImpl;

@Configuration
@EnableReactiveMongoRepositories
@EnableReactiveMongoAuditing
@EnableScheduling
@ComponentScan(basePackages = {
        "tech.jmcs.floortech.common.helper",
        "tech.jmcs.floortech.common.auth",
        "tech.jmcs.floortech.common.amqp",
        "tech.jmcs.floortech.common.spring"
})
@PropertySource("classpath:jwt.properties")
public class AppConfiguration {
//    @Autowired
//    private ConfigurableEnvironment env;
//
//    @Bean
//    public void setEnv() {
//        env.setActiveProfiles("dev");
//        System.out.println("Environment set to: " + env.getActiveProfiles());
//    }

    /**
     * For Entity Validation (eg @NotNull)
     * @return
     */
    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener() {
        return new ValidatingMongoEventListener(validator());
    }

    @Bean
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }

    /**
     * For Mongo Auditing (@CreatedDate, @CreatedBy, @LastModifiedDate, @LastModifiedBy)
     * @return
     */
    @Bean
    public AuditorAware<String> myAuditorProvider() {
        return new AuditorAwareImpl();
    }

    @Bean
    public MethodValidationPostProcessor validationPostProcessor() {
        return new MethodValidationPostProcessor();
    }
}
