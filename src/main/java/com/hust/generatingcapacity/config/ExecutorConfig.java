package com.hust.generatingcapacity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ExecutorConfig {

    @Bean("generationExecutor")
    public Executor generationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);      // 核心线程数
        executor.setMaxPoolSize(20);       // 最大线程数
        executor.setQueueCapacity(100);    // 队列长度
        executor.setThreadNamePrefix("GenTask-"); // 线程名前缀
        executor.initialize();
        return executor;
    }
}
