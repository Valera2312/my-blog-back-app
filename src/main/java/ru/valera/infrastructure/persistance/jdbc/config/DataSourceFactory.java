package ru.valera.infrastructure.persistance.jdbc.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@Configuration
public class DataSourceFactory {

    @Value("classpath:blog_init.sql")
    Resource resource;

    @Bean
    public DataSource dataSource(JdbcConfig config) {
        HikariDataSource ds = new HikariDataSource();

        ds.setJdbcUrl(config.getUrl());
        ds.setUsername(config.getUsername());
        ds.setPassword(config.getPassword());
        ds.setDriverClassName(config.getDriver());
        ds.setSchema(config.getSchema());

        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setConnectionTimeout(3000);
        ds.setLeakDetectionThreshold(5000);
        return ds;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void contextRefreshedEvent(ContextRefreshedEvent event) {
        DataSource dataSource = event.getApplicationContext().getBean(DataSource.class);
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(resource);
        populator.execute(dataSource);
    }
}
