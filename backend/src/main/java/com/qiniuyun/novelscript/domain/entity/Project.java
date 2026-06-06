package com.qiniuyun.novelscript.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 小说改编项目实体。
 */
@Getter
@Setter
@TableName("project")
public class Project extends BaseEntity {

    /** 项目标题，对应一本待改编小说。 */
    private String title;

    /** 项目简介，用于补充小说背景或改编目标。 */
    private String description;

    /** 项目当前状态，例如草稿、处理中或已完成。 */
    @TableField("status")
    private String status;

}
