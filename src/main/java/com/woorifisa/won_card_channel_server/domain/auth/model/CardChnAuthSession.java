package com.woorifisa.won_card_channel_server.domain.auth.model;

import com.woorifisa.won_card_channel_server.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@Table(
        name = "card_chn_auth_session",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_card_chn_auth_session_refresh", columnNames = "refresh_token_hash"),
                @UniqueConstraint(name = "uk_card_chn_auth_session_auth_user", columnNames = "auth_user_uuid")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardChnAuthSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_user_uuid", nullable = false)
    private CardChnAuthUser authUser;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "access_token_jti", nullable = false, length = 64)
    private String accessTokenJti;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Version
    private Long version;

    public void rotate(String refreshTokenHash, String accessTokenJti, LocalDateTime expiredAt) {
        this.refreshTokenHash = refreshTokenHash;
        this.accessTokenJti = accessTokenJti;
        this.expiredAt = expiredAt;
    }
}
