package com.woorifisa.won_card_channel_server.global.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.function.Supplier;

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
        cardAiDataSource = buildDataSource(cardAiDataSourceProperties(), "card-ai");
        return new JdbcTemplate(cardAiDataSource);
    }

    @Bean
    public JdbcTemplate securitiesAiJdbcTemplate() {
        securitiesAiDataSource = buildDataSource(securitiesAiDataSourceProperties(), "securities-ai");
        return new JdbcTemplate(securitiesAiDataSource);
    }

    @Bean
    public MeterBinder aiDataSourceMetrics() {
        return registry -> {
            registerDataSourceMetrics(registry, "card-ai", () -> cardAiDataSource);
            registerDataSourceMetrics(registry, "securities-ai", () -> securitiesAiDataSource);
        };
    }

    @PreDestroy
    public void closeDataSources() {
        if (cardAiDataSource != null) cardAiDataSource.close();
        if (securitiesAiDataSource != null) securitiesAiDataSource.close();
    }

    private HikariDataSource buildDataSource(AiDataSourceProperties properties, String poolName) {
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName(poolName);
        ds.setJdbcUrl(properties.getUrl());
        ds.setUsername(properties.getUsername());
        ds.setPassword(properties.getPassword());
        ds.setDriverClassName(properties.getDriverClassName());
        return ds;
    }

    private void registerDataSourceMetrics(
            io.micrometer.core.instrument.MeterRegistry registry,
            String poolName,
            Supplier<HikariDataSource> dataSourceSupplier
    ) {
        Gauge.builder("app.datasource.connections.active", dataSourceSupplier, supplier -> getPoolValue(supplier, HikariPoolMXBean::getActiveConnections))
                .tag("pool", poolName)
                .description("Active connections in the datasource pool")
                .register(registry);
        Gauge.builder("app.datasource.connections.idle", dataSourceSupplier, supplier -> getPoolValue(supplier, HikariPoolMXBean::getIdleConnections))
                .tag("pool", poolName)
                .description("Idle connections in the datasource pool")
                .register(registry);
        Gauge.builder("app.datasource.connections.max", dataSourceSupplier, supplier -> {
                    HikariDataSource dataSource = supplier.get();
                    return dataSource == null ? 0 : dataSource.getMaximumPoolSize();
                })
                .tag("pool", poolName)
                .description("Maximum configured connections in the datasource pool")
                .register(registry);
        Gauge.builder("app.datasource.connections.pending", dataSourceSupplier, supplier -> getPoolValue(supplier, HikariPoolMXBean::getThreadsAwaitingConnection))
                .tag("pool", poolName)
                .description("Threads waiting for a datasource connection")
                .register(registry);
    }

    private double getPoolValue(Supplier<HikariDataSource> dataSourceSupplier, java.util.function.ToIntFunction<HikariPoolMXBean> valueFunction) {
        HikariDataSource dataSource = dataSourceSupplier.get();
        if (dataSource == null || dataSource.getHikariPoolMXBean() == null) {
            return 0;
        }
        return valueFunction.applyAsInt(dataSource.getHikariPoolMXBean());
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
