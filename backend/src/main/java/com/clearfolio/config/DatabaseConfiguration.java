package com.clearfolio.config;

import java.nio.file.Path;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqlite.SQLiteDataSource;

@Configuration
public class DatabaseConfiguration {
    @Bean
    DataSource dataSource(@Value("${DATABASE_PATH:data/resume_history.sqlite3}") String databasePath) {
        Path path = Path.of(databasePath).toAbsolutePath().normalize();
        Path parent = path.getParent();
        try {
            if (parent != null) {
                java.nio.file.Files.createDirectories(parent);
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not create the SQLite database directory.", exception);
        }

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + path);
        return dataSource;
    }
}
