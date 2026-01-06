package ru.valera.infrastructure.persistance.jdbc;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;


@Component
public class JdbcPostRepository {

    private final DataSource dataSource;

    public JdbcPostRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}

