package com.woorifisa.won_card_channel_server.domain.card.model;

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
        name = "card_chn_card_summary",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_card_summary_user_uuid", columnNames = "user_uuid"),
                @UniqueConstraint(name = "uk_card_summary_card_user_uuid", columnNames = "card_user_uuid"),
                @UniqueConstraint(name = "uk_card_summary_card_uuid", columnNames = "card_uuid")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardChnCardSummary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_summary_id")
    private Long cardSummaryId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "user_uuid", nullable = false, columnDefinition = "CHAR(36)")
    private UUID userUuid;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "card_user_uuid", nullable = false, columnDefinition = "CHAR(36)")
    private UUID cardUserUuid;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "card_uuid", nullable = false, columnDefinition = "CHAR(36)")
    private UUID cardUuid;

    @Column(name = "card_name", nullable = false, length = 100)
    private String cardName;

    @Column(name = "card_no_display", nullable = false, length = 50)
    private String cardNoDisplay;

    @Column(name = "card_status", nullable = false, length = 30)
    private String cardStatus;

    @Column(name = "current_month_usage_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal currentMonthUsageAmount;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;
}
