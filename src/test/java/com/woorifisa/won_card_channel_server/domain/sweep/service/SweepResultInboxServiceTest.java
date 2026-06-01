package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.dto.command.InboxClaimResult;
import com.woorifisa.won_card_channel_server.domain.sweep.dto.event.SweepInvestmentResultEvent;
import com.woorifisa.won_card_channel_server.domain.sweep.model.CardChnSweepInbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.CardChnInboxEventRepository;
import com.woorifisa.won_card_channel_server.global.config.SweepResultConsumerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SweepResultInboxServiceTest {

    private CardChnInboxEventRepository inboxRepository;
    private SweepResultInboxService service;

    @BeforeEach
    void setUp() {
        inboxRepository = mock(CardChnInboxEventRepository.class);
        service = new SweepResultInboxService(
                inboxRepository,
                new SweepResultConsumerProperties(true, 10000, 10, 5, 300)
        );
    }

    @Test
    @DisplayName("처음 수신한 결과 이벤트는 inbox에 저장하고 PROCESSING으로 선점한다")
    void claimNewEvent() {
        SweepInvestmentResultEvent event = event();

        when(inboxRepository.findByIdempotencyKeyForUpdate(event.idempotencyKey()))
                .thenReturn(Optional.empty());
        when(inboxRepository.save(any(CardChnSweepInbox.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InboxClaimResult result = service.claim(event, "{\"eventId\":\"INVEST-SWEEP-TEST-1\"}");

        assertThat(result.claimed()).isTrue();
        assertThat(result.alreadyProcessed()).isFalse();
        verify(inboxRepository).save(any(CardChnSweepInbox.class));
    }

    @Test
    @DisplayName("이미 처리 완료된 inbox는 재처리하지 않는다")
    void claimAlreadyProcessed() {
        CardChnSweepInbox inbox = CardChnSweepInbox.received(
                "INVEST-SWEEP-TEST-1",
                2L,
                SweepEventType.SWEEP_INVESTMENT_COMPLETED,
                "{}",
                "CORR-SWEEP-TEST-1",
                "SWEEP:POINT_LEDGER:1"
        );
        inbox.markProcessed();

        when(inboxRepository.findByIdempotencyKeyForUpdate("SWEEP:POINT_LEDGER:1"))
                .thenReturn(Optional.of(inbox));

        InboxClaimResult result = service.claim(event(), "{}");

        assertThat(result.claimed()).isFalse();
        assertThat(result.alreadyProcessed()).isTrue();
        verify(inboxRepository, never()).save(any());
    }

    private SweepInvestmentResultEvent event() {
        return new SweepInvestmentResultEvent(
                "INVEST-SWEEP-TEST-1",
                SweepEventType.SWEEP_INVESTMENT_COMPLETED,
                "CORR-SWEEP-TEST-1",
                "SWEEP:POINT_LEDGER:1",
                2L,
                1L,
                1L,
                UUID.fromString("a5324ba5-0ee3-44c6-b3d5-a951f9e94df5"),
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "2026-05",
                1000L,
                1000L,
                100L,
                null,
                null
        );
    }
}
