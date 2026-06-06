package com.qiniuyun.novelscript.controller.response;

import com.qiniuyun.novelscript.domain.entity.AdaptationJob;
import com.qiniuyun.novelscript.domain.entity.ScriptVersion;
import com.qiniuyun.novelscript.domain.entity.YamlSnapshot;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 剧本生成、查询和编辑工作区共用的剧本详情响应对象。
 */
@Data
public class AdaptationScriptResponse {

    /** 项目 ID。 */
    private Long projectId;

    /** 剧本版本 ID。 */
    private Long scriptVersionId;

    /** 版本号。 */
    private Integer versionNo;

    /** 当前剧本标题。 */
    private String title;

    /** 版本来源类型。 */
    private String sourceType;

    /** YAML Schema 版本号。 */
    private String schemaVersion;

    /** Schema 校验状态。 */
    private String validationStatus;

    /** 剧本 YAML 原文。 */
    private String yamlContent;

    /** 结构化校验错误列表。 */
    private List<ScriptValidationErrorResponse> validationErrors;

    /** 版本创建时间。 */
    private LocalDateTime createdAt;

    /** 关联任务 ID。 */
    private Long jobId;

    /** 任务状态。 */
    private String jobStatus;

    /**
     * 根据落库结果构建基础剧本详情响应。
     *
     * @param projectId 项目 ID
     * @param scriptVersion 剧本版本实体
     * @param yamlSnapshot YAML 快照实体
     * @param adaptationJob 改编任务实体
     * @return 接口响应对象
     */
    public static AdaptationScriptResponse from(
        Long projectId,
        ScriptVersion scriptVersion,
        YamlSnapshot yamlSnapshot,
        AdaptationJob adaptationJob
    ) {
        return from(projectId, scriptVersion, yamlSnapshot, adaptationJob, List.of());
    }

    /**
     * 根据落库结果构建包含校验错误的剧本详情响应。
     *
     * @param projectId 项目 ID
     * @param scriptVersion 剧本版本实体
     * @param yamlSnapshot YAML 快照实体
     * @param adaptationJob 改编任务实体
     * @param validationErrors 结构化校验错误列表
     * @return 接口响应对象
     */
    public static AdaptationScriptResponse from(
        Long projectId,
        ScriptVersion scriptVersion,
        YamlSnapshot yamlSnapshot,
        AdaptationJob adaptationJob,
        List<ScriptValidationErrorResponse> validationErrors
    ) {
        AdaptationScriptResponse response = new AdaptationScriptResponse();
        response.setProjectId(projectId);
        response.setScriptVersionId(scriptVersion.getId());
        response.setVersionNo(scriptVersion.getVersionNo());
        response.setTitle(scriptVersion.getTitle());
        response.setSourceType(scriptVersion.getSourceType());
        response.setSchemaVersion(yamlSnapshot.getSchemaVersion());
        response.setValidationStatus(yamlSnapshot.getValidationStatus());
        response.setYamlContent(yamlSnapshot.getYamlContent());
        response.setValidationErrors(validationErrors);
        response.setCreatedAt(scriptVersion.getCreatedAt());
        if (adaptationJob != null) {
            response.setJobId(adaptationJob.getId());
            response.setJobStatus(adaptationJob.getStatus());
        }
        return response;
    }
}
