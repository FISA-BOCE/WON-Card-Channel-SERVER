package com.woorifisa.won_card_channel_server.domain.sweep.repository;

import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepResultInbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SweepResultInboxRepository extends JpaRepository<SweepResultInbox, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i
            from SweepResultInbox i
            where i.idempotencyKey = :idempotencyKey
            """)
    Optional<SweepResultInbox> findByIdempotencyKeyForUpdate(@Param("idempotencyKey") String idempotencyKey);
}
