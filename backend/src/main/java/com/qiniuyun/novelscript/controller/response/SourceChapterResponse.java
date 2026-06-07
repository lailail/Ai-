package com.qiniuyun.novelscript.controller.response;

import com.qiniuyun.novelscript.domain.entity.SourceChapter;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 小说章节对外响应。
 */
@Getter
@AllArgsConstructor
public class SourceChapterResponse {

    /** 章节 ID。 */
    private final Long id;

    /** 所属项目 ID。 */
    private final Long projectId;

    /** 章节序号。 */
    private final Integer chapterNo;

    /** 章节标题。 */
    private final String title;

    /** 章节正文。 */
    private final String content;

    /** 章节字数。 */
    private final Integer wordCount;

    /** 创建时间。 */
    private final LocalDateTime createdAt;

    /** 最后更新时间。 */
    private final LocalDateTime updatedAt;

    /**
     * 将章节实体转换为对外响应。
     *
     * @param chapter 章节实体
     * @return 章节响应
     */
    public static SourceChapterResponse from(SourceChapter chapter) {
        return new SourceChapterResponse(
            chapter.getId(),
            chapter.getProjectId(),
            chapter.getChapterNo(),
            chapter.getTitle(),
            chapter.getContent(),
            chapter.getWordCount(),
            chapter.getCreatedAt(),
            chapter.getUpdatedAt()
        );
    }
}
