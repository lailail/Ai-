# AI 小说转剧本工具

本项目用于将 3 个章节以上的小说文本转换为结构化剧本 YAML，帮助作者更快获得可编辑、可继续打磨的剧本初稿。

系统的核心思路不是一次性把整篇小说直接交给模型生成最终剧本，而是先抽取章节上下文，构建项目级 `Story Bible`，再分阶段生成剧本大纲、场景和 YAML。这样可以减少多章节改编中常见的人物漂移、剧情断裂、伏笔丢失和时间线混乱。

## 核心功能

- 多章节小说录入，支持同一作品持续追加章节
- 支持纯文本、`.txt` 和 `.md` 章节内容
- 自动提取人物、关系、地点、事件、冲突、伏笔和时间线
- 为每本小说构建独立的 `Story Bible`
- 分阶段生成剧本大纲、场景内容和 YAML 初稿
- 后端执行 YAML Schema 校验
- 保存 `ScriptVersion` 与 `YamlSnapshot`，保留版本历史
- 按 `Project` 隔离多本小说的上下文、版本和生成结果

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
- Ant Design X
- Monaco Editor
- TanStack Query

大模型：

- DeepSeek
- 通过 Spring AI 统一接入

## 模型配置

当前默认通过环境变量提供 DeepSeek API Key：

```text
DEEPSEEK_API_KEY
```

默认模型相关参数：

- Base URL：`https://api.deepseek.com`
- Model：`deepseek-chat`
- Temperature：`0.7`

Windows 下设置环境变量示例：

```powershell
setx DEEPSEEK_API_KEY "你的 DeepSeek API Key"
```

设置完成后需要重启终端或 VS Code，再启动项目。

## 本地数据库

当前使用本地 MySQL，默认数据库名为：

```text
novel_script
```

本地开发配置写在：

```text
backend/src/main/resources/application.yml
```

当前默认本地连接为：

- 用户名：`root`
- 密码：`123456`

后端启动时会自动加载 SQL 初始化文件：

```text
backend/src/main/resources/db/schema.sql
backend/src/main/resources/db/data.sql
```

## 最小接口

当前后端已经提供以下最小接口：

- `POST /api/projects`
- `GET /api/projects/{projectId}`
- `POST /api/projects/{projectId}/chapters`
- `POST /api/projects/{projectId}/adaptations`
- `GET /api/projects/{projectId}/scripts/latest`

其中：

- `POST /api/projects/{projectId}/adaptations` 会同步执行一次最小改编链路，生成新的 `ScriptVersion` 和 `YamlSnapshot`
- `GET /api/projects/{projectId}/scripts/latest` 用于查询指定项目当前最新的剧本 YAML 初稿

## YAML Schema 文档

剧本 YAML 的字段说明和设计原因见：

- [SCRIPT_YAML_SCHEMA.md](SCRIPT_YAML_SCHEMA.md)

## 设计说明

当前采用分阶段流水线：

```text
章节清洗
 -> 章节上下文抽取
 -> 全局上下文合并
 -> Story Bible 构建
 -> 剧本大纲规划
 -> 分场剧本生成
 -> YAML 序列化
 -> Schema 校验
 -> 版本保存
```

当前 v1 的最小生成入口为同步调用，便于比赛演示、联调和结果验证；后续如果需要任务队列或异步编排，可以在不改变核心领域模型的前提下扩展。
