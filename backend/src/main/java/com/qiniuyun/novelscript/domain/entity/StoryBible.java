package com.qiniuyun.novelscript.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 项目级 Story Bible 快照实体。
 */
@Getter
@Setter
@TableName("story_bible")
public class StoryBible extends BaseEntity {

    /**
     * 所属改编项目 ID。
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * Story Bible 的 JSON 内容。
     */
    @TableField("bible_json")
    private String bibleJson;

    /**
     * Story Bible 版本号。
     */
    @TableField("version_no")
    private Integer versionNo;

    /**
     * 参与本次构建的章节上下文快照 ID 列表。
     */
    @TableField("source_context_ids")
    private String sourceContextIds;
}
