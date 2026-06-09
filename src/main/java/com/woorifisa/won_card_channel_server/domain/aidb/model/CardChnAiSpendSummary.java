package com.woorifisa.won_card_channel_server.domain.aidb.model;

import com.woorifisa.won_card_channel_server.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Builder
@Entity
@Table(
        name = "card_chn_ai_spend_summary",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_monthly_point_summary_user_base_month",
                        columnNames = {"user_uuid", "base_month"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardChnAiSpendSummary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Long summaryId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_uuid", nullable = false, columnDefinition = "CHAR(36)")
    private UUID userUuid;

    @Column(name = "base_month", nullable = false, length = 7)
    private String baseMonth;

    @Column(name = "total_spend_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalSpendAmount;

    @Column(name = "current_month_earned_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal currentMonthEarnedAmount;

    @Column(name = "point_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal pointAmount;

    @Column(name = "food_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal foodAmount;

    @Column(name = "shopping_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal shoppingAmount;

    @Column(name = "transport_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal transportAmount;

    @Column(name = "subscription_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal subscriptionAmount;

    @Column(name = "etc_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal etcAmount;

    @Column(name = "top_merchant_json", columnDefinition = "json")
    private String topMerchantJson;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;
}
