package com.qiniuyun.novelscript.config.ai;

import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import com.qiniuyun.novelscript.ai.adapter.DeepSeekAiChatAdapter;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * AI 基础设施装配配置。
 */
@Configuration
@EnableConfigurationProperties({DeepSeekProperties.class, PromptProperties.class})
public class AiInfrastructureConfiguration {

    /**
     * 在启用 DeepSeek 时创建底层 Spring AI ChatClient。
     *
     * @param properties DeepSeek 配置
     * @param restClientBuilder Spring Web 的 RestClient 构建器
     * @return OpenAI 兼容协议的聊天客户端
     */
    @Bean
    @ConditionalOnProperty(prefix = "novel-script.ai.deepseek", name = "enabled", havingValue = "true")
    public OpenAiChatClient deepSeekOpenAiChatClient(
        DeepSeekProperties properties,
        RestClient.Builder restClientBuilder
    ) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("已启用 DeepSeek，但未配置 DEEPSEEK_API_KEY。");
        }

        OpenAiApi openAiApi = new OpenAiApi(properties.getBaseUrl(), properties.getApiKey(), restClientBuilder);
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
            .withModel(properties.getModel())
            .withTemperature(properties.getTemperature());

        if (properties.getMaxTokens() != null) {
            optionsBuilder.withMaxTokens(properties.getMaxTokens());
        }

        return new OpenAiChatClient(openAiApi, optionsBuilder.build());
    }

    /**
     * 暴露统一的 AI 适配接口，供后续流水线步骤复用。
     *
     * @param properties DeepSeek 配置
     * @param chatClientProvider 可选的底层模型客户端
     * @return 统一 AI 适配器
     */
    @Bean
    public AiChatAdapter aiChatAdapter(
        DeepSeekProperties properties,
        ObjectProvider<OpenAiChatClient> chatClientProvider
    ) {
        return new DeepSeekAiChatAdapter(properties, chatClientProvider.getIfAvailable());
    }
}
