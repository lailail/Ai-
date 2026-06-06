# AI 小说转剧本工具

本项目用于将 3 章及以上的小说文本转换为结构化剧本 YAML，帮助作者更快得到可编辑、可继续打磨的剧本初稿。

系统不是一次性把整篇小说直接交给模型生成最终剧本，而是先提取章节上下文，构建项目级 `Story Bible`，再分阶段生成剧本大纲、场景和 YAML。这样可以减少多章节改编中常见的人物漂移、剧情断裂、伏笔丢失和时间线混乱。

## 核心能力

- 支持按项目持续录入多章节小说内容
- 每个项目独立维护章节、上下文、Story Bible、剧本版本和 YAML 快照
- 使用分阶段流水线完成小说到剧本的结构化改编
- 生成符合项目 Schema 的 YAML 初稿
- 后端执行 YAML Schema 校验并保存结果
- 前端提供项目工作台，支持章节管理、改编进度、Story Bible 查看和 YAML 初稿预览

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

大模型：

- DeepSeek
- 通过 Spring AI 统一接入

## 模型配置

当前通过环境变量提供 DeepSeek API Key：

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

设置完成后需要重启终端或 VS Code，再启动项目。

## 本地数据库

当前使用本地 MySQL，默认数据库名：

```text
novel_script
```

本地开发连接写在：

```text
backend/src/main/resources/application.yml
```

默认本地连接：

- 用户名：`root`
- 密码：`123456`

后端启动时会自动加载 SQL 初始化文件：

```text
backend/src/main/resources/db/schema.sql
```

## 本地启动

后端启动：

```powershell
cd backend
mvn spring-boot:run
```

前端启动：

```powershell
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，并通过 Vite 代理将 `/api` 请求转发到 `http://localhost:8080`。

## 当前工作台说明

当前前端工作区为单页 Tabs 结构，包含：

- `章节管理`
- `改编进度`
- `Story Bible`
- `YAML 初稿`

其中改编进度页会展示任务阶段、进度百分比和当前阶段说明；当任务完成后，可直接切换到 `Story Bible` 和 `YAML 初稿` 查看结果。

## 最小接口

当前后端已提供以下核心接口：

- `POST /api/projects`
- `GET /api/projects`
- `GET /api/projects/{projectId}`
- `POST /api/projects/{projectId}/chapters`
- `GET /api/projects/{projectId}/chapters`
- `POST /api/projects/{projectId}/adaptations`
- `GET /api/projects/{projectId}/adaptations/latest-job`
- `GET /api/projects/{projectId}/scripts/latest`
- `GET /api/projects/{projectId}/story-bible/latest`

接口说明：

- `POST /api/projects/{projectId}/adaptations` 用于启动一次改编任务，立即返回任务信息
- `GET /api/projects/{projectId}/adaptations/latest-job` 用于查询当前项目最新改编任务的阶段和进度
- `GET /api/projects/{projectId}/scripts/latest` 用于查询当前项目最新剧本 YAML 初稿
- `GET /api/projects/{projectId}/story-bible/latest` 用于查询当前项目最新 `Story Bible` 快照

## YAML Schema 文档

剧本 YAML 的字段说明和设计原因见：

- [SCRIPT_YAML_SCHEMA.md](SCRIPT_YAML_SCHEMA.md)

## 设计说明

当前采用分阶段流水线：

```text
章节清洗
 -> 章节上下文提取
 -> 全局上下文合并
 -> Story Bible 构建
 -> 剧本大纲规划
 -> 分场剧本生成
 -> YAML 序列化
 -> Schema 校验
 -> 版本保存
```

当前前端通过“启动任务 + 轮询任务进度”的方式驱动改编流程，后端使用 `adaptation_job` 记录每个阶段的状态，便于用户在改编较慢时仍能看到真实进度反馈。
