# 剧本 YAML Schema

这份 Schema 用来描述小说改编后的剧本初稿。它的目标不是把剧本写成最终成片格式，而是让作者拿到一份结构清楚、方便修改、能继续生成或校验的 YAML。

## 顶层结构

```yaml
schema_version: "1.0"
project:
  title: ""
  source_chapters: []
  adaptation_mode: "novel_to_screenplay"
story_bible:
  characters: []
  relationships: []
  locations: []
  timeline: []
  conflicts: []
  foreshadowing: []
  tone: ""
  adaptation_strategy: ""
episodes: []
metadata:
  generated_at: ""
  generator: ""
  notes: []
```

## project

`project` 记录作品信息和来源章节。

设计原因：剧本初稿需要知道自己来自哪部小说、覆盖哪些章节。后续作者追加章节时，也可以判断当前剧本版本是基于哪些原文生成的。

常用字段：

- `title`：作品名。
- `source_chapters`：本次剧本覆盖的章节编号。
- `adaptation_mode`：改编模式，当前默认为 `novel_to_screenplay`。

## story_bible

`story_bible` 保存整部小说的改编上下文。

设计原因：小说改编成剧本时，最容易出问题的不是单句对白，而是人物前后不一致、伏笔丢失、时间线混乱。把人物、关系、地点、时间线、冲突和伏笔单独整理出来，可以让后续分场生成更稳定，也方便作者检查 AI 是否理解了原文。

常用字段：

- `characters`：人物表。
- `relationships`：人物关系。
- `locations`：地点。
- `timeline`：事件时间线。
- `conflicts`：主要冲突。
- `foreshadowing`：伏笔和回收提示。
- `tone`：整体气质。
- `adaptation_strategy`：改编策略。

## episodes

`episodes` 保存剧集或短剧分集。

设计原因：影视剧和短剧通常按集和场组织。即使第一版只生成一集，也保留 `episodes`，方便后面把更长小说拆成多集。

示例：

```yaml
episodes:
  - id: "ep01"
    title: "第一集"
    premise: "主角被卷入核心事件"
    source_refs: ["ch1", "ch2", "ch3"]
    scenes: []
```

## scenes

`scenes` 是剧本正文的主要部分。

示例：

```yaml
scenes:
  - id: "sc01"
    slugline: "夜 外 旧街"
    purpose: "建立悬念并引出主线线索"
    source_refs: ["ch1:p12-p18"]
    characters: ["char_linwan"]
    actions:
      - "林晚停下脚步，注意到墙角的血迹。"
    beats:
      - id: "beat01"
        action: "她蹲下查看，发现血迹一路延伸到巷口。"
    dialogue:
      - character_id: "char_linwan"
        parenthetical: "压低声音"
        line: "这不是普通的争执。"
        subtext: "她意识到事情严重"
    transition: "CUT_TO"
    notes:
      emotion: "压抑"
      pacing: "slow"
```

字段说明：

- `slugline`：场景标题，标明时间、内外景和地点。
- `purpose`：这场戏在剧情中的作用。
- `source_refs`：这场戏对应的原文章节或段落。
- `characters`：本场出现的人物 ID。
- `actions`：直接可读的动作说明。
- `beats`：更细的事件推进。
- `dialogue`：对白。
- `parenthetical`：对白中的语气、动作或短提示。
- `subtext`：潜台词，帮助作者继续打磨。
- `transition`：转场，例如 `CUT_TO`、`FADE_IN`、`FADE_OUT`。
- `notes`：节奏、情绪和修改备注。

## source_refs

`source_refs` 是这个 Schema 里很重要的字段。

设计原因：小说改编不是凭空创作。作者需要知道某场戏来自哪些章节，才能判断 AI 有没有删错重点、误解人物或虚构关键事实。保留来源引用，也方便后续做“只重写某一章对应场景”的功能。

## 校验规则

- `schema_version` 必须存在。
- `project.title` 必须存在。
- `story_bible` 必须存在。
- `episodes` 必须存在。
- 每个 `episode` 必须有 `id`。
- 每个 `scene` 必须有 `id`、`slugline` 和 `source_refs`。
- `dialogue.character_id` 必须能在 `story_bible.characters` 中找到。
- `source_refs` 必须指向已录入章节。


