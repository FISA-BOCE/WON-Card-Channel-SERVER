package com.woorifisa.won_card_channel_server.domain.sweep.repository;

import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepResultInbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.InboxProcessStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SweepResultInboxRepository extends JpaRepository<SweepResultInbox, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i
            from SweepResultInbox i
            where i.idempotencyKey = :idempotencyKey
            """)
    Optional<SweepResultInbox> findByIdempotencyKeyForUpdate(@Param("idempotencyKey") String idempotencyKey);

    @Query("""
            select i
            from SweepResultInbox i
            where (:processStatus is null or i.processStatus = :processStatus)
              and (:eventType is null or i.eventType = :eventType)
              and (:sweepRequestId is null or i.sweepRequestId = :sweepRequestId)
              and (:createdFrom is null or i.createdAt >= :createdFrom)
              and (:createdTo is null or i.createdAt < :createdTo)
            order by i.createdAt desc, i.inboxEventId desc
            """)
    Page<SweepResultInbox> findAdminInboxEvents(
            @Param("processStatus") InboxProcessStatus processStatus,
            @Param("eventType") SweepEventType eventType,
            @Param("sweepRequestId") Long sweepRequestId,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            Pageable pageable
    );

    @Query("""
            select count(i)
            from SweepResultInbox i
            where (:processStatus is null or i.processStatus = :processStatus)
              and (:eventType is null or i.eventType = :eventType)
              and (:sweepRequestId is null or i.sweepRequestId = :sweepRequestId)
              and (:createdFrom is null or i.createdAt >= :createdFrom)
              and (:createdTo is null or i.createdAt < :createdTo)
            """)
    long countAdminInboxEvents(
            @Param("processStatus") InboxProcessStatus processStatus,
            @Param("eventType") SweepEventType eventType,
            @Param("sweepRequestId") Long sweepRequestId,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo
    );
}
