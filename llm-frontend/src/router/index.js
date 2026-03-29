import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/prompt-test'
  },
  {
    path: '/prompt-test',
    name: 'PromptTest',
    component: () => import('../views/PromptTest.vue')
  },
  {
    path: '/content-optimization',
    name: 'ContentOptimization',
    component: () => import('../views/ContentOptimization.vue')
  },
  {
    path: '/template-management',
    name: 'TemplateManagement',
    component: () => import('../views/TemplateManagement.vue')
  },
  {
    path: '/model-management',
    name: 'ModelManagement',
    component: () => import('../views/ModelManagement.vue')
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/Chat.vue')
  },
  {
    path: '/prompt-comparison',
    name: 'PromptComparison',
    component: () => import('../views/PromptComparison.vue')
  },
  {
    path: '/markdown-to-image',
    name: 'MarkdownToImage',
    component: () => import('../views/MarkdownToImage.vue')
  },
  {
    path: '/doc-viewer',
    name: 'DocViewer',
    component: () => import('../views/DocViewer.vue')
  },
  {
    path: '/secretary',
    name: 'Secretary',
    component: () => import('../views/Secretary.vue')
  },
  {
    path: '/note-capture',
    name: 'NoteCapture',
    component: () => import('../views/NoteCapture.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

