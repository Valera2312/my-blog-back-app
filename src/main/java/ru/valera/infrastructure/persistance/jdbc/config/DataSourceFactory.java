package ru.valera.infrastructure.persistance.jdbc.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceFactory {

    @Bean
    public static DataSource dataSource(JdbcConfig config) {
        HikariDataSource ds = new HikariDataSource();

        ds.setJdbcUrl(config.getUrl());
        ds.setUsername(config.getUsername());
        ds.setPassword(config.getPassword());
        ds.setDriverClassName(config.getDriver());

        ds.setMaximumPoolSize(10);
        ds.setMinimumIdle(2);
        ds.setConnectionTimeout(3000);
        ds.setLeakDetectionThreshold(5000);
        return ds;
    }
}
