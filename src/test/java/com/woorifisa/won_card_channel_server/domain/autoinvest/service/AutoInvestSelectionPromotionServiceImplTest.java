package com.woorifisa.won_card_channel_server.domain.autoinvest.service;

import com.woorifisa.won_card_channel_server.domain.card.repository.CardChnCardSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutoInvestSelectionPromotionServiceImplTest {

    private CardChnCardSummaryRepository cardSummaryRepository;
    private AutoInvestSelectionPromotionServiceImpl service;

    @BeforeEach
    void setUp() {
        cardSummaryRepository = mock(CardChnCardSummaryRepository.class);
        service = new AutoInvestSelectionPromotionServiceImpl(cardSummaryRepository);
    }

    @Test
    @DisplayName("기준 시각까지 적용 가능한 예약 ETF를 승격하고 반영 건수를 반환한다")
    void promoteEffectivePendingSelections() {
        // given
        LocalDateTime batchStartedAt = LocalDateTime.of(2026, 6, 16, 0, 30);
        when(cardSummaryRepository.promoteEffectivePendingSelections(batchStartedAt))
                .thenReturn(3);

        // when
        int promotedCount = service.promoteEffectivePendingSelections(batchStartedAt);

        // then
        assertThat(promotedCount).isEqualTo(3);
        verify(cardSummaryRepository).promoteEffectivePendingSelections(batchStartedAt);
    }
}
