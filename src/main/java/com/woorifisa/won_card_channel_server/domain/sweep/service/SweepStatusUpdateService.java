package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.event.SweepInvestmentResultEvent;
import com.woorifisa.won_card_channel_server.domain.sweep.exception.code.SweepErrorCode;
import com.woorifisa.won_card_channel_server.domain.sweep.model.Sweep;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepRepository;
import com.woorifisa.won_card_channel_server.global.exception.handler.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SweepStatusUpdateService {

    private final SweepRepository sweepRequestRepository;

    @Transactional
    public void update(SweepInvestmentResultEvent event) {
        Sweep sweep = sweepRequestRepository.findByIdempotencyKey(event.idempotencyKey())
                .orElseThrow(() -> new BusinessException(SweepErrorCode.SWEEP_REWARD_LEDGER_NOT_FOUND));

        if (event.completed()) {
            sweep.markSucceeded();
            return;
        }

        sweep.markFailed(event.failureMessage());
    }
}
