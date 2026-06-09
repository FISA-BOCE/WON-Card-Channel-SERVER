package com.woorifisa.won_card_channel_server.domain.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@Table(name = "card_chn_token_blacklist")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardChnTokenBlacklist {

    @Id
    @Column(name = "access_token_jti", nullable = false, length = 64)
    private String accessTokenJti;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;
}
