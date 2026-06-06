package com.qiniuyun.novelscript.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.qiniuyun.novelscript.ai.adapter.AiChatAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 验证本机 DeepSeek 环境变量配置正确后，适配器可以真实调用模型。
 */
@SpringBootTest(properties = "novel-script.ai.deepseek.enabled=true")
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class DeepSeekAiChatAdapterIntegrationTests {

    @Autowired
    private AiChatAdapter aiChatAdapter;

    @Test
    void shouldCallDeepSeekSuccessfullyWhenApiKeyIsConfigured() {
        String result = aiChatAdapter.chat(
            "你是一个简洁的测试助手，只返回一句很短的话。",
            "请只回复：DeepSeek连接成功"
        );

        assertThat(result).isNotBlank();
    }
}
