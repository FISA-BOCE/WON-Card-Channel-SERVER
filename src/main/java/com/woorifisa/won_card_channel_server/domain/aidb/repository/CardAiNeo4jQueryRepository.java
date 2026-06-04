package com.woorifisa.won_card_channel_server.domain.aidb.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class CardAiNeo4jQueryRepository {

    private static final String FIND_MONTHLY_SAME_ETF_AVERAGE_POINT_AMOUNT = """
            MATCH (me:User {userUuid: $userUuid})-[:SELECTED]->(etf:ETF)
            MATCH (other:User)-[:SELECTED]->(etf)
            WHERE other.userUuid <> me.userUuid
            MATCH (other)-[:REQUESTED_SWEEP]->(sr:SweepRequest)-[:TARGETS]->(etf)
            WHERE sr.requestStatus = 'SUCCEEDED'
              AND sr.baseMonth = $baseMonth
            WITH
                me,
                etf,
                other,
                sum(sr.pointAmount) AS userTotalPointAmount
            RETURN
                me.displayName AS userName,
                etf.etfId AS selectedEtfId,
                etf.ticker AS selectedEtfTicker,
                etf.etfName AS selectedEtfName,
                $baseMonth AS baseMonth,
                count(other) AS sameEtfUserCount,
                avg(userTotalPointAmount) AS averagePointAmount,
                sum(userTotalPointAmount) AS totalPointAmount
            """;

    private static final String FIND_MONTHLY_SWEEP_REQUESTS = """
            MATCH (u:User {userUuid: $userUuid})
                  -[:REQUESTED_SWEEP]->(sr:SweepRequest)
                  -[:TARGETS]->(etf:ETF)
            WHERE sr.baseMonth = $baseMonth
            OPTIONAL MATCH (sr)-[:EXECUTED_AS]->(se:SweepExecution)
            RETURN
                u.displayName AS userName,
                sr.sweepRequestId AS sweepRequestId,
                sr.baseMonth AS baseMonth,
                sr.pointAmount AS pointAmount,
                sr.krwAmount AS krwAmount,
                sr.requestStatus AS requestStatus,
                sr.requestedAt AS requestedAt,
                sr.completedAt AS requestCompletedAt,
                etf.etfId AS etfId,
                etf.ticker AS ticker,
                etf.etfName AS etfName,
                se.sweepId AS sweepId,
                se.sweepStatus AS sweepStatus,
                se.receivedAt AS receivedAt,
                se.startedAt AS startedAt,
                se.completedAt AS executionCompletedAt,
                se.failReason AS failReason
            ORDER BY sr.requestedAt DESC
            LIMIT $limit
            """;

    private final Driver cardNeo4jDriver;

    public CardAiNeo4jQueryRepository(@Qualifier("cardNeo4jDriver") Driver cardNeo4jDriver) {
        this.cardNeo4jDriver = cardNeo4jDriver;
    }

    public Optional<Map<String, Object>> findMonthlySameEtfAveragePointAmount(UUID userUuid, String baseMonth) {
        Map<String, Object> parameters = Map.of(
                "userUuid", userUuid.toString(),
                "baseMonth", baseMonth
        );

        try (var session = cardNeo4jDriver.session()) {
            return session.executeRead(tx -> tx.run(FIND_MONTHLY_SAME_ETF_AVERAGE_POINT_AMOUNT, parameters)
                    .list(Record::asMap)
                    .stream()
                    .findFirst());
        }
    }

    public List<Map<String, Object>> findMonthlySweepRequests(UUID userUuid, String baseMonth, int limit) {
        Map<String, Object> parameters = Map.of(
                "userUuid", userUuid.toString(),
                "baseMonth", baseMonth,
                "limit", limit
        );

        try (var session = cardNeo4jDriver.session()) {
            return session.executeRead(tx -> tx.run(FIND_MONTHLY_SWEEP_REQUESTS, parameters)
                    .list(Record::asMap));
        }
    }
}
