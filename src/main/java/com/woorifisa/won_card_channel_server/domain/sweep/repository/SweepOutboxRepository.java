package com.woorifisa.won_card_channel_server.domain.sweep.repository;

import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SweepOutboxRepository extends JpaRepository<SweepOutbox, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from SweepOutbox o
            where o.publishStatus in :statuses
              and (o.nextRetryAt is null or o.nextRetryAt <= :now)
            order by o.outboxEventId asc
            """)
    List<SweepOutbox> findPublishTargets(
            @Param("statuses") Collection<OutboxPublishStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
            select o
            from SweepOutbox o
            where (:publishStatus is null or o.publishStatus = :publishStatus)
              and (:eventType is null or o.eventType = :eventType)
              and (:sweepRequestId is null or o.sweepRequestId = :sweepRequestId)
              and (:createdFrom is null or o.createdAt >= :createdFrom)
              and (:createdTo is null or o.createdAt < :createdTo)
            order by o.createdAt desc, o.outboxEventId desc
            """)
    Page<SweepOutbox> findAdminOutboxEvents(
            @Param("publishStatus") OutboxPublishStatus publishStatus,
            @Param("eventType") SweepEventType eventType,
            @Param("sweepRequestId") Long sweepRequestId,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            Pageable pageable
    );

    @Query("""
            select count(o)
            from SweepOutbox o
            where (:publishStatus is null or o.publishStatus = :publishStatus)
              and (:eventType is null or o.eventType = :eventType)
              and (:sweepRequestId is null or o.sweepRequestId = :sweepRequestId)
              and (:createdFrom is null or o.createdAt >= :createdFrom)
              and (:createdTo is null or o.createdAt < :createdTo)
            """)
    long countAdminOutboxEvents(
            @Param("publishStatus") OutboxPublishStatus publishStatus,
            @Param("eventType") SweepEventType eventType,
            @Param("sweepRequestId") Long sweepRequestId,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo
    );
}
