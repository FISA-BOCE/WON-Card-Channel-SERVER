package com.woorifisa.won_card_channel_server.domain.aidb.repository;

import com.woorifisa.won_card_channel_server.domain.aidb.model.CardChnAiSpendSummary;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardChnAiSpendSummaryRepository extends JpaRepository<CardChnAiSpendSummary, Long> {

    @Query("""
            select s.totalSpendAmount as totalSpendAmount,
                   s.foodAmount as foodAmount,
                   s.shoppingAmount as shoppingAmount,
                   s.transportAmount as transportAmount,
                   s.subscriptionAmount as subscriptionAmount,
                   s.etcAmount as etcAmount
            from CardChnAiSpendSummary s
            where s.userUuid = :userUuid
              and s.baseMonth = :baseMonth
            """)
    Optional<SpendAmountSummary> findSpendAmountSummaryByUserUuidAndBaseMonth(
            @Param("userUuid") UUID userUuid,
            @Param("baseMonth") String baseMonth
    );

    @Query("""
            select coalesce(sum(s.currentMonthEarnedAmount - s.pointAmount), 0)
            from CardChnAiSpendSummary s
            where s.userUuid = :userUuid
              and s.baseMonth <= :baseMonth
            """)
    BigDecimal calculateCurrentPointAmount(
            @Param("userUuid") UUID userUuid,
            @Param("baseMonth") String baseMonth
    );

    boolean existsByUserUuidAndBaseMonthLessThanEqual(UUID userUuid, String baseMonth);

    @Query("""
            select s.currentMonthEarnedAmount
            from CardChnAiSpendSummary s
            where s.userUuid = :userUuid
              and s.baseMonth = :baseMonth
            """)
    Optional<BigDecimal> findCurrentMonthEarnedAmountByUserUuidAndBaseMonth(
            @Param("userUuid") UUID userUuid,
            @Param("baseMonth") String baseMonth
    );

    interface SpendAmountSummary {
        BigDecimal getTotalSpendAmount();

        BigDecimal getFoodAmount();

        BigDecimal getShoppingAmount();

        BigDecimal getTransportAmount();

        BigDecimal getSubscriptionAmount();

        BigDecimal getEtcAmount();
    }
}
