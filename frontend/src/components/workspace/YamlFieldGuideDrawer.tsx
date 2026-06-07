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
    meaning: "标记当前 YAML 草稿遵循的字段版本。",
    reason: "后端校验和后续升级都依赖这个版本号，避免不同结构混用。"
  },
  {
    key: "project",
    label: "project",
    meaning: "记录项目标题、来源章节和改编模式。",
    reason: "它负责把这份剧本和哪本小说、哪批章节绑定起来。"
  },
  {
    key: "story_bible",
    label: "story_bible",
    meaning: "保存人物、关系、地点、时间线、冲突和伏笔等全局上下文。",
    reason: "这是保证多集、多场景改编前后一致的核心记忆层。"
  },
  {
    key: "episodes",
    label: "episodes",
    meaning: "按集组织改编结果，每一集都包含 premise 和 scenes。",
    reason: "影视和短剧通常需要以集为单位审阅、返工和继续扩写。"
  },
  {
    key: "premise",
    label: "premise",
    meaning: "概括本集的核心推进方向和情绪目标。",
    reason: "方便作者快速判断这一集是否还在正确的改编主线上。"
  },
  {
    key: "slugline",
    label: "slugline",
    meaning: "场景标题行，一般写时间、地点和内外景。",
    reason: "这是后续生成正式剧本页面时最关键的场景锚点。"
  },
  {
    key: "actions",
    label: "actions",
    meaning: "描述镜头里实际发生的动作和事件。",
    reason: "动作段是把小说叙述改成影视化表达的主要承载区。"
  },
  {
    key: "dialogue",
    label: "dialogue",
    meaning: "保存角色对白、括注和潜台词。",
    reason: "结构化对白更方便后端校验、正式剧本渲染和后续编辑。"
  },
  {
    key: "transition",
    label: "transition",
    meaning: "记录场景之间的转场提示。",
    reason: "它能帮助正式剧本展示更接近真实影视写作格式。"
  },
  {
    key: "metadata",
    label: "metadata",
    meaning: "补充生成时间、模型信息和备注。",
    reason: "便于回溯这份草稿是怎么来的，也方便比赛演示时说明来源。"
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
      width={420}
      open={open}
      onClose={onClose}
    >
      <Typography.Paragraph>
        这里列的是当前 v1 已经落地的关键字段。它们既服务后端 Schema 校验，也服务正式剧本渲染和版本保存。
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
