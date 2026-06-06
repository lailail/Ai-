package com.qiniuyun.novelscript.service;

import com.qiniuyun.novelscript.controller.response.AdaptationScriptResponse;

/**
 * 小说改编主链路编排服务。
 */
public interface AdaptationPipelineService {

    /**
     * 触发指定项目的最小改编链路，并返回最新生成结果。
     *
     * @param projectId 项目 ID
     * @return 生成后的剧本响应
     */
    AdaptationScriptResponse generateScript(Long projectId);

    /**
     * 查询指定项目最新的剧本版本。
     *
     * @param projectId 项目 ID
     * @return 最新剧本响应
     */
    AdaptationScriptResponse getLatestScript(Long projectId);
}
