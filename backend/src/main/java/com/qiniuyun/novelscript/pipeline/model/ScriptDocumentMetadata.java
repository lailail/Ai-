package com.qiniuyun.novelscript.pipeline.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * YAML 剧本中的元信息。
 */
@Data
public class ScriptDocumentMetadata {

    /**
     * 生成时间。
     */
    @JsonProperty("generated_at")
    @JsonAlias("generatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime generatedAt;

    /**
     * 生成器标识。
     */
    private String generator;

    /**
     * 备注列表。
     */
    private List<String> notes = new ArrayList<>();
}
