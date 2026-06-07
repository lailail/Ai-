import { lazy, Suspense, type ReactNode } from "react";
import { Spin } from "antd";
import { createBrowserRouter } from "react-router-dom";

const HomePage = lazy(async () => {
  const module = await import("../pages/HomePage");
  return { default: module.HomePage };
});

const ProjectWorkspacePage = lazy(async () => {
  const module = await import("../pages/ProjectWorkspacePage");
  return { default: module.ProjectWorkspacePage };
});

/**
 * 为路由页面提供统一的懒加载占位。
 *
 * @param element 需要渲染的页面元素
 * @returns 包含加载态的路由元素
 */
function withSuspense(element: ReactNode) {
  return (
    <Suspense
      fallback={
        <main className="page-shell">
          <div className="empty-state">
            <Spin size="large" />
          </div>
        </main>
      }
    >
      {element}
    </Suspense>
  );
}

export const router = createBrowserRouter([
  {
    path: "/",
    element: withSuspense(<HomePage />)
  },
  {
    path: "/projects/:projectId",
    element: withSuspense(<ProjectWorkspacePage />)
  }
]);
