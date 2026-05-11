import type { LucideIcon } from 'lucide-react'
import {
  Compass,
  FileText,
  GitBranch,
  GitCompare,
  Image,
  LayoutGrid,
  MessageSquare,
  Notebook,
  Settings,
  UserCheck,
  Wrench,
  Bot
} from 'lucide-react'

export type MenuItem = {
  name: string
  path: string
  icon: LucideIcon
  description: string
}

export type MenuGroup = {
  label: string
  items: MenuItem[]
}

export type PageMeta = {
  title: string
  subtitle: string
}

export const menuGroups: MenuGroup[] = [
  {
    label: 'Core',
    items: [
      { name: '智能对话', path: '/chat', icon: MessageSquare, description: '多模型实时会话与上下文追踪' },
      { name: '知识库', path: '/doc-viewer', icon: FileText, description: '文档检索、管理与结构化阅读' },
      { name: '碎片记录', path: '/note-capture', icon: Notebook, description: '快速记录并沉淀灵感与待办' }
    ]
  },
  {
    label: 'Engineering',
    items: [
      { name: 'Prompt 实验室', path: '/prompt-test', icon: LayoutGrid, description: 'Prompt 设计与参数实验工作台' },
      { name: 'Prompt A/B 对比', path: '/prompt-comparison', icon: GitCompare, description: '多版本结果对比与评估' },
      { name: '内容优化', path: '/content-optimization', icon: Compass, description: '内容分析、润色和改写' },
      { name: '模型管理', path: '/model-management', icon: Settings, description: '模型接入、配置与路由策略' },
      { name: '模板管理', path: '/template-management', icon: FileText, description: '业务模板组织与复用' }
    ]
  },
  {
    label: 'Orchestration',
    items: [
      { name: '智能体', path: '/agent-list', icon: Bot, description: '智能体清单、能力和状态管理' },
      { name: 'Agent 编排', path: '/graph-orchestration', icon: GitBranch, description: '可视化构建 Agent 工作流' },
      { name: 'Tool 管理', path: '/tool-management', icon: Wrench, description: '工具注册、配置和权限控制' }
    ]
  },
  {
    label: 'Utilities',
    items: [
      { name: 'Markdown 绘图', path: '/markdown-to-image', icon: Image, description: 'Markdown 生成视觉化图片' },
      { name: '个人助理', path: '/secretary', icon: UserCheck, description: '任务提醒与效率辅助' }
    ]
  }
]

export const pageMetaMap: Record<string, PageMeta> = {
  '/chat': { title: '智能对话', subtitle: '与多模型协作，统一会话管理与推理上下文。' },
  '/doc-viewer': { title: '知识库中心', subtitle: '聚合文档索引、检索和结构化阅读体验。' },
  '/note-capture': { title: '碎片化笔记', subtitle: '快速捕捉灵感并沉淀为可复用知识资产。' },
  '/prompt-test': { title: 'Prompt 工程', subtitle: '进行 Prompt 迭代实验与效果验证。' },
  '/prompt-comparison': { title: 'Prompt A/B 对比', subtitle: '对比多版本输出差异，量化优化价值。' },
  '/content-optimization': { title: '内容优化', subtitle: '统一执行润色、压缩、改写与结构优化。' },
  '/model-management': { title: '模型控制台', subtitle: '管理模型配置、访问策略与稳定性。' },
  '/template-management': { title: '模板管理', subtitle: '模板分层组织，提升交付一致性。' },
  '/agent-list': { title: '智能体中心', subtitle: '维护 Agent 生命周期与能力清单。' },
  '/graph-orchestration': { title: 'Agent 编排', subtitle: '通过可视化流程设计复杂任务编排。' },
  '/tool-management': { title: 'Tool 管理', subtitle: '标准化工具能力接入与运行治理。' },
  '/markdown-to-image': { title: 'Markdown 绘图', subtitle: '将文本内容快速转化为视觉表达。' },
  '/secretary': { title: '个人助理', subtitle: '聚合提醒、计划与工作节奏管理。' }
}

export const defaultPageMeta: PageMeta = {
  title: 'LLM 控制台',
  subtitle: '统一管理对话、编排、工具和模型能力。'
}
