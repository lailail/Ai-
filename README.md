# AI 小说转剧本工具

AI 小说转剧本工具用于将 3 个章节以上的小说文本转换为结构化剧本 YAML，帮助作者快速获得可编辑、可继续打磨的剧本初稿。

项目的核心思路是先构建小说改编上下文 `Story Bible`，再基于上下文分阶段生成剧本，减少多章节改编中常见的人物漂移、剧情断裂、伏笔丢失和时间线混乱。

## 核心功能

- 多章节小说录入：支持同一作品下持续追加章节。
- 文本格式支持：支持粘贴文本、`.txt` 和 `.md` 小说章节。
- 上下文抽取：提取人物、关系、地点、事件、冲突、伏笔和时间线。
- Story Bible 构建：为每本小说生成独立的全局改编上下文。
- 分阶段剧本生成：先规划大纲和分场，再生成动作说明、对白和备注。
- YAML 输出：生成结构化剧本 YAML，便于编辑、校验和后续扩展。
- 版本管理：保存 AI 生成稿、用户编辑稿和修复稿。
- 多作品隔离：每本小说按 `Project` 独立管理，避免不同作品的上下文互相污染。

## 技术栈

后端：

- Java
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

## 大模型配置

当前默认使用 DeepSeek 

```text
DEEPSEEK_API_KEY
```
其他的参数配置：

- Base URL：`https://api.deepseek.com`
- Model：`deepseek-chat`
- Temperature：`0.7`

设置 API Key：

```powershell
setx DEEPSEEK_API_KEY "你的DeepSeek API Key"
```

## 本地数据库

当前使用本地 MySQL，数据库名建议为：

```text
novel_script
```

后端启动时会自动加载 SQL 初始化文件，当前目录为：

```text
backend/src/main/resources/db/schema.sql
backend/src/main/resources/db/data.sql
```

## Schema 文档

剧本 YAML 的字段说明和设计原因见 [SCRIPT_YAML_SCHEMA.md](SCRIPT_YAML_SCHEMA.md)。

## 设计说明

系统不会直接把小说原文一次性交给模型生成最终剧本，而是采用分阶段流水线：

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

这种设计可以让生成结果更稳定，也方便作者查看 AI 对原作的理解过程，并在后续继续编辑和重写。
