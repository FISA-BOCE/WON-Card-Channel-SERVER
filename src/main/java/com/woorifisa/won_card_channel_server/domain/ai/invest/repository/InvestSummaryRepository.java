package com.woorifisa.won_card_channel_server.domain.ai.invest.repository;

import com.woorifisa.won_card_channel_server.domain.ai.invest.model.InvestSummary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class InvestSummaryRepository {

    private final JdbcTemplate securitiesAiJdbcTemplate;

    public InvestSummaryRepository(@Qualifier("securitiesAiJdbcTemplate") JdbcTemplate securitiesAiJdbcTemplate) {
        this.securitiesAiJdbcTemplate = securitiesAiJdbcTemplate;
    }

    private static final String SELECT_BY_USER =
            "SELECT user_uuid, invest_account_uuid, total_buy_amount, profit_loss_amount, etf_summary_json " +
            "FROM invest_chn_ai_summary " +
            "WHERE user_uuid = ? " +
            "LIMIT 1";

    public Optional<InvestSummary> findByUserUuid(UUID userUuid) {
        return securitiesAiJdbcTemplate.query(SELECT_BY_USER, rowMapper(), userUuid.toString())
                .stream().findFirst();
    }

    private RowMapper<InvestSummary> rowMapper() {
        return (rs, rowNum) -> InvestSummary.builder()
                .userUuid(UUID.fromString(rs.getString("user_uuid")))
                .investAccountUuid(UUID.fromString(rs.getString("invest_account_uuid")))
                .totalBuyAmount(rs.getLong("total_buy_amount"))
                .profitLossAmount(rs.getLong("profit_loss_amount"))
                .etfSummaryJson(rs.getString("etf_summary_json"))
                .build();
    }
}
