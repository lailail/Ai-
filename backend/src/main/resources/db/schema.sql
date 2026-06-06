CREATE TABLE IF NOT EXISTS project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目主键 ID',
    title VARCHAR(255) NOT NULL COMMENT '项目标题，对应一本待改编小说',
    description TEXT COMMENT '项目简介，用于补充小说背景或改编目标',
    status VARCHAR(64) COMMENT '项目当前状态',
    created_at DATETIME COMMENT '记录创建时间',
    updated_at DATETIME COMMENT '记录最后更新时间'
) COMMENT='小说改编项目表';

CREATE TABLE IF NOT EXISTS source_chapter (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '章节主键 ID',
    project_id BIGINT NOT NULL COMMENT '所属改编项目 ID',
    chapter_no INT NOT NULL COMMENT '章节序号，同一项目内唯一',
    title VARCHAR(255) COMMENT '章节标题',
    content TEXT NOT NULL COMMENT '章节原文内容',
    word_count INT COMMENT '章节字数',
    created_at DATETIME COMMENT '记录创建时间',
    updated_at DATETIME COMMENT '记录最后更新时间',
    CONSTRAINT uk_source_chapter_project_no UNIQUE (project_id, chapter_no)
) COMMENT='小说原始章节表';

CREATE TABLE IF NOT EXISTS adaptation_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务主键 ID',
    project_id BIGINT NOT NULL COMMENT '所属改编项目 ID',
    status VARCHAR(64) NOT NULL COMMENT '任务整体状态',
    current_stage VARCHAR(128) COMMENT '当前执行到的流水线阶段',
    error_stage VARCHAR(128) COMMENT '发生错误的阶段名称',
    error_message TEXT COMMENT '错误摘要，便于排查和重试',
    started_at DATETIME COMMENT '任务开始时间',
    finished_at DATETIME COMMENT '任务结束时间',
    created_at DATETIME COMMENT '记录创建时间',
    updated_at DATETIME COMMENT '记录最后更新时间'
) COMMENT='改编任务表';

CREATE TABLE IF NOT EXISTS script_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '剧本版本主键 ID',
    project_id BIGINT NOT NULL COMMENT '所属改编项目 ID',
    version_no INT NOT NULL COMMENT '剧本版本号',
    source_type VARCHAR(64) NOT NULL COMMENT '版本来源类型',
    title VARCHAR(255) COMMENT '当前版本标题',
    created_at DATETIME COMMENT '记录创建时间',
    updated_at DATETIME COMMENT '记录最后更新时间'
) COMMENT='剧本版本表';

CREATE TABLE IF NOT EXISTS yaml_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'YAML 快照主键 ID',
    project_id BIGINT NOT NULL COMMENT '所属改编项目 ID',
    script_version_id BIGINT NOT NULL COMMENT '对应的剧本版本 ID',
    schema_version VARCHAR(32) NOT NULL COMMENT '当前 YAML 使用的 Schema 版本',
    yaml_content TEXT NOT NULL COMMENT 'YAML 原文内容',
    validation_status VARCHAR(64) COMMENT 'Schema 校验状态',
    validation_errors TEXT COMMENT 'Schema 校验失败时的错误信息',
    created_at DATETIME COMMENT '记录创建时间',
    updated_at DATETIME COMMENT '记录最后更新时间'
) COMMENT='剧本 YAML 快照表';
