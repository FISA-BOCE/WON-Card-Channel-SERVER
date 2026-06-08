package com.woorifisa.won_card_channel_server.global.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(prefix = "features.aidb", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiDbDataSourceConfig {

    private HikariDataSource cardAiDataSource;
    private HikariDataSource securitiesAiDataSource;

    @Bean
    @ConfigurationProperties(prefix = "datasource.card-ai")
    public AiDataSourceProperties cardAiDataSourceProperties() {
        return new AiDataSourceProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "datasource.securities-ai")
    public AiDataSourceProperties securitiesAiDataSourceProperties() {
        return new AiDataSourceProperties();
    }

    @Bean
    public JdbcTemplate cardAiJdbcTemplate() {
        cardAiDataSource = buildDataSource(cardAiDataSourceProperties());
        return new JdbcTemplate(cardAiDataSource);
    }

    @Bean
    public JdbcTemplate securitiesAiJdbcTemplate() {
        securitiesAiDataSource = buildDataSource(securitiesAiDataSourceProperties());
        return new JdbcTemplate(securitiesAiDataSource);
    }

    @PreDestroy
    public void closeDataSources() {
        if (cardAiDataSource != null) cardAiDataSource.close();
        if (securitiesAiDataSource != null) securitiesAiDataSource.close();
    }

    private HikariDataSource buildDataSource(AiDataSourceProperties properties) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(properties.getUrl());
        ds.setUsername(properties.getUsername());
        ds.setPassword(properties.getPassword());
        ds.setDriverClassName(properties.getDriverClassName());
        return ds;
    }

    @Getter
    @Setter
    public static class AiDataSourceProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName;
    }
}
