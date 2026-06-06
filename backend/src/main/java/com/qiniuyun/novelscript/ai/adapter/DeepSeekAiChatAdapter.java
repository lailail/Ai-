package com.qiniuyun.novelscript.ai.adapter;

import java.util.List;

import com.qiniuyun.novelscript.config.ai.DeepSeekProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.util.StringUtils;

/**
 * 基于 Spring AI 的 DeepSeek 聊天适配器。
 */
@Slf4j
public class DeepSeekAiChatAdapter implements AiChatAdapter {

    private final DeepSeekProperties deepSeekProperties;
    private final OpenAiChatClient openAiChatClient;

    public DeepSeekAiChatAdapter(DeepSeekProperties deepSeekProperties, OpenAiChatClient openAiChatClient) {
        this.deepSeekProperties = deepSeekProperties;
        this.openAiChatClient = openAiChatClient;
    }

    /**
     * 调用 DeepSeek 生成文本结果。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return 模型返回文本
     */
    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (openAiChatClient == null || !deepSeekProperties.isEnabled()) {
            throw new IllegalStateException("DeepSeek 适配器未启用，请检查 novel-script.ai.deepseek.enabled 配置。");
        }

        log.info("【AI 适配器】开始调用 DeepSeek 模型，模型名：{}", deepSeekProperties.getModel());
        ChatResponse response = openAiChatClient.call(
            new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)))
        );

        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("DeepSeek 返回为空，无法继续后续改编流程。");
        }

        String content = response.getResult().getOutput().getContent();
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("DeepSeek 返回内容为空，无法继续后续改编流程。");
        }

        log.info("【AI 适配器】DeepSeek 调用完成，返回字符数：{}", content.length());
        return content;
    }
}
