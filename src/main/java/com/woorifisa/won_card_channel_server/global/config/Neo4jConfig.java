package com.woorifisa.won_card_channel_server.global.config;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {

    @Bean
    @ConfigurationProperties(prefix = "neo4j.card")
    public Neo4jProperties cardNeo4jProperties() {
        return new Neo4jProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "neo4j.securities")
    public Neo4jProperties securitiesNeo4jProperties() {
        return new Neo4jProperties();
    }

    @Bean(destroyMethod = "close")
    public Driver cardNeo4jDriver() {
        Neo4jProperties props = cardNeo4jProperties();
        return GraphDatabase.driver(props.getUri(), AuthTokens.basic(props.getUsername(), props.getPassword()));
    }

    @Bean(destroyMethod = "close")
    public Driver securitiesNeo4jDriver() {
        Neo4jProperties props = securitiesNeo4jProperties();
        return GraphDatabase.driver(props.getUri(), AuthTokens.basic(props.getUsername(), props.getPassword()));
    }

    @Getter
    @Setter
    public static class Neo4jProperties {
        private String uri;
        private String username;
        private String password;
    }
}
