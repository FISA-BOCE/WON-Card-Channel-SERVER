package com.woorifisa.won_card_channel_server.domain.sweep.repository;

import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardChnSweepOutboxRepository extends JpaRepository<CardChnSweepOutbox, Long> {
}
