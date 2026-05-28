package com.woorifisa.won_card_channel_server.domain.performance.model;

import com.woorifisa.won_card_channel_server.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        name = "card_chn_performance_summary",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_card_chn_performance_summary_performance_id",
                        columnNames = "performance_id"
                ),
                @UniqueConstraint(
                        name = "uk_card_chn_performance_summary_card_user_base_month",
                        columnNames = {"card_user_uuid", "base_month"}
                )
        },
        indexes = {
                @Index(name = "idx_card_chn_performance_summary_user_uuid", columnList = "user_uuid"),
                @Index(name = "idx_card_chn_performance_summary_card_user_uuid", columnList = "card_user_uuid"),
                @Index(name = "idx_card_chn_performance_summary_base_month", columnList = "base_month")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardChnPerformanceSummary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "performance_summary_id")
    private Long performanceSummaryId;

    @Column(name = "performance_id", nullable = false)
    private Long performanceId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_uuid", nullable = false, columnDefinition = "CHAR(36)")
    private UUID userUuid;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "card_user_uuid", nullable = false, columnDefinition = "CHAR(36)")
    private UUID cardUserUuid;

    @Column(name = "base_month", nullable = false, length = 7, columnDefinition = "CHAR(7)")
    private String baseMonth;

    @Column(name = "previous_month_spend_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal previousMonthSpendAmount;

    @Column(name = "current_month_spend_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal currentMonthSpendAmount;

    @Column(name = "reward_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal rewardRate;

    @Column(name = "reward_point_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal rewardPointAmount;

    @Column(name = "performance_status", length = 1, columnDefinition = "CHAR(1)")
    private String performanceStatus;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;
}
