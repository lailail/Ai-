package com.qiniuyun.novelscript.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 改编任务实体。
 */
@Getter
@Setter
@TableName("adaptation_job")
public class AdaptationJob extends BaseEntity {

    /** 所属改编项目 ID。 */
    @TableField("project_id")
    private Long projectId;

    /** 任务整体状态。 */
    private String status;

    /** 当前执行到的流水线阶段。 */
    @TableField("current_stage")
    private String currentStage;

    /** 发生错误的阶段名称。 */
    @TableField("error_stage")
    private String errorStage;

    /** 错误摘要，便于排查和重试。 */
    @TableField("error_message")
    private String errorMessage;

    /** 任务开始时间。 */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /** 任务结束时间。 */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

}
