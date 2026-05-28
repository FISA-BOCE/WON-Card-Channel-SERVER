package com.woorifisa.won_card_channel_server.domain.performance.repository;

import com.woorifisa.won_card_channel_server.domain.performance.model.CardChnPerformanceSummary;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardChnPerformanceSummaryRepository extends JpaRepository<CardChnPerformanceSummary, Long> {

    Optional<CardChnPerformanceSummary> findByUserUuidAndBaseMonth(UUID userUuid, String baseMonth);
}
