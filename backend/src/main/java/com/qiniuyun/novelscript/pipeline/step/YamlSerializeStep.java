package com.qiniuyun.novelscript.pipeline.step;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.qiniuyun.novelscript.pipeline.model.ScriptDocument;
import org.springframework.stereotype.Component;

/**
 * 负责将结构化剧本对象序列化为 YAML。
 */
@Component
public class YamlSerializeStep {

    private final YAMLMapper yamlMapper;

    /**
     * 构造 YAML 序列化步骤。
     */
    public YamlSerializeStep() {
        this.yamlMapper = YAMLMapper.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .disable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .findAndAddModules()
            .build();
        this.yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * 将剧本文档序列化为 YAML 文本。
     *
     * @param scriptDocument 结构化剧本文档
     * @return YAML 文本
     */
    public String execute(ScriptDocument scriptDocument) {
        if (scriptDocument == null) {
            throw new IllegalArgumentException("YAML 序列化时剧本文档不能为空。");
        }

        try {
            return yamlMapper.writeValueAsString(scriptDocument);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("剧本文档序列化为 YAML 失败。", exception);
        }
    }
}
