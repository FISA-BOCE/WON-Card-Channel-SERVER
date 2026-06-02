package com.woorifisa.won_card_channel_server.global.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({Neo4jConfig.CardNeo4jProperties.class, Neo4jConfig.SecuritiesNeo4jProperties.class})
public class Neo4jConfig {

    @Bean(destroyMethod = "close")
    public Driver cardNeo4jDriver(CardNeo4jProperties props) {
        return GraphDatabase.driver(props.uri(), AuthTokens.basic(props.username(), props.password()));
    }

    @Bean(destroyMethod = "close")
    public Driver securitiesNeo4jDriver(SecuritiesNeo4jProperties props) {
        return GraphDatabase.driver(props.uri(), AuthTokens.basic(props.username(), props.password()));
    }

    @ConfigurationProperties(prefix = "neo4j.card")
    public record CardNeo4jProperties(String uri, String username, String password) {}

    @ConfigurationProperties(prefix = "neo4j.securities")
    public record SecuritiesNeo4jProperties(String uri, String username, String password) {}
}
