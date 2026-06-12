package com.woorifisa.won_card_channel_server.domain.sweep.repository;

import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;
import jakarta.persistence.LockModeType;
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
}
