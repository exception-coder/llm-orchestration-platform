# GEMINI.md - LLM Frontend (React Version)

This document provides updated context and guidelines for the LLM Frontend project, which has been migrated from Vue 3 to React.

## Project Overview

The **LLM Frontend** is a modern web application serving as the interface for an LLM orchestration platform. It is built with **React 19**, **Vite**, and **Tailwind CSS v4**.

### Main Technologies

- **Framework:** React 19 (Functional Components with Hooks)
- **Build Tool:** Vite
- **Styling:** Tailwind CSS v4 (with Neumorphism design system)
- **Routing:** React Router 7
- **State Management:** React Hooks (useState, useMemo, etc.)
- **Icons:** Lucide React
- **Animation:** Framer Motion
- **Flow/Graph:** @xyflow/react (React Flow)
- **HTTP Client:** Axios (for standard requests) & Fetch (for SSE)

## Architecture & Directory Structure

- `src/api/index.ts`: Centralized API definitions grouped by business logic.
- `src/hooks/`: Reusable logic layer:
    - `useMarkdown.ts`: Configured `marked` and `highlight.js` for consistent rendering.
    - `useResponsive.ts`: Mobile/Desktop breakpoint detection.
    - `useSSEStream.ts`: Encapsulated SSE handling (POST + line-by-line parsing).
    - `useGraphLayout.ts`: Dagre-based layout for flow graphs.
- `src/styles/`: 
    - `index.css`: Tailwind v4 entry with theme definitions.
- `src/utils/`:
    - `request.ts`: Axios instance with baseURL `/api/v1`.
    - `crypto.ts`: AES-GCM encryption/decryption.
- `src/views/`: Page-level components.
- `src/App.tsx`: Root layout with a responsive sidebar and theme management.

## Building and Running

### Development

```bash
# Install dependencies
npm install

# Start development server (Port 3000)
npm run dev
```

## Development Conventions

- **Component Style:** Use Functional Components and Hooks.
- **UI Standards:**
    - Use Tailwind CSS v4 utility classes.
    - Neumorphism classes: `.neo-convex` (elevated), `.neo-concave` (inset).
- **Logic Reuse:**
    - Use `useResponsive()` for mobile checks.
    - Use `useMarkdown()` for all Markdown rendering.
    - Use `useSSEStream()`'s `fetchSSE` for streaming LLM responses.

## Core Features

- **Prompt Engineering:** Visual testing, comparison, and template management.
- **Content Optimization:** Platform-specific refining.
- **Smart Note Capture:** Encrypted notes with AI categorization.
- **Doc Viewer:** Documentation tree browsing with search.
- **Agent Orchestration:** Visual graph-based agent flow construction using React Flow.
