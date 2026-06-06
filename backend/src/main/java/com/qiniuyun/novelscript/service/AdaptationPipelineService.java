package com.qiniuyun.novelscript.service;

import com.qiniuyun.novelscript.controller.response.AdaptationJobResponse;
import com.qiniuyun.novelscript.controller.response.AdaptationScriptResponse;

/**
 * 小说改编主链路编排服务。
 */
public interface AdaptationPipelineService {

    /**
     * 启动指定项目的改编任务。
     *
     * @param projectId 项目 ID
     * @return 任务启动后的进度响应
     */
    AdaptationJobResponse startAdaptation(Long projectId);

    /**
     * 查询指定项目当前最新的改编任务。
     *
     * @param projectId 项目 ID
     * @return 最新任务进度响应
     */
    AdaptationJobResponse getLatestJob(Long projectId);

    /**
     * 查询指定项目当前最新的剧本版本。
     *
     * @param projectId 项目 ID
     * @return 最新剧本响应
     */
    AdaptationScriptResponse getLatestScript(Long projectId);
}
