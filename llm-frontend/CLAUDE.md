# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
npm install      # 安装依赖
npm run dev      # 启动开发服务器 (http://localhost:3000)
npm run build    # 生产构建
npm run preview  # 预览生产构建
```

后端服务需运行在 `http://localhost:8080`，Vite 开发代理将 `/api` 请求转发至后端。

## 技术栈

Vue 3 + Vite + **Tailwind CSS v4** + Element Plus + Pinia，新拟态（Neumorphism）设计语言。

| 库 | 用途 |
|---|---|
| `tailwindcss@4` + `@tailwindcss/vite` | 原子化 CSS，通过 `@theme` 定义设计令牌（无 tailwind.config.js） |
| `element-plus` | 表单控件 / 表格 / 弹窗等重型组件 |
| `lucide-vue-next` | 图标库（新代码统一使用，替代 @element-plus/icons-vue） |
| `@vueuse/motion` | 物理动效指令（`v-motion-pop`、`v-motion-slide-visible-bottom` 等） |
| `clsx` + `tailwind-merge` | 类名合并工具，封装为 `utils/utils.js` 的 `cn()` |

## 分层结构

```
src/
├── main.js                  # 入口：Tailwind → Element Plus → Router → Pinia → Motion
├── App.vue                  # 根布局：分组侧边栏 + 主题切换 + 页面过渡动画
├── api/index.js             # 后端 API 集中定义，按业务分组导出
├── composables/
│   ├── useResponsive.js     # 移动端断点检测（768px），自动注册/清理 resize
│   ├── useMarkdown.js       # marked + highlight.js 配置与渲染
│   └── useSSEStream.js      # SSE 流式请求封装（POST + 逐行解析）
├── styles/
│   ├── index.css            # Tailwind v4 入口：@theme 设计令牌 + neo-convex/neo-concave 工具类
│   ├── variables.css        # CSS 自定义属性（--app-*），主题切换的中继变量
│   ├── themes.css           # 三套主题定义（glass / neomorph / cyber），覆写 --app-* 变量
│   ├── components-reset.css # Element Plus 组件样式覆写，强制映射到主题令牌
│   └── markdown.css         # .markdown-rendered 共享 Markdown 渲染样式
├── utils/
│   ├── request.js           # Axios 实例，baseURL /api/v1，超时 60s
│   ├── crypto.js            # AES-GCM 客户端加密（Web Crypto API + PBKDF2）
│   └── utils.js             # cn() — clsx + tailwind-merge 类名合并
├── router/index.js          # 11 条路由，全部懒加载
└── views/                   # 页面级组件（11 个）
```

## 导航结构

`App.vue` 使用 `router-link` + 分组菜单，三个分组：

| 分组 | 页面 |
|---|---|
| **核心** | 智能对话 `/chat`、知识库 `/doc-viewer`、碎片记录 `/note-capture` |
| **工程** | Prompt 实验室 `/prompt-test`、内容优化 `/content-optimization`、模型管理 `/model-management` |
| **工具** | Markdown 绘图 `/markdown-to-image`、个人助理 `/secretary` |

新增页面：`views/` 创建 `.vue` → `router/index.js` 添加路由 → `App.vue` 的 `menuGroups` 数组添加条目 → `api/index.js` 添加 API。

## 样式体系（三层）

### 第一层：Tailwind v4 设计令牌（`styles/index.css`）

通过 `@theme` 块定义语义色（oklch 色彩空间）、圆角、阴影：
- `--color-background` / `--color-foreground` / `--color-primary` / `--color-card`
- `--shadow-neo-flat`（凸起）/ `--shadow-neo-inset`（凹陷）
- 工具类：`.neo-convex`（凸起卡片/按钮）、`.neo-concave`（凹陷槽位/输入框）

### 第二层：CSS 变量中继（`styles/variables.css`）

`--app-*` 变量定义侧边栏、气泡、阴影、模糊等语义值。`themes.css` 中每套主题通过 `[data-theme='xxx']` 选择器覆写这些变量。

### 第三层：Element Plus 覆写（`styles/components-reset.css`）

强制将 `.el-input__wrapper`、`.el-button`、`.el-popper` 等映射到 `--app-*` 变量，确保表单控件跟随主题。

### 主题切换

`App.vue` 通过 `data-theme` 属性切换，值存入 `localStorage('app-theme')`。当前三套：
- **默认**（空值）— 亮色新拟态
- `glass` — 暗色毛玻璃（`backdrop-filter: blur`）
- `neomorph` — 亮色拟物化（双向阴影浮雕）
- `cyber` — 暗色赛博朋克（霓虹发光边框）

## 编码约定

- 组件使用 **`<script setup>`**（Composition API）
- 样式优先用 **Tailwind 工具类** + `neo-convex` / `neo-concave`，不写 scoped CSS 中的硬编码颜色
- 图标使用 **lucide-vue-next**，按需 import（`import { Settings, Trash2 } from 'lucide-vue-next'`）
- 动效使用 **`v-motion-*`** 指令（`v-motion-pop`、`v-motion-slide-visible-bottom` 等）
- 移动端检测用 `useResponsive()` composable
- Markdown 渲染用 `useMarkdown()` composable + `class="markdown-rendered"`
- SSE 流式请求用 `useSSEStream()` 的 `fetchSSE(url, body, { onContent, onTokenUsage })`
- 非流式 HTTP 用 `utils/request.js`（Axios），不直接使用 axios
- 类名合并用 `cn()` 工具函数（`import { cn } from '@/utils/utils'`）
- scoped 样式中引用 Tailwind 需加 `@reference "@/styles/index.css";`
- **禁止硬编码颜色值**，使用 Tailwind 语义类（`text-foreground/60`、`bg-primary`）或 `--app-*` 变量
- 状态管理已引入 Pinia 但目前无 store 文件，状态均在组件内管理
