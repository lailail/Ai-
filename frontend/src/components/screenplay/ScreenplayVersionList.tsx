import { Button, List, Skeleton, Space, Tag, Typography } from "antd";
import type { ScriptVersionSummary } from "../../types/adaptation";

type ScreenplayVersionListProps = {
  versionSummaries: ScriptVersionSummary[];
  selectedScriptVersionId: number | null;
  isLoading: boolean;
  onSelectVersion: (scriptVersionId: number) => void;
};

/**
 * 展示正式剧本工作区左侧的版本列表。
 */
export function ScreenplayVersionList({
  versionSummaries,
  selectedScriptVersionId,
  isLoading,
  onSelectVersion
}: ScreenplayVersionListProps) {
  if (isLoading) {
    return (
      <section className="screenplay-side-panel">
        <Skeleton active paragraph={{ rows: 8 }} />
      </section>
    );
  }

  return (
    <section className="screenplay-side-panel">
      <div className="panel-heading">
        <Typography.Text className="eyebrow">正式剧本</Typography.Text>
        <Typography.Title level={5}>版本列表</Typography.Title>
      </div>
      <List
        dataSource={versionSummaries}
        locale={{ emptyText: "当前项目还没有可查看的剧本版本。" }}
        renderItem={(version) => {
          const selected = version.scriptVersionId === selectedScriptVersionId;
          const label = `第 ${version.versionNo} 版 · ${version.title}`;

          return (
            <List.Item className="yaml-version-item">
              <Button
                type={selected ? "primary" : "default"}
                className="version-card-button"
                onClick={() => onSelectVersion(version.scriptVersionId)}
              >
                <div className="version-card-content">
                  <div className="version-card-header">
                    <Typography.Text className="version-card-title">{label}</Typography.Text>
                    <Typography.Text className="version-card-time">
                      {formatVersionTime(version.createdAt)}
                    </Typography.Text>
                  </div>
                  <Space size={[6, 6]} wrap className="version-card-tags">
                    {version.latest ? <Tag color="blue">最新</Tag> : null}
                    <Tag color={version.validationStatus === "PASSED" ? "success" : "error"}>
                      {version.validationStatus === "PASSED" ? "YAML 已通过" : "YAML 未通过"}
                    </Tag>
                  </Space>
                </div>
              </Button>
            </List.Item>
          );
        }}
      />
    </section>
  );
}

/**
 * 将时间字符串格式化为更易读的展示文本。
 *
 * @param createdAt 版本创建时间
 * @returns 格式化后的时间
 */
function formatVersionTime(createdAt: string) {
  return createdAt.replace("T", " ").slice(0, 16);
}
