package com.qiniuyun.novelscript.controller.response;

import com.qiniuyun.novelscript.domain.entity.Project;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 改编项目对外响应。
 */
@Getter
@AllArgsConstructor
public class ProjectResponse {

    /** 项目 ID。 */
    private final Long id;

    /** 项目标题。 */
    private final String title;

    /** 项目简介。 */
    private final String description;

    /** 项目状态。 */
    private final String status;

    /** 已保存章节数量。 */
    private final Integer chapterCount;

    /** 创建时间。 */
    private final LocalDateTime createdAt;

    /** 最后更新时间。 */
    private final LocalDateTime updatedAt;

    /**
     * 将项目实体转换为对外响应。
     *
     * @param project 项目实体
     * @param chapterCount 章节数量
     * @return 项目响应
     */
    public static ProjectResponse from(Project project, Integer chapterCount) {
        return new ProjectResponse(
            project.getId(),
            project.getTitle(),
            project.getDescription(),
            project.getStatus(),
            chapterCount,
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}
