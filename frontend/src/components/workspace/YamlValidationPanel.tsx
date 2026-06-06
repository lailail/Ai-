import { Alert, List, Typography } from "antd";
import type { ScriptValidationResult } from "../../types/adaptation";

type YamlValidationPanelProps = {
  validationResult: ScriptValidationResult | null;
};

/**
 * 渲染 YAML 工作区右侧的校验反馈面板。
 */
export function YamlValidationPanel({ validationResult }: YamlValidationPanelProps) {
  if (!validationResult) {
    return (
      <section className="yaml-side-panel">
        <div className="panel-heading">
          <Typography.Text className="eyebrow">Schema 校验</Typography.Text>
          <Typography.Title level={5}>校验结果</Typography.Title>
        </div>
        <Alert
          type="info"
          showIcon
          message="还没有执行新的校验"
          description="建议在保存前先执行一次 YAML 校验，确认结构和字段符合当前 Schema。"
        />
      </section>
    );
  }

  return (
    <section className="yaml-side-panel">
      <div className="panel-heading">
        <Typography.Text className="eyebrow">Schema 校验</Typography.Text>
        <Typography.Title level={5}>校验结果</Typography.Title>
      </div>
      <Alert
        type={validationResult.valid ? "success" : "error"}
        showIcon
        message={validationResult.valid ? "当前 YAML 已通过校验" : "当前版本校验未通过"}
        description={`Schema 版本：${validationResult.schemaVersion}`}
      />
      {!validationResult.valid ? (
        <List
          className="yaml-validation-list"
          dataSource={validationResult.errors}
          locale={{ emptyText: "当前没有结构化错误明细。" }}
          renderItem={(error, index) => (
            <List.Item className="yaml-validation-item">
              <Typography.Text strong>{index + 1}. {error.message}</Typography.Text>
              <Typography.Text type="secondary">{error.path || "未知路径"}</Typography.Text>
            </List.Item>
          )}
        />
      ) : null}
    </section>
  );
}
