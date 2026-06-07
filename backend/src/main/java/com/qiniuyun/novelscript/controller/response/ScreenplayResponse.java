package com.qiniuyun.novelscript.controller.response;

import com.qiniuyun.novelscript.domain.entity.ScreenplaySnapshot;
import com.qiniuyun.novelscript.domain.entity.ScriptVersion;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 正式剧本查询与渲染响应对象。
 */
@Getter
@Setter
public class ScreenplayResponse {

    /** 项目 ID。 */
    private Long projectId;

    /** 剧本版本 ID。 */
    private Long scriptVersionId;

    /** 剧本版本号。 */
    private Integer versionNo;

    /** 剧本标题。 */
    private String title;

    /** 版本来源类型。 */
    private String sourceType;

    /** 正式剧本渲染规则版本。 */
    private String renderVersion;

    /** 正式剧本 Markdown 内容。 */
    private String markdownContent;

    /** 快照创建时间。 */
    private LocalDateTime createdAt;

    /** 快照更新时间。 */
    private LocalDateTime updatedAt;

    /**
     * 根据剧本版本与正式剧本快照构建响应对象。
     *
     * @param projectId 项目 ID
     * @param scriptVersion 剧本版本实体
     * @param screenplaySnapshot 正式剧本快照实体
     * @return 正式剧本响应
     */
    public static ScreenplayResponse from(
        Long projectId,
        ScriptVersion scriptVersion,
        ScreenplaySnapshot screenplaySnapshot
    ) {
        ScreenplayResponse response = new ScreenplayResponse();
        response.setProjectId(projectId);
        response.setScriptVersionId(scriptVersion.getId());
        response.setVersionNo(scriptVersion.getVersionNo());
        response.setTitle(screenplaySnapshot.getTitle());
        response.setSourceType(scriptVersion.getSourceType());
        response.setRenderVersion(screenplaySnapshot.getRenderVersion());
        response.setMarkdownContent(screenplaySnapshot.getMarkdownContent());
        response.setCreatedAt(screenplaySnapshot.getCreatedAt());
        response.setUpdatedAt(screenplaySnapshot.getUpdatedAt());
        return response;
    }
}
