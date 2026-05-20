package com.woorifisa.won_card_channel_server.domain.auth.model;

import com.woorifisa.won_card_channel_server.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "card_chn_auth_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardChnAuthUser extends BaseTimeEntity {

    @Id
    @Column(name = "auth_user_uuid", nullable = false, length = 36)
    private String authUserUuid;

    @Column(name = "card_user_uuid", length = 36)
    private String cardUserUuid;

    @Column(name = "user_uuid", nullable = false, length = 36, unique = true)
    private String userUuid;

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "email_enc")
    private String emailEnc;

    @Column(name = "tel_enc")
    private String telEnc;

    @Column(name = "tel_hash", length = 64, unique = true)
    private String telHash;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 20)
    private UserStatus userStatus;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    public void updateLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void updateEmailEnc(String emailEnc) {
        this.emailEnc = emailEnc;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void withdraw(LocalDateTime withdrawnAt) {
        this.userStatus = UserStatus.DEACTIVATE;
        this.deactivatedAt = withdrawnAt;
    }
}
