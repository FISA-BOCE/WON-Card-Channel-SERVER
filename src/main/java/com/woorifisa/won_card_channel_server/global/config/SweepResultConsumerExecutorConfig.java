package com.woorifisa.won_card_channel_server.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@RequiredArgsConstructor
public class SweepResultConsumerExecutorConfig {

    private final SweepResultConsumerProperties properties;

    @Bean(name = "sweepResultConsumerExecutor")
    public ThreadPoolTaskExecutor sweepResultConsumerExecutor() {
        int workerCount = properties.workerCount();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workerCount);
        executor.setMaxPoolSize(workerCount);
        executor.setQueueCapacity(workerCount * 4);
        executor.setThreadNamePrefix("sweep-result-consumer-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        return executor;
    }
}
