package com.woorifisa.won_card_channel_server.global.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class AiDbDataSourceConfig {

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
        return new JdbcTemplate(buildDataSource(cardAiDataSourceProperties()));
    }

    @Bean
    public JdbcTemplate securitiesAiJdbcTemplate() {
        return new JdbcTemplate(buildDataSource(securitiesAiDataSourceProperties()));
    }

    private DataSource buildDataSource(AiDataSourceProperties properties) {
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
