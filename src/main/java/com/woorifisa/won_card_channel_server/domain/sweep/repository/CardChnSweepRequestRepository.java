package com.woorifisa.won_card_channel_server.domain.sweep.repository;

import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardChnSweepRequestRepository extends JpaRepository<CardChnSweepRequest, Long> {
    boolean existsByPointLedgerId(Long pointLedgerId);

    boolean existsByIdempotencyKey(String idempotencyKey);

}
