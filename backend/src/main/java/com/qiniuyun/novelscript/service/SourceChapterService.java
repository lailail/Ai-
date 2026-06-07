package com.qiniuyun.novelscript.service;

import com.qiniuyun.novelscript.controller.request.SourceChapterCreateRequest;
import com.qiniuyun.novelscript.controller.request.SourceChapterUpdateRequest;
import com.qiniuyun.novelscript.controller.response.SourceChapterResponse;
import java.util.List;

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
     * 更新指定项目下某一章的标题和正文。
     *
     * @param projectId 项目 ID
     * @param chapterId 章节 ID
     * @param request 章节更新请求
     * @return 更新后的章节响应
     */
    SourceChapterResponse updateChapter(Long projectId, Long chapterId, SourceChapterUpdateRequest request);

    /**
     * 查询指定项目下已经保存的章节列表。
     *
     * @param projectId 项目 ID
     * @return 章节列表响应
     */
    List<SourceChapterResponse> listChapters(Long projectId);

    /**
     * 判断项目是否达到最小章节数量要求。
     *
     * @param projectId 项目 ID
     * @param minimumCount 最小章节数
     * @return 是否已达到最小要求
     */
    boolean hasMinimumChapterCount(Long projectId, int minimumCount);
}
