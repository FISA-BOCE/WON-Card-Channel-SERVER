package com.woorifisa.won_card_channel_server.domain.sweep.repository;

import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepOutbox;
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

public interface CardChnSweepOutboxRepository extends JpaRepository<CardChnSweepOutbox, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select o
            from CardChnSweepOutbox o
            where o.publishStatus in :statuses
              and (o.nextRetryAt is null or o.nextRetryAt <= :now)
            order by o.outboxEventId asc
            """)
    List<CardChnSweepOutbox> findPublishTargets(
            @Param("statuses") Collection<OutboxPublishStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
