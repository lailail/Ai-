# AI 小说转剧本工具

这是一个面向小说作者的 AI 辅助改编工具。它接收 3 章及以上的小说文本，先分阶段提取上下文并构建 `Story Bible`，再生成符合项目 Schema 的剧本 YAML 初稿，最后把 YAML 渲染成可直接阅读和继续编辑的正式剧本页面，帮助作者更快进入影视和短剧改编流程。

## 核心功能

- 按项目持续录入多章节小说内容
- 首页通过项目下拉框快速进入已有工作台
- 章节管理支持录入、查看和编辑章节正文
- 通过分阶段流水线生成结构化剧本 YAML 初稿
- 构建并保存项目级 `Story Bible`
- 支持 YAML 版本切换、后端 Schema 校验和另存新版本
- 支持从 YAML 渲染正式剧本
- 支持编辑正式剧本后回写 YAML，并生成新的剧本版本
- 支持导出 YAML、正式剧本 Markdown、正式剧本 TXT
- YAML 与正式剧本工作区都支持通过版本下拉框切换历史版本

## 视频链接

https://www.bilibili.com/video/BV1MqEb6jEbZ/?spm_id_from=333.1387.homepage.video_card.click

## 技术栈

后端：

- Java 17
- Spring Boot
- Spring AI
- MyBatis-Plus
- MySQL
- Jackson YAML
- Bean Validation

前端：

- React
- TypeScript
- Vite
- Ant Design
- TanStack Query
- Monaco Editor

说明：

- `Monaco Editor` 当前同时用于 `YAML 初稿` 编辑区和 `正式剧本` 的 Markdown 编辑区。

模型：

- DeepSeek
- 通过 Spring AI 统一接入

## 模型配置

项目通过环境变量读取 DeepSeek API Key：

```text
DEEPSEEK_API_KEY
```

默认模型参数：

- Base URL：`https://api.deepseek.com`
- Model：`deepseek-chat`
- Temperature：`0.7`

Windows 下设置环境变量示例：

```powershell
setx DEEPSEEK_API_KEY "你的 DeepSeek API Key"
```

设置后请重新打开终端或 VS Code，再启动项目。

## 本地数据库配置

当前默认使用本地 MySQL，配置写在：

```text
backend/src/main/resources/application.yml
```

默认连接：

- 数据库名：`novel_script`
- 用户名：`root`
- 密码：`123456`

后端启动时默认自动加载：

```text
backend/src/main/resources/db/schema.sql
```

说明：

- `schema.sql` 会在本地启动时自动建库建表。
- `data.sql` 当前作为演示数据预留文件保留在同目录下，但默认不自动加载。

当前核心表：

- `project`
- `source_chapter`
- `chapter_context`
- `story_bible`
- `adaptation_job`
- `script_version`
- `yaml_snapshot`
- `screenplay_snapshot`


## 本地启动

后端：

```powershell
cd backend
mvn spring-boot:run
```

前端首次启动：

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址是 `http://localhost:5173`，并通过 Vite 代理把 `/api` 请求转发到 `http://localhost:8080`。

## 页面说明

首页：

- `新建项目`：创建新的小说改编项目
- `继续已有项目`：通过下拉框选择已有项目并进入工作台

项目工作台：

- `章节管理`：录入、查看和编辑小说章节
- `改编进度`：启动改编任务，查看分阶段进度条和任务状态
- `Story Bible`：查看人物、关系、地点、时间线、冲突和伏笔
- `YAML 初稿`：编辑结构化剧本、查看字段说明、执行校验、导出 YAML
- `正式剧本`：查看渲染后的可读剧本、直接编辑、保存回写 YAML、导出 Markdown/TXT

YAML 工作区布局：

- 左侧：版本下拉框和当前版本摘要
- 中间：YAML Monaco 编辑区
- 右侧：Schema 校验结果

字段说明：

- `YAML 初稿` 页的“字段说明”抽屉当前展示完整 Schema 字段路径说明，而不是只展示关键字段。

正式剧本工作区布局：

- 左侧：剧本版本下拉框和当前版本摘要
- 中间：正式剧本 Monaco Markdown 编辑区
- 右侧：重新渲染、保存并回写 YAML、导出 Markdown/TXT

## 主要接口

基础接口：

- `GET /api/health`
- `POST /api/projects`
- `GET /api/projects`
- `GET /api/projects/{projectId}`
- `POST /api/projects/{projectId}/chapters`
- `PUT /api/projects/{projectId}/chapters/{chapterId}`
- `GET /api/projects/{projectId}/chapters`

改编流水线接口：

- `POST /api/projects/{projectId}/adaptations`
- `GET /api/projects/{projectId}/adaptations/latest-job`
- `GET /api/projects/{projectId}/story-bible/latest`
- `GET /api/projects/{projectId}/scripts/latest`

YAML 工作区接口：

- `GET /api/projects/{projectId}/scripts`
- `GET /api/projects/{projectId}/scripts/{scriptVersionId}`
- `POST /api/projects/{projectId}/scripts/validate`
- `POST /api/projects/{projectId}/scripts`

正式剧本接口：

- `GET /api/projects/{projectId}/screenplays/latest`
- `GET /api/projects/{projectId}/screenplays/{scriptVersionId}`
- `POST /api/projects/{projectId}/screenplays/render`
- `POST /api/projects/{projectId}/screenplays/sync-yaml`
- `POST /api/projects/{projectId}/screenplays/save`
- `GET /api/projects/{projectId}/screenplays/{scriptVersionId}/export?format=md`
- `GET /api/projects/{projectId}/screenplays/{scriptVersionId}/export?format=txt`

## 设计说明

主链路固定为：

```text
NovelInput
 -> ChapterNormalize
 -> ChapterContextExtract
 -> GlobalContextMerge
 -> StoryBibleBuild
 -> ScriptOutlinePlan
 -> SceneGenerate
 -> YamlSerialize
 -> SchemaValidate
 -> VersionSave
```

正式剧本不是第二份权威数据源。项目里真正的结构化底稿始终是 `yaml_snapshot`；正式剧本是由 YAML 规则化渲染出来的可读视图。作者在正式剧本页的编辑结果，会通过后端受控解析回写到 YAML，再生成新的 `script_version`、`yaml_snapshot` 和 `screenplay_snapshot`。

AI 流水线对大模型返回结果采用“后端兼容解析为主，Prompt 收紧为辅”的策略。也就是说，当模型把字符串列表返回成单字符串、把简单文本包成对象、或把单对象替代数组时，后端会先做标准化处理，再进入正式反序列化与校验流程，尽量减少因为字段形态漂移导致的整条链路失败。

## YAML Schema 文档

YAML 字段说明和设计原因见：

- [SCRIPT_YAML_SCHEMA.md](SCRIPT_YAML_SCHEMA.md)

