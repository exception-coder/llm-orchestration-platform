# GEMINI.md - LLM Frontend

This document provides updated context and guidelines for the LLM Frontend project, reflecting the latest architectural refinements and development conventions.

## Project Overview

The **LLM Frontend** is a modern web application serving as the interface for an LLM orchestration platform. It is built with **Vue 3 (Composition API)**, **Vite**, and **Element Plus**.

### Main Technologies

- **Framework:** Vue 3 (Composition API with `<script setup>`)
- **Build Tool:** Vite
- **UI Components:** Element Plus (with `@element-plus/icons-vue` icons)
- **State Management:** Pinia (Currently managed within components; no global stores yet)
- **Routing:** Vue Router (Lazy-loaded views)
- **HTTP Client:** Axios (for standard requests) & Fetch (for SSE)
- **Security:** AES-GCM Client-side encryption (Web Crypto API)
- **Content:** `marked` for Markdown, `highlight.js` for code highlighting, `html2canvas` for image generation.

## Architecture & Directory Structure

- `src/api/index.js`: Centralized API definitions grouped by business logic (`promptTestAPI`, `chatAPI`, `noteAPI`, `docViewerAPI`, `secretaryAPI`, etc.).
- `src/composables/`: Reusable logic layer:
    - `useMarkdown.js`: Configured `marked` and `highlight.js` for consistent rendering.
    - `useResponsive.js`: Mobile/Desktop breakpoint detection with automatic resize listeners.
    - `useSSEStream.js`: Encapsulated SSE handling (POST + line-by-line parsing).
- `src/styles/`: 
    - `variables.css`: Centralized theme management via CSS custom properties (`--app-*`).
    - `markdown.css`: Shared styling for `.markdown-rendered` containers.
- `src/utils/`:
    - `request.js`: Axios instance with baseURL `/api/v1` and unified error messaging.
    - `crypto.js`: AES-GCM encryption/decryption for sensitive data (e.g., Note Capture).
- `src/views/`: Page-level components implementing core features.
- `src/App.vue`: Root layout with a responsive sidebar navigation (using `el-sub-menu`) and main content area.

## Building and Running

### Development

```bash
# Install dependencies
npm install

# Start development server (Port 3000)
# Proxy redirects /api to http://localhost:8080
npm run dev
```

### Production

```bash
# Build for production
npm run build

# Preview production build
npm run preview
```

## Development Conventions

- **Component Style:** Use **`<script setup>`** and Composition API exclusively. **Avoid Options API.**
- **UI Standards:**
    - Use **Element Plus** components. Icons are globally registered.
    - **No Hardcoded Colors:** Always use CSS variables from `src/styles/variables.css` (e.g., `var(--app-color-primary)`).
- **Logic Reuse:**
    - Use `useResponsive()` for mobile checks instead of direct `window.innerWidth`.
    - Use `useMarkdown()` for all Markdown rendering; apply `class="markdown-rendered"` to containers.
    - Use `useSSEStream()`'s `fetchSSE` for any streaming LLM responses.
- **API Communication:**
    - Standard requests: Use the `request` utility from `src/utils/request.js`.
    - New endpoints: Add to `src/api/index.js`.
- **Workflow for New Pages:**
    1. Create `.vue` file in `src/views/`.
    2. Add lazy-loaded route to `src/router/index.js`.
    3. Update sidebar menu in `App.vue` (under appropriate `el-sub-menu`).
    4. Define required APIs in `src/api/index.js`.

## Core Features

- **Prompt Engineering:** Visual testing, comparison, and template management.
- **Content Optimization:** Platform-specific refining (Red, TikTok, etc.).
- **Smart Note Capture:** Encrypted notes with AI-based categorization.
- **Doc Viewer:** Documentation tree browsing with search and indexing.
- **AI Secretary:** Specialized chat with memory and tool integration.
- **Markdown-to-Image:** Exporting styled Markdown content as images with multiple templates.
