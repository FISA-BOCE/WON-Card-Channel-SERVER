package com.woorifisa.won_card_channel_server.domain.auth.repository;

import com.woorifisa.won_card_channel_server.domain.auth.model.CardChnTokenBlacklist;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardChnTokenBlacklistRepository extends JpaRepository<CardChnTokenBlacklist, String> {

    void deleteByExpiredAtBefore(LocalDateTime now);
}
