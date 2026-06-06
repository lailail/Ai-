package com.qiniuyun.novelscript.controller.response;

import com.qiniuyun.novelscript.pipeline.model.SchemaValidationResult;
import java.util.List;
import lombok.Data;

/**
 * 向前端返回 YAML 校验结果的响应对象。
 */
@Data
public class ScriptValidationResponse {

    /** 当前项目 ID。 */
    private Long projectId;

    /** 当前使用的 Schema 版本。 */
    private String schemaVersion;

    /** 是否校验通过。 */
    private boolean valid;

    /** 结构化错误列表。 */
    private List<ScriptValidationErrorResponse> errors;

    /**
     * 根据内部校验结果构建响应对象。
     *
     * @param projectId 项目 ID
     * @param schemaVersion Schema 版本
     * @param validationResult 内部校验结果
     * @return 面向前端的校验响应
     */
    public static ScriptValidationResponse from(
        Long projectId,
        String schemaVersion,
        SchemaValidationResult validationResult
    ) {
        ScriptValidationResponse response = new ScriptValidationResponse();
        response.setProjectId(projectId);
        response.setSchemaVersion(schemaVersion);
        response.setValid(validationResult.isValid());
        response.setErrors(validationResult.getErrors().stream().map(ScriptValidationErrorResponse::from).toList());
        return response;
    }
}
