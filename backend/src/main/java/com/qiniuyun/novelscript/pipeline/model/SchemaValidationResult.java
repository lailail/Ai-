package com.qiniuyun.novelscript.pipeline.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * YAML Schema 校验结果。
 */
@Data
public class SchemaValidationResult {

    /**
     * 是否校验通过。
     */
    private boolean valid;

    /**
     * 校验错误列表。
     */
    private List<SchemaValidationError> errors = new ArrayList<>();
}
