import { createBrowserRouter } from "react-router-dom";
import { HomePage } from "../pages/HomePage";
import { ProjectWorkspacePage } from "../pages/ProjectWorkspacePage";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <HomePage />
  },
  {
    path: "/projects/:projectId",
    element: <ProjectWorkspacePage />
  }
]);
