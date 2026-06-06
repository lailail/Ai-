package com.qiniuyun.novelscript.controller.response;

import com.qiniuyun.novelscript.domain.entity.AdaptationJob;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 改编任务进度查询响应对象。
 */
@Data
public class AdaptationJobResponse {

    private static final List<String> ORDERED_STAGES = List.of(
        "CHAPTER_NORMALIZE",
        "CHAPTER_CONTEXT_EXTRACT",
        "GLOBAL_CONTEXT_MERGE",
        "STORY_BIBLE_BUILD",
        "SCRIPT_OUTLINE_PLAN",
        "SCENE_GENERATE",
        "YAML_SERIALIZE",
        "SCHEMA_VALIDATE",
        "VERSION_SAVE"
    );

    /**
     * 项目 ID。
     */
    private Long projectId;

    /**
     * 改编任务 ID。
     */
    private Long jobId;

    /**
     * 任务状态。
     */
    private String status;

    /**
     * 当前阶段编码。
     */
    private String currentStage;

    /**
     * 当前阶段中文名称。
     */
    private String currentStageLabel;

    /**
     * 当前阶段序号，从 0 开始，完成态为 9。
     */
    private Integer currentStageIndex;

    /**
     * 总阶段数。
     */
    private Integer stageCount;

    /**
     * 进度百分比。
     */
    private Integer progressPercent;

    /**
     * 失败阶段。
     */
    private String errorStage;

    /**
     * 失败摘要。
     */
    private String errorMessage;

    /**
     * 任务开始时间。
     */
    private LocalDateTime startedAt;

    /**
     * 任务结束时间。
     */
    private LocalDateTime finishedAt;

    /**
     * 根据任务实体构建任务进度响应。
     *
     * @param job 改编任务实体
     * @return 任务进度响应
     */
    public static AdaptationJobResponse from(AdaptationJob job) {
        String stage = resolveDisplayStage(job);
        int stageIndex = resolveStageIndex(stage);

        AdaptationJobResponse response = new AdaptationJobResponse();
        response.setProjectId(job.getProjectId());
        response.setJobId(job.getId());
        response.setStatus(job.getStatus());
        response.setCurrentStage(stage);
        response.setCurrentStageLabel(resolveStageLabel(stage));
        response.setCurrentStageIndex(stageIndex);
        response.setStageCount(ORDERED_STAGES.size());
        response.setProgressPercent(resolveProgressPercent(stageIndex));
        response.setErrorStage(job.getErrorStage());
        response.setErrorMessage(job.getErrorMessage());
        response.setStartedAt(job.getStartedAt());
        response.setFinishedAt(job.getFinishedAt());
        return response;
    }

    /**
     * 解析前端展示用的任务阶段。
     *
     * @param job 改编任务实体
     * @return 阶段编码
     */
    private static String resolveDisplayStage(AdaptationJob job) {
        if ("SUCCEEDED".equals(job.getStatus())) {
            return "COMPLETED";
        }
        if ("FAILED".equals(job.getStatus()) && job.getErrorStage() != null) {
            return job.getErrorStage();
        }
        return job.getCurrentStage();
    }

    /**
     * 根据阶段编码解析序号。
     *
     * @param stage 阶段编码
     * @return 阶段序号
     */
    private static int resolveStageIndex(String stage) {
        if ("COMPLETED".equals(stage)) {
            return ORDERED_STAGES.size();
        }
        int index = ORDERED_STAGES.indexOf(stage);
        return index < 0 ? 0 : index + 1;
    }

    /**
     * 根据阶段序号换算百分比。
     *
     * @param stageIndex 阶段序号
     * @return 百分比
     */
    private static int resolveProgressPercent(int stageIndex) {
        if (stageIndex <= 0) {
            return 0;
        }
        if (stageIndex >= ORDERED_STAGES.size()) {
            return 100;
        }
        return Math.round(stageIndex * 100.0F / ORDERED_STAGES.size());
    }

    /**
     * 将阶段编码转换为中文名称。
     *
     * @param stage 阶段编码
     * @return 中文名称
     */
    private static String resolveStageLabel(String stage) {
        return switch (stage) {
            case "CHAPTER_NORMALIZE" -> "章节标准化";
            case "CHAPTER_CONTEXT_EXTRACT" -> "单章上下文提取";
            case "GLOBAL_CONTEXT_MERGE" -> "全局上下文合并";
            case "STORY_BIBLE_BUILD" -> "Story Bible 构建";
            case "SCRIPT_OUTLINE_PLAN" -> "剧本大纲规划";
            case "SCENE_GENERATE" -> "场景生成";
            case "YAML_SERIALIZE" -> "YAML 序列化";
            case "SCHEMA_VALIDATE" -> "Schema 校验";
            case "VERSION_SAVE" -> "版本保存";
            case "COMPLETED" -> "已完成";
            default -> "任务已创建";
        };
    }
}
