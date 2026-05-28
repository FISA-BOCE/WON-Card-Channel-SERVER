package com.woorifisa.won_card_channel_server.domain.card.repository;

import com.woorifisa.won_card_channel_server.domain.card.model.CardChnCardSummary;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardChnCardSummaryRepository extends JpaRepository<CardChnCardSummary, Long> {

    Optional<CardChnCardSummary> findByUserUuid(UUID userUuid);

    Optional<CardChnCardSummary> findByCardUserUuid(UUID cardUserUuid);
}
