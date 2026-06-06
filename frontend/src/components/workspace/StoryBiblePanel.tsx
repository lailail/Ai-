import { Divider, List, Skeleton, Tag, Typography } from "antd";
import type { StoryBibleSnapshot } from "../../types/adaptation";

type StoryBiblePanelProps = {
  storyBible: StoryBibleSnapshot | null;
  isLoading: boolean;
};

export function StoryBiblePanel({ storyBible, isLoading }: StoryBiblePanelProps) {
  if (isLoading) {
    return (
      <section className="panel">
        <Skeleton active paragraph={{ rows: 10 }} />
      </section>
    );
  }

  if (!storyBible) {
    return (
      <section className="panel">
        <div className="panel-heading">
          <Typography.Text className="eyebrow">Story Bible</Typography.Text>
          <Typography.Title level={4}>项目级改编上下文</Typography.Title>
        </div>
        <Typography.Paragraph className="panel-copy">
          完成一次改编后，这里会展示当前项目的 Story Bible，帮助你快速查看角色、关系、地点、冲突和伏笔。
        </Typography.Paragraph>
      </section>
    );
  }

  return (
    <section className="panel">
      <div className="panel-heading">
        <Typography.Text className="eyebrow">Story Bible</Typography.Text>
        <Typography.Title level={4}>项目级改编上下文</Typography.Title>
      </div>
      <Typography.Paragraph className="panel-copy">当前版本：第 {storyBible.versionNo} 版</Typography.Paragraph>

      <div className="workspace-block">
        <Typography.Text strong>主要角色</Typography.Text>
        <List
          size="small"
          dataSource={storyBible.characters}
          locale={{ emptyText: "暂无角色信息" }}
          renderItem={(character) => (
            <List.Item className="workspace-list-item">
              <div>
                <Typography.Text strong>{character.name}</Typography.Text>
                <Typography.Paragraph className="tiny-copy">{character.goal || "暂无角色目标"}</Typography.Paragraph>
              </div>
              <Tag>{character.role}</Tag>
            </List.Item>
          )}
        />
      </div>

      <Divider />

      <div className="workspace-block">
        <Typography.Text strong>人物关系</Typography.Text>
        <List
          size="small"
          dataSource={storyBible.relationships}
          locale={{ emptyText: "暂无人物关系" }}
          renderItem={(relationship) => (
            <List.Item className="workspace-list-item">
              <Typography.Text>{relationship.description}</Typography.Text>
            </List.Item>
          )}
        />
      </div>

      <Divider />

      <div className="workspace-block">
        <Typography.Text strong>关键地点</Typography.Text>
        <List
          size="small"
          dataSource={storyBible.locations}
          locale={{ emptyText: "暂无地点信息" }}
          renderItem={(location) => (
            <List.Item className="workspace-list-item">
              <div>
                <Typography.Text>{location.name}</Typography.Text>
                <Typography.Paragraph className="tiny-copy">{location.description}</Typography.Paragraph>
              </div>
            </List.Item>
          )}
        />
      </div>

      <Divider />

      <div className="workspace-block">
        <Typography.Text strong>时间线</Typography.Text>
        <List
          size="small"
          dataSource={storyBible.timeline}
          locale={{ emptyText: "暂无时间线信息" }}
          renderItem={(event) => (
            <List.Item className="workspace-list-item">
              <Typography.Text>{event.summary}</Typography.Text>
            </List.Item>
          )}
        />
      </div>

      <Divider />

      <div className="workspace-block">
        <Typography.Text strong>核心冲突</Typography.Text>
        <List
          size="small"
          dataSource={storyBible.conflicts}
          locale={{ emptyText: "暂无冲突信息" }}
          renderItem={(conflict) => (
            <List.Item className="workspace-list-item">
              <Typography.Text>{conflict.summary}</Typography.Text>
            </List.Item>
          )}
        />
      </div>

      <Divider />

      <div className="workspace-block">
        <Typography.Text strong>伏笔</Typography.Text>
        <List
          size="small"
          dataSource={storyBible.foreshadowing}
          locale={{ emptyText: "暂无伏笔信息" }}
          renderItem={(foreshadowing) => (
            <List.Item className="workspace-list-item">
              <Typography.Text>{foreshadowing.setup}</Typography.Text>
            </List.Item>
          )}
        />
      </div>

      <Divider />

      <div className="workspace-block">
        <Typography.Text strong>改编策略</Typography.Text>
        <div className="tag-flow">
          {storyBible.adaptationStrategy.length > 0 ? (
            storyBible.adaptationStrategy.map((item) => (
              <Tag key={item} color="blue">
                {item}
              </Tag>
            ))
          ) : (
            <Typography.Text type="secondary">暂无改编策略</Typography.Text>
          )}
        </div>
      </div>
    </section>
  );
}
