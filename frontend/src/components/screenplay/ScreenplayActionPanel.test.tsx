import { render, screen } from "@testing-library/react";
import { ScreenplayActionPanel } from "./ScreenplayActionPanel";

describe("ScreenplayActionPanel", () => {
  it("should not show the back-to-yaml button", () => {
    render(
      <ScreenplayActionPanel
        selectedScreenplay={{
          projectId: 1,
          scriptVersionId: 2,
          versionNo: 3,
          title: "长夜余烬",
          sourceType: "USER_EDITED",
          renderVersion: "render-v3",
          markdownContent: "# 正式剧本",
          createdAt: "2026-06-07T10:00:00",
          updatedAt: "2026-06-07T10:05:00"
        }}
        canRender={true}
        hasUnsavedChanges={false}
        isRendering={false}
        isSaving={false}
        onRender={() => {}}
        onSave={() => {}}
        onExportMarkdown={() => {}}
        onExportText={() => {}}
      />
    );

    expect(screen.queryByRole("button", { name: "返回 YAML 初稿" })).not.toBeInTheDocument();
  });
});
