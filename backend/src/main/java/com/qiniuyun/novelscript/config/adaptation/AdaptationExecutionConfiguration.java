package com.qiniuyun.novelscript.config.adaptation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 改编任务执行器配置。
 */
@Configuration
@EnableConfigurationProperties(AdaptationExecutionProperties.class)
public class AdaptationExecutionConfiguration {

    /**
     * 创建改编任务执行器。
     *
     * @param properties 改编执行配置
     * @return 任务执行器
     */
    @Bean("adaptationTaskExecutor")
    public TaskExecutor adaptationTaskExecutor(AdaptationExecutionProperties properties) {
        if (!properties.isAsyncEnabled()) {
            return new SyncTaskExecutor();
        }

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.initialize();
        return executor;
    }
}
