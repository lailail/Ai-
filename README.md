# AI 小说转剧本工具

这是一个面向小说作者的 AI 辅助改编工具。它接收 3 章及以上的小说文本，先分阶段提取上下文并构建 `Story Bible`，再生成符合项目 Schema 的剧本 YAML 初稿，最后把 YAML 渲染成可直接阅读和继续编辑的正式剧本页面，帮助作者更快进入影视和短剧改编流程。

## 核心功能

- 按项目持续录入多章节小说内容
- 通过分阶段流水线生成结构化剧本 YAML 初稿
- 构建并保存项目级 `Story Bible`
- 支持 YAML 版本切换、后端 Schema 校验和另存新版本
- 支持从 YAML 渲染正式剧本
- 支持编辑正式剧本后回写 YAML，并生成新的剧本版本
- 支持导出 YAML、正式剧本 Markdown、正式剧本 TXT

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

后端启动时会自动加载：

```text
backend/src/main/resources/db/schema.sql
```

当前核心表：

- `project`
- `source_chapter`
- `chapter_context`
- `story_bible`
- `adaptation_job`
- `script_version`
- `yaml_snapshot`
- `screenplay_snapshot`

## 目录结构

```text
backend/
frontend/
scripts/
samples/
```

- `backend/`：Spring Boot 后端工程
- `frontend/`：React 前端工程
- `scripts/`：本地启动、导入样例、验证等辅助脚本
- `samples/`：样例小说和样例剧本输出

样例目录约定：

```text
samples/novels/
samples/scripts/
```

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

项目工作台：

- `章节管理`：录入和查看小说章节
- `改编进度`：启动改编任务，查看分阶段进度条和任务状态
- `Story Bible`：查看人物、关系、地点、时间线、冲突和伏笔
- `YAML 初稿`：编辑结构化剧本、查看字段说明、执行校验、导出 YAML、跳转正式剧本页

正式剧本页：

- 左侧：剧本版本列表
- 中间：正式剧本 Markdown 编辑器
- 右侧：重新渲染、保存并回写 YAML、导出 Markdown/TXT、返回 YAML 页面

## 主要接口

基础接口：

- `GET /api/health`
- `POST /api/projects`
- `GET /api/projects`
- `GET /api/projects/{projectId}`
- `POST /api/projects/{projectId}/chapters`
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

## YAML Schema 文档

YAML 字段说明和设计原因见：

- [SCRIPT_YAML_SCHEMA.md](SCRIPT_YAML_SCHEMA.md)
- [docs/spec/yaml-schema.md](docs/spec/yaml-schema.md)
