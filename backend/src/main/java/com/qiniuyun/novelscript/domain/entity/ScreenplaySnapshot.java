package com.qiniuyun.novelscript.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 正式剧本快照实体。
 */
@Getter
@Setter
@TableName("screenplay_snapshot")
public class ScreenplaySnapshot extends BaseEntity {

    /** 所属改编项目 ID。 */
    @TableField("project_id")
    private Long projectId;

    /** 对应的剧本版本 ID。 */
    @TableField("script_version_id")
    private Long scriptVersionId;

    /** 当前正式剧本标题。 */
    private String title;

    /** 正式剧本 Markdown 正文。 */
    @TableField("markdown_content")
    private String markdownContent;

    /** 正式剧本渲染规则版本。 */
    @TableField("render_version")
    private String renderVersion;

    /** 当前快照所对应的版本来源。 */
    @TableField("source_type")
    private String sourceType;
}
