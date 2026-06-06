import { Alert, Skeleton, Space, Tag, Typography } from "antd";
import type { AdaptationScript } from "../../types/adaptation";

type ScriptPreviewPanelProps = {
  latestScript: AdaptationScript | null;
  isLoading: boolean;
};

export function ScriptPreviewPanel({ latestScript, isLoading }: ScriptPreviewPanelProps) {
  if (isLoading) {
    return (
      <section className="panel panel-soft">
        <Skeleton active paragraph={{ rows: 12 }} />
      </section>
    );
  }

  return (
    <section className="panel panel-soft">
      <div className="panel-heading">
        <Typography.Text className="eyebrow">最新结果</Typography.Text>
        <Typography.Title level={4}>YAML 初稿预览</Typography.Title>
      </div>
      {!latestScript ? (
        <Alert
          className="workspace-alert"
          type="info"
          showIcon
          message="还没有 YAML 结果"
          description="开始改编后，这里会展示最新 YAML 内容的预览。后续会继续接入完整编辑器。"
        />
      ) : (
        <>
          <Typography.Paragraph className="panel-copy">当前结果标题：{latestScript.title}</Typography.Paragraph>
          <Space wrap>
            <Tag color="blue">Schema 版本：{latestScript.schemaVersion}</Tag>
            <Tag color={latestScript.validationStatus === "PASSED" ? "success" : "error"}>
              校验状态：{latestScript.validationStatus}
            </Tag>
          </Space>
          <div className="yaml-preview">
            <pre>{latestScript.yamlContent}</pre>
          </div>
        </>
      )}
    </section>
  );
}
