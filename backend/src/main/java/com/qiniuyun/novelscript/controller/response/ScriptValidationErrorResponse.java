package com.qiniuyun.novelscript.controller.response;

import com.qiniuyun.novelscript.pipeline.model.SchemaValidationError;
import lombok.Data;

/**
 * 向前端返回单条 YAML 校验错误信息的响应对象。
 */
@Data
public class ScriptValidationErrorResponse {

    /** 错误字段路径。 */
    private String path;

    /** 错误说明。 */
    private String message;

    /**
     * 根据内部校验错误对象构建响应结果。
     *
     * @param error 内部校验错误对象
     * @return 面向前端的错误响应
     */
    public static ScriptValidationErrorResponse from(SchemaValidationError error) {
        ScriptValidationErrorResponse response = new ScriptValidationErrorResponse();
        response.setPath(error.getPath());
        response.setMessage(error.getMessage());
        return response;
    }
}
