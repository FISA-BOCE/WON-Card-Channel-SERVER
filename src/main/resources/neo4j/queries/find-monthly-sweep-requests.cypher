MATCH (u:User {userUuid: $userUuid})
      -[:REQUESTED_SWEEP]->(sr:SweepRequest)
      -[:TARGETS]->(etf:ETF)
WHERE sr.baseMonth = $baseMonth
WITH DISTINCT u, sr, etf
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