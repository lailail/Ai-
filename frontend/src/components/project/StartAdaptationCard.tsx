import { Button, Typography } from "antd";
import { canStartAdaptation } from "../../utils/chapter";

type StartAdaptationCardProps = {
  chapterCount: number;
  onStart: () => void;
};

export function StartAdaptationCard({ chapterCount, onStart }: StartAdaptationCardProps) {
  const enabled = canStartAdaptation(chapterCount);

  return (
    <section className="panel panel-contrast">
      <div className="panel-heading">
        <Typography.Text className="eyebrow">下一步</Typography.Text>
        <Typography.Title level={4}>改编入口</Typography.Title>
      </div>
      <Typography.Paragraph className="panel-copy">
        录入章节后，这里会作为改编流程的启动入口。当前 commit 先完成章节整理与录入闭环，下一次提交会接入完整工作台。
      </Typography.Paragraph>
      <div className="action-row">
        <Button type="primary" size="large" disabled={!enabled} onClick={onStart}>
          开始改编
        </Button>
        <Typography.Text type={enabled ? "success" : "secondary"}>
          {enabled ? "章节数量已达标，可以进入下一步联调。" : "至少需要录入 3 章内容后才能进入改编流程。"}
        </Typography.Text>
      </div>
    </section>
  );
}
