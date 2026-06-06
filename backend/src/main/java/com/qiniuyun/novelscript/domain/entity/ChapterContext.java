package com.qiniuyun.novelscript.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 单章上下文快照实体。
 */
@Getter
@Setter
@TableName("chapter_context")
public class ChapterContext extends BaseEntity {

    /**
     * 所属改编项目 ID。
     */
    @TableField("project_id")
    private Long projectId;

    /**
     * 对应的原始章节 ID。
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 单章上下文 JSON 内容。
     */
    @TableField("context_json")
    private String contextJson;

    /**
     * 本次生成使用的模型名称。
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 当前快照状态。
     */
    @TableField("status")
    private String status;
}
