import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import { HomePage } from "./HomePage";

const navigateMock = vi.fn();
const listProjectsMock = vi.fn();
const createProjectMock = vi.fn();
const saveRecentProjectIdMock = vi.fn();

vi.mock("../api/projects", () => ({
  listProjects: () => listProjectsMock(),
  createProject: (payload: unknown) => createProjectMock(payload)
}));

vi.mock("../utils/recent-projects", () => ({
  getRecentProjectIds: () => [2],
  saveRecentProjectId: (projectId: number) => saveRecentProjectIdMock(projectId)
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>("react-router-dom");
  return {
    ...actual,
    useNavigate: () => navigateMock
  };
});

function renderHomePage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false
      }
    }
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("HomePage", () => {
  it("should show selected project detail when choosing from dropdown", async () => {
    listProjectsMock.mockResolvedValueOnce([
      {
        id: 1,
        title: "旧城疑影",
        description: "悬疑向项目",
        status: "DRAFT",
        chapterCount: 3,
        createdAt: null,
        updatedAt: null
      },
      {
        id: 2,
        title: "长夜余烬",
        description: "短剧改编项目",
        status: "DRAFT",
        chapterCount: 6,
        createdAt: null,
        updatedAt: null
      }
    ]);

    renderHomePage();

    expect(await screen.findByText("继续已有项目")).toBeInTheDocument();
    expect(await screen.findByText("短剧改编项目")).toBeInTheDocument();

    fireEvent.mouseDown(screen.getByRole("combobox"));
    fireEvent.click(await screen.findByText("旧城疑影"));

    expect(await screen.findByText("悬疑向项目")).toBeInTheDocument();
    expect(screen.getByText("已录入 3 章")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /进入项目/ }));

    await waitFor(() => {
      expect(saveRecentProjectIdMock).toHaveBeenCalledWith(1);
      expect(navigateMock).toHaveBeenCalledWith("/projects/1");
    });
  });
});
