MATCH (me:User {userUuid: $userUuid})-[:SELECTED]->(etf:ETF)
WITH DISTINCT me, etf
CALL {
    WITH me, etf
    MATCH (other:User)-[:SELECTED]->(etf)
    WHERE other.userUuid <> me.userUuid
    WITH DISTINCT other, etf
    MATCH (other)-[:REQUESTED_SWEEP]->(sr:SweepRequest)-[:TARGETS]->(etf)
    WHERE sr.requestStatus = $completedRequestStatus
      AND sr.baseMonth = $baseMonth
    WITH DISTINCT other, sr
    WITH other, sum(sr.pointAmount) AS userTotalPointAmount
    RETURN
        count(other) AS sameEtfUserCount,
        coalesce(avg(userTotalPointAmount), 0) AS averagePointAmount,
        coalesce(sum(userTotalPointAmount), 0) AS totalPointAmount
}
RETURN
    me.displayName AS userName,
    etf.etfId AS selectedEtfId,
    etf.ticker AS selectedEtfTicker,
    etf.etfName AS selectedEtfName,
    $baseMonth AS baseMonth,
    sameEtfUserCount AS sameEtfUserCount,
    averagePointAmount AS averagePointAmount,
    totalPointAmount AS totalPointAmount