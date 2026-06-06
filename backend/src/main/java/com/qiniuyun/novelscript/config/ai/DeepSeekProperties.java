package com.qiniuyun.novelscript.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DeepSeek 模型接入配置。
 */
@Data
@ConfigurationProperties(prefix = "novel-script.ai.deepseek")
public class DeepSeekProperties {

    /**
     * 是否启用 DeepSeek 模型调用。
     */
    private boolean enabled = false;

    /**
     * DeepSeek OpenAI 兼容接口地址。
     */
    private String baseUrl = "https://api.deepseek.com";

    /**
     * DeepSeek API Key。
     */
    private String apiKey;

    /**
     * 默认聊天模型名称。
     */
    private String model = "deepseek-chat";

    /**
     * 默认采样温度。
     */
    private Float temperature = 0.7F;

    /**
     * 可选的最大输出 token 数。
     */
    private Integer maxTokens;
}
