package ru.valera.infrastructure.persistence.jdbc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.properties")
@Getter
@Setter
public class JdbcConfig {

    @Value("${db.url}")
    String url;
    @Value("${db.username}")
    String username;
    @Value("${db.password}")
    String password;
    @Value("${db.driver}")
    String driver;
    @Value("${db.schema}")
    String schema;
}