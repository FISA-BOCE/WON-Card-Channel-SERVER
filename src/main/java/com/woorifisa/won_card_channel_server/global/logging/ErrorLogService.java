package com.woorifisa.won_card_channel_server.global.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorLogService {

    private final ErrorLogRepository errorLogRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${slack.webhook-url:}")
    private String slackWebhookUrl;

    @Transactional
    public void record(int status, String method, String uri, long elapsedMs) {
        errorLogRepository.save(ErrorLog.builder()
                .status(status)
                .method(method)
                .uri(uri)
                .elapsedMs(elapsedMs)
                .build());

        if (status >= 500 && slackWebhookUrl != null && !slackWebhookUrl.isBlank()) {
            sendSlackAlert(status, method, uri, elapsedMs);
        }
    }

    @Transactional(readOnly = true)
    public Page<ErrorLog> findAll(Pageable pageable) {
        return errorLogRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ErrorLog> findByStatus(int status, Pageable pageable) {
        return errorLogRepository.findByStatus(status, pageable);
    }

    private void sendSlackAlert(int status, String method, String uri, long elapsedMs) {
        String payload = String.format(
                "{\"text\": \"[CARD-CHANNEL] %d ERROR\\n%s %s (elapsed: %dms)\"}",
                status, method, uri, elapsedMs
        );
        webClientBuilder.build()
                .post()
                .uri(slackWebhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        r -> {},
                        e -> log.warn("slack alert failed: {}", e.getMessage())
                );
    }
}
