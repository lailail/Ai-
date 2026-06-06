import { render, screen } from "@testing-library/react";
import { StoryBiblePanel } from "./StoryBiblePanel";

describe("StoryBiblePanel", () => {
  it("should render empty state when no story bible is available", () => {
    render(<StoryBiblePanel storyBible={null} isLoading={false} />);

    expect(
      screen.getByText("完成一次改编后，这里会展示当前项目的 Story Bible，帮助你快速查看角色、关系、地点、冲突和伏笔。")
    ).toBeInTheDocument();
  });

  it("should render story bible summary when data is available", () => {
    render(
      <StoryBiblePanel
        isLoading={false}
        storyBible={{
          projectId: 1,
          storyBibleId: 2,
          versionNo: 1,
          characters: [
            {
              id: "char_shenyan",
              name: "沈砚",
              aliases: [],
              role: "protagonist",
              traits: ["冷静", "敏锐"],
              goal: "查明旧案真相"
            }
          ],
          relationships: [
            {
              from: "char_shenyan",
              to: "char_linwan",
              type: "ally",
              description: "沈砚与林晚暂时结盟"
            }
          ],
          locations: [
            {
              id: "loc_old_street",
              name: "旧城巷口",
              description: "旧城入口处的狭窄街巷"
            }
          ],
          timeline: [
            {
              id: "evt_001",
              order: 1,
              summary: "沈砚回到旧城",
              sourceRefs: ["chapter:1"]
            }
          ],
          conflicts: [
            {
              id: "conf_001",
              summary: "继续追查会激怒隐藏势力"
            }
          ],
          foreshadowing: [
            {
              id: "foreshadow_001",
              setup: "墙边留下的血迹",
              payoffHint: "指向旧案现场",
              sourceRefs: ["chapter:1"]
            }
          ],
          adaptationStrategy: ["前三章压缩为开篇一集"]
        }}
      />
    );

    expect(screen.getByText("沈砚")).toBeInTheDocument();
    expect(screen.getByText("沈砚与林晚暂时结盟")).toBeInTheDocument();
    expect(screen.getByText("旧城巷口")).toBeInTheDocument();
    expect(screen.getByText("继续追查会激怒隐藏势力")).toBeInTheDocument();
    expect(screen.getByText("前三章压缩为开篇一集")).toBeInTheDocument();
  });
});
