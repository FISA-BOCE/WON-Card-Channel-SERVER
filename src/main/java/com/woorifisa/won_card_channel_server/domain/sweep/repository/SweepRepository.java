package com.woorifisa.won_card_channel_server.domain.sweep.repository;

import com.woorifisa.won_card_channel_server.domain.sweep.model.Sweep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SweepRepository extends JpaRepository<Sweep, Long> {
    boolean existsByPointLedgerId(Long pointLedgerId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Sweep> findByIdempotencyKey(String idempotencyKey);

}
