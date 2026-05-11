Date: 2026-05-05
Task: 重构前端整体页面的 UI/UX，提升一致性和可用性。
Files created or modified: `src/App.tsx`, `src/app/navigation.ts`, `src/styles/index.css`, `docs/dev-log.md`
Key design decisions: 把导航配置从页面组件中抽离成独立数据文件；保留原有路由能力，只重构外层布局与交互。
Reason for the change: 旧页面信息层次不清、视觉噪音较多，影响使用效率和专业感。
User-facing improvement: 侧边栏导航更清晰，顶部信息更聚焦，页面结构更现代。
Maintainability note: 页面结构与导航数据解耦后，后续新增菜单和标题只需改配置，减少改动风险。
