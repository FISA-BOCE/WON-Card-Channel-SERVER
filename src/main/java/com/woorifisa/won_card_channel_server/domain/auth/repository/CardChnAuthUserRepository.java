package com.woorifisa.won_card_channel_server.domain.auth.repository;

import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnAuthUser;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardChnAuthUserRepository extends JpaRepository<CardChnAuthUser, UUID> {

    Optional<CardChnAuthUser> findByUserUuid(UUID userUuid);

    Optional<CardChnAuthUser> findByTelHash(String telHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from CardChnAuthUser u where u.telHash = :telHash")
    Optional<CardChnAuthUser> findByTelHashForUpdate(String telHash);
}
