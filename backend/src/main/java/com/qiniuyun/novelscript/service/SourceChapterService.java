package com.qiniuyun.novelscript.service;

import com.qiniuyun.novelscript.controller.request.SourceChapterCreateRequest;
import com.qiniuyun.novelscript.controller.response.SourceChapterResponse;

/**
 * 小说章节的业务服务接口。
 */
public interface SourceChapterService {

    /**
     * 在指定项目下保存一章小说内容。
     *
     * @param projectId 项目 ID
     * @param request 章节保存请求
     * @return 保存后的章节响应
     */
    SourceChapterResponse createChapter(Long projectId, SourceChapterCreateRequest request);

    /**
     * 判断项目是否达到最小章节数量要求。
     *
     * @param projectId 项目 ID
     * @param minimumCount 最小章节数
     * @return 是否已达到最小要求
     */
    boolean hasMinimumChapterCount(Long projectId, int minimumCount);
}
