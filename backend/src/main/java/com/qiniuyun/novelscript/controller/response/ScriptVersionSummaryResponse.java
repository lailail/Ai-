package com.qiniuyun.novelscript.controller.response;

import com.qiniuyun.novelscript.domain.entity.ScriptVersion;
import com.qiniuyun.novelscript.domain.entity.YamlSnapshot;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 剧本版本列表项响应对象。
 */
@Data
public class ScriptVersionSummaryResponse {

    /** 当前项目 ID。 */
    private Long projectId;

    /** 剧本版本 ID。 */
    private Long scriptVersionId;

    /** 版本号。 */
    private Integer versionNo;

    /** 版本标题。 */
    private String title;

    /** 版本来源类型。 */
    private String sourceType;

    /** Schema 校验状态。 */
    private String validationStatus;

    /** 是否为当前最新版本。 */
    private boolean latest;

    /** 版本创建时间。 */
    private LocalDateTime createdAt;

    /**
     * 根据剧本版本实体构建列表项响应。
     *
     * @param projectId 项目 ID
     * @param scriptVersion 剧本版本实体
     * @param yamlSnapshot 对应的 YAML 快照
     * @param latest 是否为当前最新版本
     * @return 剧本版本列表项响应
     */
    public static ScriptVersionSummaryResponse from(
        Long projectId,
        ScriptVersion scriptVersion,
        YamlSnapshot yamlSnapshot,
        boolean latest
    ) {
        ScriptVersionSummaryResponse response = new ScriptVersionSummaryResponse();
        response.setProjectId(projectId);
        response.setScriptVersionId(scriptVersion.getId());
        response.setVersionNo(scriptVersion.getVersionNo());
        response.setTitle(scriptVersion.getTitle());
        response.setSourceType(scriptVersion.getSourceType());
        response.setValidationStatus(yamlSnapshot != null ? yamlSnapshot.getValidationStatus() : null);
        response.setLatest(latest);
        response.setCreatedAt(scriptVersion.getCreatedAt());
        return response;
    }
}
