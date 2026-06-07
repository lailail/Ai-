import { Collapse, Drawer, Space, Tag, Typography } from "antd";

type YamlFieldGuideDrawerProps = {
  open: boolean;
  onClose: () => void;
};

type FieldGuideItem = {
  key: string;
  label: string;
  meaning: string;
  reason: string;
};

const FIELD_GUIDE_ITEMS: FieldGuideItem[] = [
  {
    key: "schema_version",
    label: "schema_version",
    meaning: "标记当前 YAML 使用的 Schema 版本号。",
    reason: "后端需要根据它判断字段兼容关系，后续升级结构时也靠它区分不同版本。"
  },
  {
    key: "project",
    label: "project",
    meaning: "项目级基础信息对象。",
    reason: "让这份剧本初稿始终和具体小说项目绑定，避免脱离来源上下文。"
  },
  {
    key: "project.id",
    label: "project.id",
    meaning: "项目的结构化唯一标识。",
    reason: "方便版本保存、导出和回写时稳定定位当前项目。"
  },
  {
    key: "project.title",
    label: "project.title",
    meaning: "小说改编项目标题。",
    reason: "作者和评审在查看版本时首先依赖标题识别当前作品。"
  },
  {
    key: "project.source_chapters",
    label: "project.source_chapters[]",
    meaning: "本次剧本覆盖的来源章节编号列表。",
    reason: "帮助作者确认这版初稿到底基于哪些章节生成，也方便后续追加章节。"
  },
  {
    key: "project.adaptation_mode",
    label: "project.adaptation_mode",
    meaning: "当前改编模式标识，例如 `novel_to_screenplay`。",
    reason: "为后续扩展其他改编模式预留边界，同时保证当前语义明确。"
  },
  {
    key: "story_bible",
    label: "story_bible",
    meaning: "全局故事设定对象，保存人物、关系、地点、时间线和改编策略。",
    reason: "这是多章节改编保持前后一致的核心记忆层。"
  },
  {
    key: "story_bible.characters",
    label: "story_bible.characters[]",
    meaning: "角色设定列表。",
    reason: "所有场景和对白都要回到这里确认人物身份，避免角色漂移。"
  },
  {
    key: "story_bible.characters.id",
    label: "story_bible.characters[].id",
    meaning: "角色唯一标识。",
    reason: "场景人物引用和对白角色引用都依赖它，而不是依赖不稳定的人名文本。"
  },
  {
    key: "story_bible.characters.name",
    label: "story_bible.characters[].name",
    meaning: "角色主名称。",
    reason: "它是正式剧本渲染时默认展示的人物名字。"
  },
  {
    key: "story_bible.characters.aliases",
    label: "story_bible.characters[].aliases[]",
    meaning: "角色别名或常见称呼列表。",
    reason: "小说里同一角色常有不同叫法，保留别名有助于上下文合并和人工校对。"
  },
  {
    key: "story_bible.characters.role",
    label: "story_bible.characters[].role",
    meaning: "角色定位，例如主角、盟友、反派。",
    reason: "有助于作者快速判断 AI 是否理解了人物在故事中的戏剧位置。"
  },
  {
    key: "story_bible.characters.traits",
    label: "story_bible.characters[].traits[]",
    meaning: "角色特征关键词列表。",
    reason: "这些特征能约束后续对白和行动风格，减少角色失真。"
  },
  {
    key: "story_bible.characters.goal",
    label: "story_bible.characters[].goal",
    meaning: "角色当前最核心的目标或驱动力。",
    reason: "剧本改编时最容易丢掉人物动机，所以需要单独保留。"
  },
  {
    key: "story_bible.relationships",
    label: "story_bible.relationships[]",
    meaning: "人物关系列表。",
    reason: "帮助后续场景安排冲突、合作和情感推进。"
  },
  {
    key: "story_bible.relationships.from",
    label: "story_bible.relationships[].from",
    meaning: "关系起点角色 ID。",
    reason: "保证关系是结构化可追踪的，而不是模糊描述。"
  },
  {
    key: "story_bible.relationships.to",
    label: "story_bible.relationships[].to",
    meaning: "关系终点角色 ID。",
    reason: "与 `from` 一起构成明确的人物关系边。"
  },
  {
    key: "story_bible.relationships.type",
    label: "story_bible.relationships[].type",
    meaning: "关系类型，例如盟友、对立、师徒。",
    reason: "为后续改编规划提供稳定的关系标签。"
  },
  {
    key: "story_bible.relationships.description",
    label: "story_bible.relationships[].description",
    meaning: "关系的补充说明。",
    reason: "类型标签不够表达复杂关系时，需要简短文字补充语境。"
  },
  {
    key: "story_bible.locations",
    label: "story_bible.locations[]",
    meaning: "地点设定列表。",
    reason: "地点统一之后，场景标题和氛围渲染才更稳定。"
  },
  {
    key: "story_bible.locations.id",
    label: "story_bible.locations[].id",
    meaning: "地点唯一标识。",
    reason: "为后续做场景引用、筛选和扩展留出结构化锚点。"
  },
  {
    key: "story_bible.locations.name",
    label: "story_bible.locations[].name",
    meaning: "地点名称。",
    reason: "它是场景标题行和说明文案中最常被直接使用的字段。"
  },
  {
    key: "story_bible.locations.description",
    label: "story_bible.locations[].description",
    meaning: "地点的视觉或叙事描述。",
    reason: "帮助作者快速判断该地点是否保留了原文气质。"
  },
  {
    key: "story_bible.timeline",
    label: "story_bible.timeline[]",
    meaning: "全局时间线事件列表。",
    reason: "小说改编时最怕时间顺序错乱，所以需要单独建模。"
  },
  {
    key: "story_bible.timeline.id",
    label: "story_bible.timeline[].id",
    meaning: "时间线事件唯一标识。",
    reason: "便于后续引用、排序和排查事件冲突。"
  },
  {
    key: "story_bible.timeline.order",
    label: "story_bible.timeline[].order",
    meaning: "事件排序号。",
    reason: "直接把时间先后关系显式化，减少模型理解偏差。"
  },
  {
    key: "story_bible.timeline.summary",
    label: "story_bible.timeline[].summary",
    meaning: "事件摘要。",
    reason: "作者查看 Story Bible 时，需要快速把握时间线而不是重读原文。"
  },
  {
    key: "story_bible.timeline.source_refs",
    label: "story_bible.timeline[].source_refs[]",
    meaning: "事件对应的来源章节引用。",
    reason: "保证时间线不是凭空写出，而是能回到小说章节核对。"
  },
  {
    key: "story_bible.conflicts",
    label: "story_bible.conflicts[]",
    meaning: "核心冲突列表。",
    reason: "冲突是推进戏剧化改编的关键骨架。"
  },
  {
    key: "story_bible.conflicts.id",
    label: "story_bible.conflicts[].id",
    meaning: "冲突唯一标识。",
    reason: "便于在不同集和不同场景里追踪同一条冲突线。"
  },
  {
    key: "story_bible.conflicts.summary",
    label: "story_bible.conflicts[].summary",
    meaning: "冲突摘要。",
    reason: "让作者能快速检查主冲突有没有被 AI 抓准。"
  },
  {
    key: "story_bible.foreshadowing",
    label: "story_bible.foreshadowing[]",
    meaning: "伏笔列表。",
    reason: "伏笔最容易在长文本改编里丢失，单独建模更利于检查和回收。"
  },
  {
    key: "story_bible.foreshadowing.id",
    label: "story_bible.foreshadowing[].id",
    meaning: "伏笔唯一标识。",
    reason: "便于跨集追踪同一伏笔。"
  },
  {
    key: "story_bible.foreshadowing.setup",
    label: "story_bible.foreshadowing[].setup",
    meaning: "伏笔铺垫内容。",
    reason: "告诉作者这个伏笔在前文到底埋了什么。"
  },
  {
    key: "story_bible.foreshadowing.payoff_hint",
    label: "story_bible.foreshadowing[].payoff_hint",
    meaning: "伏笔未来的回收提示。",
    reason: "方便作者在后续扩写时继续保留回收方向。"
  },
  {
    key: "story_bible.foreshadowing.source_refs",
    label: "story_bible.foreshadowing[].source_refs[]",
    meaning: "伏笔来源章节引用。",
    reason: "让作者能快速回到原文确认 AI 是否理解准确。"
  },
  {
    key: "story_bible.adaptation_strategy",
    label: "story_bible.adaptation_strategy[]",
    meaning: "改编策略建议列表。",
    reason: "把原文事实和改编处理方式分开，后续返工时更清楚。"
  },
  {
    key: "episodes",
    label: "episodes[]",
    meaning: "剧集列表。",
    reason: "即使当前只生成一集，也先保留分集结构，便于后续扩展。"
  },
  {
    key: "episodes.id",
    label: "episodes[].id",
    meaning: "剧集唯一标识。",
    reason: "用于版本保存、导出和场景归属定位。"
  },
  {
    key: "episodes.title",
    label: "episodes[].title",
    meaning: "剧集标题。",
    reason: "方便作者按集浏览和管理版本。"
  },
  {
    key: "episodes.premise",
    label: "episodes[].premise",
    meaning: "本集核心推进摘要。",
    reason: "帮助作者在不展开场景的情况下快速判断这一集是否跑偏。"
  },
  {
    key: "episodes.source_refs",
    label: "episodes[].source_refs[]",
    meaning: "本集对应的来源章节引用。",
    reason: "说明这一集是从哪些章节压缩、重组或提炼出来的。"
  },
  {
    key: "episodes.scenes",
    label: "episodes[].scenes[]",
    meaning: "场景列表。",
    reason: "场景是影视和短剧最核心的正文组织单位。"
  },
  {
    key: "episodes.scenes.id",
    label: "episodes[].scenes[].id",
    meaning: "场景唯一标识。",
    reason: "便于编辑、定位校验错误和后续扩写。"
  },
  {
    key: "episodes.scenes.slugline",
    label: "episodes[].scenes[].slugline",
    meaning: "场景标题行，通常写时间、地点和内外景。",
    reason: "它是正式剧本渲染时最重要的场景锚点。"
  },
  {
    key: "episodes.scenes.purpose",
    label: "episodes[].scenes[].purpose",
    meaning: "这场戏的戏剧目的。",
    reason: "作者能据此判断这场戏是否真的推动了情节或人物。"
  },
  {
    key: "episodes.scenes.source_refs",
    label: "episodes[].scenes[].source_refs[]",
    meaning: "这场戏对应的来源章节引用。",
    reason: "保持改编结果与原文之间的可回溯关系。"
  },
  {
    key: "episodes.scenes.characters",
    label: "episodes[].scenes[].characters[]",
    meaning: "本场涉及的角色 ID 列表。",
    reason: "让场景和 Story Bible 之间形成稳定引用关系。"
  },
  {
    key: "episodes.scenes.actions",
    label: "episodes[].scenes[].actions[]",
    meaning: "作者可直接阅读的动作描述列表。",
    reason: "它承载小说叙述转为影视动作表达后的主要结果。"
  },
  {
    key: "episodes.scenes.beats",
    label: "episodes[].scenes[].beats[]",
    meaning: "更细粒度的动作推进列表。",
    reason: "方便作者继续拆分节奏和镜头重点。"
  },
  {
    key: "episodes.scenes.beats.id",
    label: "episodes[].scenes[].beats[].id",
    meaning: "节拍唯一标识。",
    reason: "有助于后续精修、局部替换和问题定位。"
  },
  {
    key: "episodes.scenes.beats.action",
    label: "episodes[].scenes[].beats[].action",
    meaning: "单条节拍动作描述。",
    reason: "让动作推进比 `actions` 更细，便于精修。"
  },
  {
    key: "episodes.scenes.dialogue",
    label: "episodes[].scenes[].dialogue[]",
    meaning: "结构化对白列表。",
    reason: "既方便正式剧本渲染，也方便后端做角色引用校验。"
  },
  {
    key: "episodes.scenes.dialogue.character_id",
    label: "episodes[].scenes[].dialogue[].character_id",
    meaning: "说这句台词的角色 ID。",
    reason: "对白必须和 Story Bible 里已有角色绑定，避免出现匿名漂移角色。"
  },
  {
    key: "episodes.scenes.dialogue.parenthetical",
    label: "episodes[].scenes[].dialogue[].parenthetical",
    meaning: "括号提示，例如语气、动作或情绪。",
    reason: "它能让正式剧本更接近真实写法，也便于作者打磨表演层次。"
  },
  {
    key: "episodes.scenes.dialogue.line",
    label: "episodes[].scenes[].dialogue[].line",
    meaning: "对白正文。",
    reason: "这是剧本里最直接面向观众和演员的语言内容。"
  },
  {
    key: "episodes.scenes.dialogue.subtext",
    label: "episodes[].scenes[].dialogue[].subtext",
    meaning: "潜台词说明。",
    reason: "帮助作者在精修阶段继续判断人物真实意图。"
  },
  {
    key: "episodes.scenes.transition",
    label: "episodes[].scenes[].transition",
    meaning: "场景转场提示，例如 `CUT_TO`。",
    reason: "让正式剧本展示更接近影视写作格式。"
  },
  {
    key: "episodes.scenes.notes",
    label: "episodes[].scenes[].notes",
    meaning: "场景备注对象。",
    reason: "把节奏、情绪和待补充项单独收纳，避免污染正文。"
  },
  {
    key: "episodes.scenes.notes.emotion",
    label: "episodes[].scenes[].notes.emotion",
    meaning: "场景情绪提示。",
    reason: "帮助作者在正式剧本润色时把握氛围基调。"
  },
  {
    key: "episodes.scenes.notes.pacing",
    label: "episodes[].scenes[].notes.pacing",
    meaning: "场景节奏提示。",
    reason: "让作者快速识别这场戏应当快还是慢。"
  },
  {
    key: "episodes.scenes.notes.todo",
    label: "episodes[].scenes[].notes.todo",
    meaning: "场景待补充事项。",
    reason: "方便在初稿阶段保留后续需要继续打磨的点。"
  },
  {
    key: "metadata",
    label: "metadata",
    meaning: "文档元信息对象。",
    reason: "用于记录生成来源和补充说明，方便追踪版本。"
  },
  {
    key: "metadata.generated_at",
    label: "metadata.generated_at",
    meaning: "当前 YAML 生成时间。",
    reason: "帮助作者和评审确认这份初稿的生成时点。"
  },
  {
    key: "metadata.generator",
    label: "metadata.generator",
    meaning: "生成器标识，例如使用的模型名。",
    reason: "方便回溯这份初稿的技术来源。"
  },
  {
    key: "metadata.notes",
    label: "metadata.notes[]",
    meaning: "附加备注列表。",
    reason: "用于保存补充说明，而不影响主剧本结构。"
  }
];

/**
 * 展示 YAML 字段说明的抽屉组件。
 */
export function YamlFieldGuideDrawer({ open, onClose }: YamlFieldGuideDrawerProps) {
  return (
    <Drawer
      title="YAML 字段说明"
      placement="right"
      width={460}
      open={open}
      onClose={onClose}
    >
      <Typography.Paragraph>
        这里展示的是当前 YAML Schema 已落地的完整字段说明。字段路径与实际 YAML 结构一一对应，便于你边看边改。
      </Typography.Paragraph>
      <Collapse
        items={FIELD_GUIDE_ITEMS.map((item) => ({
          key: item.key,
          label: (
            <Space size={8}>
              <Typography.Text strong>{item.label}</Typography.Text>
              <Tag color="blue">字段</Tag>
            </Space>
          ),
          children: (
            <div className="field-guide-copy">
              <Typography.Paragraph>
                <Typography.Text strong>含义：</Typography.Text>
                {item.meaning}
              </Typography.Paragraph>
              <Typography.Paragraph>
                <Typography.Text strong>设计原因：</Typography.Text>
                {item.reason}
              </Typography.Paragraph>
            </div>
          )
        }))}
      />
    </Drawer>
  );
}
