package com.qiniuyun.novelscript.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 小说原始章节实体。
 */
@Getter
@Setter
@TableName("source_chapter")
public class SourceChapter extends BaseEntity {

    /** 所属改编项目 ID。 */
    @TableField("project_id")
    private Long projectId;

    /** 章节序号，同一项目内唯一。 */
    @TableField("chapter_no")
    private Integer chapterNo;

    /** 章节标题。 */
    private String title;

    /** 章节原文内容。 */
    private String content;

    /** 章节字数，便于后续统计和分段控制。 */
    @TableField("word_count")
    private Integer wordCount;

}
