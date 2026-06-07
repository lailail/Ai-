# 剧本 YAML Schema

这份 Schema 用来描述小说改编后的结构化剧本初稿。它不是最终拍摄版格式，而是一个方便作者继续修改、方便系统校验、也方便后续渲染成正式剧本的中间稿。

## 顶层结构

```yaml
schema_version: "1.0"
project:
  id: "project_001"
  title: "示例小说"
  source_chapters: [1, 2, 3]
  adaptation_mode: "screenplay"
story_bible:
  characters: []
  relationships: []
  locations: []
  timeline: []
  conflicts: []
  foreshadowing: []
  adaptation_strategy: []
episodes: []
metadata:
  generated_at: "2026-06-07T10:00:00+08:00"
  generator: "spring-ai-deepseek"
  notes: []
```

设计原因：

- `schema_version` 用来区分结构版本，避免后续升级时互相混淆。
- `project` 保留项目和章节来源信息，保证剧本初稿可回溯。
- `story_bible` 负责保存全局上下文，防止人物和时间线漂移。
- `episodes` 承载真正的剧本内容。
- `metadata` 用来记录生成来源和补充备注。

## 完整字段说明

### schema_version

- `schema_version`
  - 含义：当前 YAML 使用的 Schema 版本号。
  - 设计原因：后端校验、前端编辑和后续版本升级都需要它做兼容判断。

### project

- `project`
  - 含义：项目级基础信息对象。
  - 设计原因：让这份剧本初稿始终和具体小说项目绑定。
- `project.id`
  - 含义：项目唯一标识。
  - 设计原因：用于版本保存、查询和导出时稳定定位项目。
- `project.title`
  - 含义：项目标题。
  - 设计原因：作者和评审查看版本时，首先依靠标题识别作品。
- `project.source_chapters`
  - 含义：本次剧本覆盖的来源章节编号列表。
  - 设计原因：说明这版剧本具体基于哪些原文章节生成。
- `project.adaptation_mode`
  - 含义：改编模式标识，当前生成链路默认写入 `screenplay`。
  - 设计原因：明确这份结构稿面向影视和短剧剧本输出，而不是小说原文或其他格式。

### story_bible

- `story_bible`
  - 含义：全局故事设定对象。
  - 设计原因：多章节改编最容易出问题的是全局一致性，所以单独保留这一层。

#### characters

- `story_bible.characters`
  - 含义：角色设定列表。
  - 设计原因：后续场景和对白都要依赖它确认人物身份。
- `story_bible.characters[].id`
  - 含义：角色唯一标识。
  - 设计原因：程序引用角色时要靠稳定 ID，而不是靠名字文本。
- `story_bible.characters[].name`
  - 含义：角色主名称。
  - 设计原因：正式剧本渲染时默认使用这个名字展示人物。
- `story_bible.characters[].aliases`
  - 含义：角色别名或其他称呼列表。
  - 设计原因：小说里常有多种叫法，保留别名有助于上下文合并。
- `story_bible.characters[].role`
  - 含义：角色定位，例如主角、盟友、反派。
  - 设计原因：帮助作者快速判断 AI 是否理解了角色在故事中的位置。
- `story_bible.characters[].traits`
  - 含义：角色性格或行为特征关键词列表。
  - 设计原因：后续写对白和动作时，可以据此保持人物风格一致。
- `story_bible.characters[].goal`
  - 含义：角色当前最核心的目标。
  - 设计原因：人物动机是改编时最容易丢掉的关键信息。

#### relationships

- `story_bible.relationships`
  - 含义：人物关系列表。
  - 设计原因：方便后续安排冲突、合作、情感推进。
- `story_bible.relationships[].from`
  - 含义：关系起点角色 ID。
  - 设计原因：关系需要明确从谁出发。
- `story_bible.relationships[].to`
  - 含义：关系终点角色 ID。
  - 设计原因：关系需要明确指向谁。
- `story_bible.relationships[].type`
  - 含义：关系类型。
  - 设计原因：先用结构化标签表达主关系，便于后续处理。
