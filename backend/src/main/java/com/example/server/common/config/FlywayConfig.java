package com.example.server.common.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Flyway flyway(
            DataSource dataSource,
            org.springframework.core.env.Environment environment
    ) {
        String locationsProperty = environment.getProperty("spring.flyway.locations", "classpath:db/migration");
        String[] locations = Arrays.stream(locationsProperty.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);

        boolean baselineOnMigrate = environment.getProperty(
                "spring.flyway.baseline-on-migrate",
                Boolean.class,
                false
        );

        return Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor flywayEntityManagerDependencyPostProcessor() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("flyway")
                    || !beanFactory.containsBeanDefinition("entityManagerFactory")) {
                return;
            }

            BeanDefinition entityManagerFactory = beanFactory.getBeanDefinition("entityManagerFactory");
            String[] existingDependsOn = entityManagerFactory.getDependsOn();

            if (existingDependsOn == null || existingDependsOn.length == 0) {
                entityManagerFactory.setDependsOn("flyway");
                return;
            }

            boolean alreadyDependsOnFlyway = Arrays.asList(existingDependsOn).contains("flyway");
            if (alreadyDependsOnFlyway) {
                return;
            }

            String[] updatedDependsOn = Arrays.copyOf(existingDependsOn, existingDependsOn.length + 1);
            updatedDependsOn[updatedDependsOn.length - 1] = "flyway";
            entityManagerFactory.setDependsOn(updatedDependsOn);
        };
    }
}
