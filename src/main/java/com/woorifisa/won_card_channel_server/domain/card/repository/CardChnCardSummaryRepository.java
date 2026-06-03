package com.woorifisa.won_card_channel_server.domain.card.repository;

import com.woorifisa.won_card_channel_server.domain.card.model.CardChnCardSummary;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardChnCardSummaryRepository extends JpaRepository<CardChnCardSummary, Long> {

    Optional<CardChnCardSummary> findByUserUuid(UUID userUuid);

    Optional<CardChnCardSummary> findByCardUserUuid(UUID cardUserUuid);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update CardChnCardSummary c
           set c.selectedEtfId = c.pendingSelectedEtfId,
               c.pendingSelectedEtfId = null,
               c.pendingEffectiveFrom = null,
               c.lastSyncedAt = :now
         where c.pendingSelectedEtfId is not null
           and c.pendingEffectiveFrom is not null
           and c.pendingEffectiveFrom <= :now
    """)
    int promoteEffectivePendingSelections(@Param("now") LocalDateTime now);

}
