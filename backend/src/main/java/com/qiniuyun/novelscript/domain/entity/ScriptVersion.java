package com.qiniuyun.novelscript.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 剧本版本实体。
 */
@Getter
@Setter
@TableName("script_version")
public class ScriptVersion extends BaseEntity {

    /** 所属改编项目 ID。 */
    @TableField("project_id")
    private Long projectId;

    /** 剧本版本号，便于保留历史记录。 */
    @TableField("version_no")
    private Integer versionNo;

    /** 版本来源，例如 AI 生成或人工编辑。 */
    @TableField("source_type")
    private String sourceType;

    /** 当前版本标题。 */
    private String title;

}