- `story_bible.relationships[].description`
  - 含义：关系补充说明。
  - 设计原因：很多复杂关系无法只靠一个类型标签说清楚。

#### locations

- `story_bible.locations`
  - 含义：地点设定列表。
  - 设计原因：地点统一之后，场景标题和氛围表达才更稳定。
- `story_bible.locations[].id`
  - 含义：地点唯一标识。
  - 设计原因：为后续引用和扩展保留结构锚点。
- `story_bible.locations[].name`
  - 含义：地点名称。
  - 设计原因：场景标题和说明里通常会直接使用。
- `story_bible.locations[].description`
  - 含义：地点描述。
  - 设计原因：帮助作者判断地点气质是否贴近原文。

#### timeline

- `story_bible.timeline`
  - 含义：全局时间线事件列表。
  - 设计原因：避免多章节改编后出现时间顺序错乱。
- `story_bible.timeline[].id`
  - 含义：事件唯一标识。
  - 设计原因：便于后续引用和排查冲突。
- `story_bible.timeline[].order`
  - 含义：事件排序号。
  - 设计原因：把时间顺序显式化，降低理解歧义。
- `story_bible.timeline[].summary`
  - 含义：事件摘要。
  - 设计原因：作者可以快速复核整条时间线。
- `story_bible.timeline[].source_refs`
  - 含义：事件来源章节引用。
  - 设计原因：保证时间线不是凭空捏造出来的。

#### conflicts

- `story_bible.conflicts`
  - 含义：核心冲突列表。
  - 设计原因：冲突是剧本推进的骨架，需要单独提炼。
- `story_bible.conflicts[].id`
  - 含义：冲突唯一标识。
  - 设计原因：便于跨集追踪同一条冲突线。
- `story_bible.conflicts[].summary`
  - 含义：冲突摘要。
  - 设计原因：帮助作者快速判断主冲突是否被保留。

#### foreshadowing

- `story_bible.foreshadowing`
  - 含义：伏笔列表。
  - 设计原因：伏笔在长文本改编里最容易丢，需要单独建模。
- `story_bible.foreshadowing[].id`
  - 含义：伏笔唯一标识。
  - 设计原因：方便后续回收时定位同一条伏笔。
- `story_bible.foreshadowing[].setup`
  - 含义：伏笔铺垫内容。
  - 设计原因：告诉作者前面到底埋了什么。
- `story_bible.foreshadowing[].payoff_hint`
  - 含义：伏笔回收提示。
  - 设计原因：方便后续扩写或精修时继续保留回收方向。
- `story_bible.foreshadowing[].source_refs`
  - 含义：伏笔来源章节引用。
  - 设计原因：方便作者回到原文核查。

#### adaptation_strategy

- `story_bible.adaptation_strategy`
  - 含义：改编策略建议列表。
  - 设计原因：把“原文事实”和“改编处理方式”分开保存，方便返工。

### episodes

- `episodes`
  - 含义：剧集列表。
  - 设计原因：即使 v1 先生成单集，也保留分集结构，后续扩展时不用推翻。
- `episodes[].id`
  - 含义：剧集唯一标识。
  - 设计原因：用于版本保存、导出和场景归属定位。
- `episodes[].title`
  - 含义：剧集标题。
  - 设计原因：方便作者按集浏览和管理版本。
- `episodes[].premise`
  - 含义：本集核心推进摘要。
  - 设计原因：帮助作者快速判断这一集是否跑偏。
- `episodes[].source_refs`
  - 含义：本集对应的来源章节引用。
  - 设计原因：说明这集是从哪些章节压缩、重组出来的。
- `episodes[].scenes`
  - 含义：场景列表。
  - 设计原因：场景是影视和短剧正文最核心的组织单位。

### scenes

- `episodes[].scenes[].id`
  - 含义：场景唯一标识。
  - 设计原因：便于编辑、校验错误定位和后续扩写。
- `episodes[].scenes[].slugline`
  - 含义：场景标题行，通常写时间、地点和内外景。
  - 设计原因：它是正式剧本渲染时最关键的场景锚点。
