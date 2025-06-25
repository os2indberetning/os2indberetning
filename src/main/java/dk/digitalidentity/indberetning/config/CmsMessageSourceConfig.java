package dk.digitalidentity.indberetning.config;


import dk.digitalidentity.indberetning.service.cms.CmsMessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CmsMessageSourceConfig {

    @Bean
    public CmsMessageSource cmsMessageSource() {
        CmsMessageSource messageSource = new CmsMessageSource();
        messageSource.setBasename("classpath:cms-messages");
        messageSource.setDefaultEncoding("UTF-8");

        return messageSource;
    }
}