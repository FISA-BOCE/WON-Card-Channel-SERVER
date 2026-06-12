package com.woorifisa.won_card_channel_server.global.scheduler;

import com.woorifisa.won_card_channel_server.global.logging.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorLogCleanupScheduler {

    private final ErrorLogRepository errorLogRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void deleteExpiredLogs() {
        int deleted = errorLogRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(7));
        log.info("error log cleanup: deleted={}", deleted);
    }
}
