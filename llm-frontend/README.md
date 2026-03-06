# LLM 编排平台 - 前端项目

基于 Vue 3 + Vite + Element Plus 构建的现代化前端应用。

## 功能特性

- 🧪 **Prompt 测试工具**：可视化测试不同模板和模型的输出效果
- ✨ **内容优化**：针对不同社交媒体平台优化文本内容
- 📝 **模板管理**：可视化管理 Prompt 模板
- 💬 **对话测试**：测试对话功能

## 技术栈

- Vue 3 - 渐进式 JavaScript 框架
- Vite - 下一代前端构建工具
- Element Plus - Vue 3 组件库
- Vue Router - 路由管理
- Pinia - 状态管理
- Axios - HTTP 客户端

## 快速开始

### 1. 安装依赖

```bash
cd llm-frontend
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 3. 构建生产版本

```bash
npm run build
```

## 项目结构

```
llm-frontend/
├── src/
│   ├── api/              # API 接口
│   │   └── index.js      # API 定义
│   ├── router/           # 路由配置
│   │   └── index.js      # 路由定义
│   ├── utils/            # 工具函数
│   │   └── request.js    # Axios 封装
│   ├── views/            # 页面组件
│   │   ├── PromptTest.vue           # Prompt 测试
│   │   ├── ContentOptimization.vue  # 内容优化
│   │   ├── TemplateManagement.vue   # 模板管理
│   │   └── Chat.vue                 # 对话测试
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── index.html            # HTML 模板
├── vite.config.js        # Vite 配置
└── package.json          # 项目配置
```

## 功能说明

### Prompt 测试工具

- 选择数据库中的模板
- 填写模板变量
- 选择 LLM 模型
- 调整参数（temperature、maxTokens）
- 查看渲染后的 Prompt 和 LLM 输出
- 显示性能指标（Token 使用、执行耗时）

### 内容优化

- 输入原始内容
- 选择目标平台（小红书、抖音、TikTok 等）
- 选择内容风格（专业、轻松、幽默等）
- 生成 1-5 个优化版本
- 查看优化后的内容、建议标题和标签

### 模板管理

- 查看所有模板
- 创建新模板
- 编辑现有模板
- 删除模板
- 按分类筛选

### 对话测试

- 发送消息
- 查看对话历史
- 选择模型和提供商
- 显示 Token 使用情况

## API 代理配置

开发环境下，所有 `/api` 请求会被代理到后端服务（默认 http://localhost:8080）。

配置位于 `vite.config.js`：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 环境要求

- Node.js >= 16
- npm >= 8

## 开发建议

1. 使用 VS Code + Volar 插件获得最佳开发体验
2. 启用 ESLint 和 Prettier 保持代码风格一致
3. 遵循 Vue 3 Composition API 最佳实践

## 常见问题

### 1. 端口被占用

修改 `vite.config.js` 中的端口号：

```javascript
server: {
  port: 3001  // 改为其他端口
}
```

### 2. API 请求失败

确保后端服务已启动并运行在 http://localhost:8080

### 3. 依赖安装失败

尝试清除缓存后重新安装：

```bash
rm -rf node_modules package-lock.json
npm install
```

## License

MIT

