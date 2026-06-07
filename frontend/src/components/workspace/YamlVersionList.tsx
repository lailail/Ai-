import { Select, Skeleton, Space, Tag, Typography } from "antd";
import type { ScriptVersionSummary } from "../../types/adaptation";

type YamlVersionListProps = {
  versionSummaries: ScriptVersionSummary[];
  selectedScriptVersionId: number | null;
  isLoading: boolean;
  onSelectVersion: (scriptVersionId: number) => void;
};

/**
 * 渲染 YAML 工作区左侧的剧本版本选择区。
 */
export function YamlVersionList({
  versionSummaries,
  selectedScriptVersionId,
  isLoading,
  onSelectVersion
}: YamlVersionListProps) {
  if (isLoading) {
    return (
      <section className="yaml-side-panel">
        <Skeleton active paragraph={{ rows: 6 }} />
      </section>
    );
  }

  const selectedVersion =
    versionSummaries.find((version) => version.scriptVersionId === selectedScriptVersionId) ?? versionSummaries[0] ?? null;

  return (
    <section className="yaml-side-panel">
      <div className="panel-heading">
        <Typography.Text className="eyebrow">版本记录</Typography.Text>
        <Typography.Title level={5}>版本列表</Typography.Title>
      </div>

      <Select
        className="version-select"
        aria-label="YAML 版本选择"
        placeholder="请选择一个 YAML 版本"
        value={selectedVersion?.scriptVersionId}
        onChange={onSelectVersion}
        options={versionSummaries.map((version) => ({
          value: version.scriptVersionId,
          label: buildVersionLabel(version)
        }))}
        popupMatchSelectWidth={false}
      />

      {selectedVersion ? (
        <div className="version-summary-card">
          <div className="version-summary-header">
            <Typography.Text className="version-summary-title">
              {buildVersionLabel(selectedVersion)}
            </Typography.Text>
            <Typography.Text className="version-summary-time">
              {formatVersionTime(selectedVersion.createdAt)}
            </Typography.Text>
          </div>
          <Space size={[6, 6]} wrap className="version-card-tags">
            {selectedVersion.latest ? <Tag color="blue">最新</Tag> : null}
            <Tag color={selectedVersion.validationStatus === "PASSED" ? "success" : "error"}>
              {selectedVersion.validationStatus === "PASSED" ? "已通过" : "未通过"}
            </Tag>
            <Tag color="gold">{renderSourceType(selectedVersion.sourceType)}</Tag>
          </Space>
        </div>
      ) : (
        <Typography.Paragraph className="tiny-copy">
          当前项目还没有可查看的剧本版本。
        </Typography.Paragraph>
      )}
    </section>
  );
}

/**
 * 构造版本下拉框中使用的标签文本。
 *
 * @param version 剧本版本摘要
 * @returns 版本标签
 */
function buildVersionLabel(version: ScriptVersionSummary) {
  return `第 ${version.versionNo} 版 · ${version.title}`;
}

/**
 * 将版本来源类型转换为更易读的中文标签。
 *
 * @param sourceType 版本来源类型
 * @returns 中文来源标签
 */
function renderSourceType(sourceType: string) {
  if (sourceType === "USER_EDITED") {
    return "人工编辑";
  }
  if (sourceType === "AI_GENERATED") {
    return "AI 生成";
  }
  return sourceType;
}

/**
 * 将后端返回的时间字符串格式化为更易读的展示文本。
 *
 * @param createdAt 版本创建时间
 * @returns 面向界面的时间字符串
 */
function formatVersionTime(createdAt: string) {
  return createdAt.replace("T", " ").slice(0, 16);
}
