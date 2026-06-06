package com.qiniuyun.novelscript;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用启动基础测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class NovelScriptApplicationTests {

    /**
     * 验证 Spring 上下文可以正常加载。
     */
    @Test
    void contextLoads() {
    }
}
