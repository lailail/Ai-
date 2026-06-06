package com.qiniuyun.novelscript;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 小说转剧本工具后端启动入口。
 */
@MapperScan("com.qiniuyun.novelscript.mapper")
@SpringBootApplication
public class NovelScriptApplication {

    /**
     * 启动后端应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(NovelScriptApplication.class, args);
    }
}