- `episodes[].scenes[].purpose`
  - 含义：这场戏的戏剧目的。
  - 设计原因：作者需要知道这场戏为什么存在。
- `episodes[].scenes[].source_refs`
  - 含义：这场戏对应的来源章节引用。
  - 设计原因：保持改编结果和原文之间的可回溯关系。
- `episodes[].scenes[].characters`
  - 含义：本场涉及的角色 ID 列表。
  - 设计原因：场景要和 Story Bible 形成稳定引用。
- `episodes[].scenes[].actions`
  - 含义：作者可直接阅读的动作描述列表。
  - 设计原因：它承载小说叙述转成影视动作表达后的主要结果。
- `episodes[].scenes[].beats`
  - 含义：更细粒度的动作推进列表。
  - 设计原因：方便作者继续拆分节奏和细节。
- `episodes[].scenes[].beats[].id`
  - 含义：节拍唯一标识。
  - 设计原因：便于精修和局部替换。
- `episodes[].scenes[].beats[].action`
  - 含义：单条节拍动作描述。
  - 设计原因：让场景推进比 `actions` 更细。
- `episodes[].scenes[].dialogue`
  - 含义：结构化对白列表。
  - 设计原因：既方便正式剧本渲染，也方便后端做引用校验。
- `episodes[].scenes[].dialogue[].character_id`
  - 含义：说这句台词的角色 ID。
  - 设计原因：对白必须和 Story Bible 中的角色绑定。
- `episodes[].scenes[].dialogue[].parenthetical`
  - 含义：括号提示，例如语气、动作或情绪。
  - 设计原因：让正式剧本更接近真实影视写法。
- `episodes[].scenes[].dialogue[].line`
  - 含义：对白正文。
  - 设计原因：这是剧本最直接面向演员和观众的文本。
- `episodes[].scenes[].dialogue[].subtext`
  - 含义：潜台词说明。
  - 设计原因：方便作者继续打磨人物真实意图。
- `episodes[].scenes[].transition`
  - 含义：场景转场提示，例如 `CUT_TO`。
  - 设计原因：保留基础影视写作格式。
- `episodes[].scenes[].notes`
  - 含义：场景备注对象。
  - 设计原因：把节奏、情绪和待补充事项集中保留。
- `episodes[].scenes[].notes.emotion`
  - 含义：场景情绪提示。
  - 设计原因：帮助作者把握氛围基调。
- `episodes[].scenes[].notes.pacing`
  - 含义：场景节奏提示。
  - 设计原因：帮助作者识别这场戏应该快还是慢。
- `episodes[].scenes[].notes.todo`
  - 含义：待补充事项。
  - 设计原因：方便在初稿阶段先留下后续打磨点。

### metadata

- `metadata`
  - 含义：文档元信息对象。
  - 设计原因：用于记录生成来源和附加说明。
- `metadata.generated_at`
  - 含义：当前 YAML 生成时间。
  - 设计原因：帮助作者和评审确认初稿生成时点。
- `metadata.generator`
  - 含义：生成器标识，例如当前接入链路或模型来源。
  - 设计原因：方便回溯这份初稿是由哪条生成链路产出的；当前自动生成结果默认写入 `spring-ai-deepseek`。
- `metadata.notes`
  - 含义：附加备注列表。
  - 设计原因：用于保存补充说明，而不影响主剧本结构。

## 校验规则

- `schema_version` 必须存在。
- `project.title` 必须存在。
- `story_bible` 必须存在。
- `episodes` 必须存在。
- `episodes[].id` 必须存在。
- `episodes[].scenes[].id`、`episodes[].scenes[].slugline`、`episodes[].scenes[].source_refs` 必须存在。
- `episodes[].scenes[].dialogue[].character_id` 必须能在 `story_bible.characters[].id` 中找到。
- `episodes[].scenes[].characters[]` 中引用的角色 ID 必须存在于 `story_bible.characters[].id`。
- `source_refs` 必须指向 `project.source_chapters` 中已有的章节。
- `actions` 必须是字符串数组。

