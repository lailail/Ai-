package com.qiniuyun.novelscript.ai.adapter;

/**
 * 统一的 AI 文本生成适配接口。
 */
public interface AiChatAdapter {

    /**
     * 使用系统提示词和用户提示词发起一次文本生成。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return 模型返回的文本内容
     */
    String chat(String systemPrompt, String userPrompt);
}
