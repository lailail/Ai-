package com.qiniuyun.novelscript.service;

import com.qiniuyun.novelscript.controller.response.AdaptationScriptResponse;
import com.qiniuyun.novelscript.controller.response.ScriptValidationResponse;
import com.qiniuyun.novelscript.controller.response.ScriptVersionSummaryResponse;
import java.util.List;

/**
 * 负责剧本 YAML 工作区相关版本操作的服务接口。
 */
public interface ScriptVersionService {

    /**
     * 查询当前项目下的剧本版本列表。
     *
     * @param projectId 项目 ID
     * @return 按版本号倒序排列的版本列表
     */
    List<ScriptVersionSummaryResponse> listScriptVersions(Long projectId);

    /**
     * 查询当前项目下指定剧本版本的 YAML 详情。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return 指定版本的 YAML 详情
     */
    AdaptationScriptResponse getScriptVersion(Long projectId, Long scriptVersionId);

    /**
     * 对当前 YAML 原文执行 Schema 校验。
     *
     * @param projectId 项目 ID
     * @param yamlContent 待校验的 YAML 原文
     * @return 结构化校验结果
     */
    ScriptValidationResponse validateScript(Long projectId, String yamlContent);

    /**
     * 将当前 YAML 保存为新的剧本版本。
     *
     * @param projectId 项目 ID
     * @param title 新版本标题
     * @param yamlContent 待保存的 YAML 原文
     * @return 保存后的新版本详情
     */
    AdaptationScriptResponse saveScriptVersion(Long projectId, String title, String yamlContent);

    /**
     * 将当前 YAML 保存为指定来源类型的新剧本版本。
     *
     * @param projectId 项目 ID
     * @param title 新版本标题
     * @param yamlContent 待保存的 YAML 原文
     * @param sourceType 新版本来源类型
     * @return 保存后的新版本详情
     */
    AdaptationScriptResponse saveScriptVersion(Long projectId, String title, String yamlContent, String sourceType);
}
