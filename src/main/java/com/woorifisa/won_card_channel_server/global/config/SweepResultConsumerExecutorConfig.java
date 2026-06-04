package com.woorifisa.won_card_channel_server.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@RequiredArgsConstructor
public class SweepResultConsumerExecutorConfig {

    private final SweepResultConsumerProperties properties;

    @Bean(name = "sweepResultConsumerExecutor")
    public ThreadPoolExecutor sweepResultConsumerExecutor() {
        int workerCount = properties.workerCount();
        AtomicInteger sequence = new AtomicInteger(1);

        return new ThreadPoolExecutor(
                workerCount,
                workerCount,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(workerCount * 4),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("sweep-result-consumer-" + sequence.getAndIncrement());
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
