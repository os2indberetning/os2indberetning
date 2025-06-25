package dk.digitalidentity.indberetning.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = { "dk.digitalidentity.indberetning.model.dao", "dk.digitalidentity.emailService.dao" }, repositoryFactoryBeanClass = JpaRepositoryFactoryBean.class)
public class JPAConfiguration {

}