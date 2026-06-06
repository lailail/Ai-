package com.qiniuyun.novelscript.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * YAML 快照实体。
 */
@Getter
@Setter
@TableName("yaml_snapshot")
public class YamlSnapshot extends BaseEntity {

    /** 所属改编项目 ID。 */
    @TableField("project_id")
    private Long projectId;

    /** 对应的剧本版本 ID。 */
    @TableField("script_version_id")
    private Long scriptVersionId;

    /** 当前 YAML 使用的 Schema 版本。 */
    @TableField("schema_version")
    private String schemaVersion;

    /** YAML 原文内容。 */
    @TableField("yaml_content")
    private String yamlContent;

    /** Schema 校验状态。 */
    @TableField("validation_status")
    private String validationStatus;

    /** Schema 校验失败时的错误信息。 */
    @TableField("validation_errors")
    private String validationErrors;

}
