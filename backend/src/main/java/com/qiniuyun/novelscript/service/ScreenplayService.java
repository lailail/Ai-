package com.qiniuyun.novelscript.service;

import com.qiniuyun.novelscript.controller.response.AdaptationScriptResponse;
import com.qiniuyun.novelscript.controller.response.ScreenplayResponse;

/**
 * 负责正式剧本查询、渲染、导出和回写 YAML 的服务接口。
 */
public interface ScreenplayService {

    /**
     * 根据指定 YAML 版本重新渲染正式剧本，并保存为快照。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return 渲染后的正式剧本响应
     */
    ScreenplayResponse renderScreenplay(Long projectId, Long scriptVersionId);

    /**
     * 查询当前项目最新版本对应的正式剧本。
     *
     * @param projectId 项目 ID
     * @return 最新正式剧本响应
     */
    ScreenplayResponse getLatestScreenplay(Long projectId);

    /**
     * 查询指定剧本版本对应的正式剧本。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return 指定版本正式剧本响应
     */
    ScreenplayResponse getScreenplay(Long projectId, Long scriptVersionId);

    /**
     * 将正式剧本编辑结果同步回 YAML，并生成新的剧本版本。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 原始剧本版本 ID
     * @param title 新版本标题
     * @param markdownContent 编辑后的正式剧本 Markdown
     * @return 新生成的 YAML 版本响应
     */
    AdaptationScriptResponse syncScreenplayToYaml(Long projectId, Long scriptVersionId, String title, String markdownContent);

    /**
     * 保存正式剧本编辑结果，本质上等同于同步回 YAML 并落新版本。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 原始剧本版本 ID
     * @param title 新版本标题
     * @param markdownContent 编辑后的正式剧本 Markdown
     * @return 新生成的 YAML 版本响应
     */
    AdaptationScriptResponse saveScreenplay(Long projectId, Long scriptVersionId, String title, String markdownContent);

    /**
     * 导出指定剧本版本的 Markdown 正式剧本。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return Markdown 文本
     */
    String exportMarkdown(Long projectId, Long scriptVersionId);

    /**
     * 导出指定剧本版本的纯文本正式剧本。
     *
     * @param projectId 项目 ID
     * @param scriptVersionId 剧本版本 ID
     * @return 纯文本内容
     */
    String exportPlainText(Long projectId, Long scriptVersionId);
}
