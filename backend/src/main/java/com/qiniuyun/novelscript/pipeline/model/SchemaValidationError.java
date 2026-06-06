package com.qiniuyun.novelscript.pipeline.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * YAML Schema 校验错误。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaValidationError {

    /**
     * 错误字段路径。
     */
    private String path;

    /**
     * 错误说明。
     */
    private String message;
}
