package com.woorifisa.won_card_channel_server.global.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ExecutorMetricsConfig {

    @Bean
    public MeterBinder sweepResultConsumerExecutorMetrics(
            @Qualifier("sweepResultConsumerExecutor") ThreadPoolTaskExecutor executor
    ) {
        return registry -> {
            ThreadPoolExecutor threadPool = executor.getThreadPoolExecutor();
            String executorName = "sweep-result-consumer";

            Gauge.builder("app.executor.threads.active", threadPool, ThreadPoolExecutor::getActiveCount)
                    .tag("name", executorName)
                    .description("Active threads in the executor")
                    .register(registry);
            Gauge.builder("app.executor.threads.current", threadPool, ThreadPoolExecutor::getPoolSize)
                    .tag("name", executorName)
                    .description("Current threads in the executor")
                    .register(registry);
            Gauge.builder("app.executor.threads.max", threadPool, ThreadPoolExecutor::getMaximumPoolSize)
                    .tag("name", executorName)
                    .description("Maximum threads configured for the executor")
                    .register(registry);
            Gauge.builder("app.executor.queue.size", threadPool, pool -> pool.getQueue().size())
                    .tag("name", executorName)
                    .description("Queued tasks waiting for executor threads")
                    .register(registry);
        };
    }
}
