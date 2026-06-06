package com.qiniuyun.novelscript.config.adaptation;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 改编任务执行配置。
 */
@Data
@ConfigurationProperties(prefix = "novel-script.adaptation")
public class AdaptationExecutionProperties {

    /**
     * 是否启用异步执行。
     */
    private boolean asyncEnabled = true;

    /**
     * 核心线程数。
     */
    private int corePoolSize = 2;

    /**
     * 最大线程数。
     */
    private int maxPoolSize = 4;

    /**
     * 队列容量。
     */
    private int queueCapacity = 8;

    /**
     * 线程名前缀。
     */
    private String threadNamePrefix = "adaptation-";
}
