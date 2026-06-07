package com.qiniuyun.novelscript.service;

import com.qiniuyun.novelscript.pipeline.model.ScriptDocument;

/**
 * 正式剧本规则渲染服务。
 */
public interface ScreenplayRenderService {

    /**
     * 将 YAML 原文解析为结构化剧本文档。
     *
     * @param yamlContent YAML 原文
     * @return 结构化剧本文档
     */
    ScriptDocument parseScriptDocument(String yamlContent);

    /**
     * 将结构化剧本文档渲染为正式剧本 Markdown。
     *
     * @param scriptDocument 结构化剧本文档
     * @return 正式剧本 Markdown
     */
    String renderMarkdown(ScriptDocument scriptDocument);
}
