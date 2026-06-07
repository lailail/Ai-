import { createBrowserRouter } from "react-router-dom";
import { HomePage } from "../pages/HomePage";
import { ProjectWorkspacePage } from "../pages/ProjectWorkspacePage";
import { ScreenplayWorkspacePage } from "../pages/ScreenplayWorkspacePage";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <HomePage />
  },
  {
    path: "/projects/:projectId",
    element: <ProjectWorkspacePage />
  },
  {
    path: "/projects/:projectId/screenplay",
    element: <ScreenplayWorkspacePage />
  }
]);
