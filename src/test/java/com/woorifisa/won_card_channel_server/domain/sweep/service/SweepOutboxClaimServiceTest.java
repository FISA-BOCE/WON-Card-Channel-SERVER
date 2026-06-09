package com.woorifisa.won_card_channel_server.domain.sweep.service;

import com.woorifisa.won_card_channel_server.domain.sweep.model.SweepOutbox;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.OutboxPublishStatus;
import com.woorifisa.won_card_channel_server.domain.sweep.model.enums.SweepEventType;
import com.woorifisa.won_card_channel_server.domain.sweep.repository.SweepOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class SweepOutboxClaimServiceTest {

    @Mock
    private SweepOutboxRepository outboxRepository;

    private SweepOutboxClaimService claimService;

    @BeforeEach
    void setUp() {
        claimService = new SweepOutboxClaimService(outboxRepository);
    }

    @Test
    @DisplayName("발행 대상 Outbox를 조회해 PROCESSING 상태로 선점한다")
    void claimPublishTargets() {
        // given
        SweepOutbox outbox = createPendingOutbox();
        setField(outbox, "outboxEventId", 1L);

        when(outboxRepository.findPublishTargets(
                any(),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(outbox));

        // when
        List<Long> result = claimService.claimPublishTargets(20);

        // then
        assertThat(result).containsExactly(1L);
        assertThat(outbox.getPublishStatus()).isEqualTo(OutboxPublishStatus.PROCESSING);
    }

    private SweepOutbox createPendingOutbox() {
        return SweepOutbox.pending(
                2L,
                "CARD-SWEEP-988351d5-6242-4299-86ef-465cd9809874",
                SweepEventType.SWEEP_REQUESTED,
                "{\"eventType\":\"SWEEP_REQUESTED\",\"pointLedgerId\":1,\"etfId\":100}",
                "correlation-id",
                "SWEEP:POINT_LEDGER:1"
        );
    }
}
