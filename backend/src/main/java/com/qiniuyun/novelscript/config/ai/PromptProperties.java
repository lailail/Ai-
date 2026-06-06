package com.qiniuyun.novelscript.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Prompt 模板资源配置。
 */
@Data
@ConfigurationProperties(prefix = "novel-script.ai.prompt")
public class PromptProperties {

    /**
     * Prompt 模板所在的 classpath 目录。
     */
    private String location = "classpath:prompts/";
}
