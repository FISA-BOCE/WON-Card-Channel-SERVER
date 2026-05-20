package com.woorifisa.won_card_channel_server.domain.auth.repository;

import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface CardChnAuthSessionRepository extends JpaRepository<CardChnAuthSession, Long> {

    Optional<CardChnAuthSession> findByRefreshTokenHash(String refreshTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CardChnAuthSession s join fetch s.authUser where s.refreshTokenHash = :refreshTokenHash")
    Optional<CardChnAuthSession> findByRefreshTokenHashForUpdate(String refreshTokenHash);

    Optional<CardChnAuthSession> findByAccessTokenJti(String accessTokenJti);

    Optional<CardChnAuthSession> findByAuthUser_AuthUserUuid(UUID authUserUuid);
}
