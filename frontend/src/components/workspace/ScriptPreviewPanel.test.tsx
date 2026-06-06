import { fireEvent, render, screen } from "@testing-library/react";
import { vi } from "vitest";
import { ScriptPreviewPanel } from "./ScriptPreviewPanel";

vi.mock("@monaco-editor/react", () => ({
  default: ({
    value,
    onChange
  }: {
    value?: string;
    onChange?: (value: string) => void;
  }) => (
    <textarea
      data-testid="yaml-editor"
      value={value ?? ""}
      onChange={(event) => onChange?.(event.target.value)}
    />
  )
}));

describe("ScriptPreviewPanel", () => {
  it("should render version list and validation errors for the selected script", () => {
    render(
      <ScriptPreviewPanel
        versionSummaries={[
          {
            projectId: 1,
            scriptVersionId: 12,
            versionNo: 3,
            title: "作者精修版",
            sourceType: "USER_EDITED",
            validationStatus: "FAILED",
            latest: true,
            createdAt: "2026-06-06T14:30:00"
          },
          {
            projectId: 1,
            scriptVersionId: 11,
            versionNo: 2,
            title: "AI 初稿",
            sourceType: "AI_GENERATED",
            validationStatus: "PASSED",
            latest: false,
            createdAt: "2026-06-06T14:00:00"
          }
        ]}
        selectedScript={{
          projectId: 1,
          scriptVersionId: 12,
          versionNo: 3,
          title: "作者精修版",
          sourceType: "USER_EDITED",
          schemaVersion: "1.0",
          validationStatus: "FAILED",
          validationErrors: [
            {
              path: "script.scenes[0].title",
              message: "场景标题不能为空",
              rejectedValue: ""
            }
          ],
          createdAt: "2026-06-06T14:30:00",
          yamlContent: "schema_version: 1.0\nscript:\n  title: 旧城疑影",
          jobId: 9,
          jobStatus: "SUCCEEDED"
        }}
        draftTitle="作者精修版"
        draftYamlContent="schema_version: 1.0\nscript:\n  title: 旧城疑影"
        validationResult={{
          projectId: 1,
          schemaVersion: "1.0",
          valid: false,
          errors: [
            {
              path: "script.scenes[0].title",
              message: "场景标题不能为空",
              rejectedValue: ""
            }
          ]
        }}
        hasUnsavedChanges={true}
        isListLoading={false}
        isDetailLoading={false}
        isValidating={false}
        isSaving={false}
        onSelectVersion={() => {}}
        onDraftTitleChange={() => {}}
        onDraftYamlChange={() => {}}
        onValidate={() => {}}
        onSave={() => {}}
      />
    );

    expect(screen.getByText("版本列表")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /第 3 版 · 作者精修版/ })).toBeInTheDocument();
    expect(screen.getByText("当前版本校验未通过")).toBeInTheDocument();
    expect(screen.getByText(/场景标题不能为空/)).toBeInTheDocument();
    expect(screen.getByDisplayValue("作者精修版")).toBeInTheDocument();
  });

  it("should trigger version switch validate and save callbacks", () => {
    const handleSelectVersion = vi.fn();
    const handleDraftTitleChange = vi.fn();
    const handleDraftYamlChange = vi.fn();
    const handleValidate = vi.fn();
    const handleSave = vi.fn();

    render(
      <ScriptPreviewPanel
        versionSummaries={[
          {
            projectId: 1,
            scriptVersionId: 12,
            versionNo: 3,
            title: "作者精修版",
            sourceType: "USER_EDITED",
            validationStatus: "FAILED",
            latest: true,
            createdAt: "2026-06-06T14:30:00"
          },
          {
            projectId: 1,
            scriptVersionId: 11,
            versionNo: 2,
            title: "AI 初稿",
            sourceType: "AI_GENERATED",
            validationStatus: "PASSED",
            latest: false,
            createdAt: "2026-06-06T14:00:00"
          }
        ]}
        selectedScript={{
          projectId: 1,
          scriptVersionId: 12,
          versionNo: 3,
          title: "作者精修版",
          sourceType: "USER_EDITED",
          schemaVersion: "1.0",
          validationStatus: "FAILED",
          validationErrors: [],
          createdAt: "2026-06-06T14:30:00",
          yamlContent: "schema_version: 1.0",
          jobId: 9,
          jobStatus: "SUCCEEDED"
        }}
        draftTitle="作者精修版"
        draftYamlContent="schema_version: 1.0"
        validationResult={null}
        hasUnsavedChanges={true}
        isListLoading={false}
        isDetailLoading={false}
        isValidating={false}
        isSaving={false}
        onSelectVersion={handleSelectVersion}
        onDraftTitleChange={handleDraftTitleChange}
        onDraftYamlChange={handleDraftYamlChange}
        onValidate={handleValidate}
        onSave={handleSave}
      />
    );

    fireEvent.click(screen.getByRole("button", { name: /第 2 版 · AI 初稿/ }));
    fireEvent.change(screen.getByLabelText("版本标题"), { target: { value: "终稿" } });
    fireEvent.change(screen.getByTestId("yaml-editor"), { target: { value: "schema_version: 1.1" } });
    fireEvent.click(screen.getByRole("button", { name: "校验 YAML" }));
    fireEvent.click(screen.getByRole("button", { name: "另存为新版本" }));

    expect(handleSelectVersion).toHaveBeenCalledWith(11);
    expect(handleDraftTitleChange).toHaveBeenCalledWith("终稿");
    expect(handleDraftYamlChange).toHaveBeenCalledWith("schema_version: 1.1");
    expect(handleValidate).toHaveBeenCalled();
    expect(handleSave).toHaveBeenCalled();
  });
});
