package com.woorifisa.won_card_channel_server.domain.ai.spend.repository;

import com.woorifisa.won_card_channel_server.domain.ai.spend.model.SpendSummary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class SpendSummaryRepository {

    private final JdbcTemplate cardAiJdbcTemplate;

    public SpendSummaryRepository(@Qualifier("cardAiJdbcTemplate") JdbcTemplate cardAiJdbcTemplate) {
        this.cardAiJdbcTemplate = cardAiJdbcTemplate;
    }

    private static final String SELECT_LATEST =
            "SELECT user_uuid, base_month, total_spend_amount, food_amount, shopping_amount, " +
            "transport_amount, subscription_amount, etc_amount, top_merchant_json, " +
            "point_balance_amount, current_month_spend_amount, reward_rate, performance_status " +
            "FROM card_chn_ai_spend_summary " +
            "WHERE user_uuid = ? " +
            "ORDER BY base_month DESC LIMIT 1";

    public Optional<SpendSummary> findLatestByUserUuid(UUID userUuid) {
        return cardAiJdbcTemplate.query(SELECT_LATEST, rowMapper(), userUuid.toString())
                .stream().findFirst();
    }

    private RowMapper<SpendSummary> rowMapper() {
        return (rs, rowNum) -> SpendSummary.builder()
                .userUuid(UUID.fromString(rs.getString("user_uuid")))
                .baseMonth(rs.getString("base_month"))
                .totalSpendAmount(rs.getLong("total_spend_amount"))
                .foodAmount(rs.getLong("food_amount"))
                .shoppingAmount(rs.getLong("shopping_amount"))
                .transportAmount(rs.getLong("transport_amount"))
                .subscriptionAmount(rs.getLong("subscription_amount"))
                .etcAmount(rs.getLong("etc_amount"))
                .topMerchantJson(rs.getString("top_merchant_json"))
                .pointBalanceAmount(rs.getLong("point_balance_amount"))
                .currentMonthSpendAmount(rs.getLong("current_month_spend_amount"))
                .rewardRate(rs.getDouble("reward_rate"))
                .performanceStatus(rs.getString("performance_status"))
                .build();
    }
}
