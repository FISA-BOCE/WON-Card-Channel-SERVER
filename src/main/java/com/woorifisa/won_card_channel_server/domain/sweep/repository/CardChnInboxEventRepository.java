package com.woorifisa.won_card_channel_server.domain.sweep.repository;

import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepInbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CardChnInboxEventRepository extends JpaRepository<CardChnSweepInbox, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i
            from CardChnSweepInbox i
            where i.idempotencyKey = :idempotencyKey
            """)
    Optional<CardChnSweepInbox> findByIdempotencyKeyForUpdate(@Param("idempotencyKey") String idempotencyKey);
}
