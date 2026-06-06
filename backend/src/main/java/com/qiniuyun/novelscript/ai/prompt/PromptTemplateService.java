package com.qiniuyun.novelscript.ai.prompt;

import com.qiniuyun.novelscript.config.ai.PromptProperties;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * 统一管理 Prompt 模板的加载与渲染。
 */
@Slf4j
@Service
public class PromptTemplateService {

    private final PromptProperties promptProperties;
    private final ResourceLoader resourceLoader;
    private final ConcurrentMap<String, String> templateCache = new ConcurrentHashMap<>();

    /**
     * 构造 Prompt 模板服务。
     *
     * @param promptProperties Prompt 配置
     * @param resourceLoader 资源加载器
     */
    public PromptTemplateService(PromptProperties promptProperties, ResourceLoader resourceLoader) {
        this.promptProperties = promptProperties;
        this.resourceLoader = resourceLoader;
    }

    /**
     * 按模板名称加载原始 Prompt 内容。
     *
     * @param templateName 模板名称，不含扩展名
     * @return 模板原文
     */
    public String load(String templateName) {
        validateTemplateName(templateName);
        return templateCache.computeIfAbsent(templateName, this::readTemplateContent);
    }

    /**
     * 按模板名称渲染 Prompt 内容。
     *
     * @param templateName 模板名称，不含扩展名
     * @param variables 模板变量
     * @return 渲染后的 Prompt
     */
    public String render(String templateName, Map<String, Object> variables) {
        Map<String, Object> safeVariables = variables == null ? Collections.emptyMap() : variables;
        String template = load(templateName);
        return new PromptTemplate(template).render(safeVariables);
    }

    /**
     * 校验模板名称是否为空。
     *
     * @param templateName 模板名称
     */
    private void validateTemplateName(String templateName) {
        if (!StringUtils.hasText(templateName)) {
            throw new IllegalArgumentException("Prompt 模板名称不能为空。");
        }
    }

    /**
     * 从类路径读取指定模板内容。
     *
     * @param templateName 模板名称
     * @return 模板原文
     */
    private String readTemplateContent(String templateName) {
        String resourcePath = promptProperties.getLocation() + templateName + ".md";
        Resource resource = resourceLoader.getResource(resourcePath);
        if (!resource.exists()) {
            throw new IllegalArgumentException("未找到 Prompt 模板：" + templateName);
        }

        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            String templateContent = FileCopyUtils.copyToString(reader);
            log.info("【Prompt 模板】已加载模板：{}", templateName);
            return templateContent;
        }
        catch (IOException exception) {
            throw new IllegalStateException("读取 Prompt 模板失败：" + templateName, exception);
        }
    }
}
