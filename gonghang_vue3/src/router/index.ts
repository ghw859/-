import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
    {
      path: '/',
      component: () => import('../components/layout/AppLayout.vue'),
      redirect: '/overview',
      children: [
        { path: 'overview', name: 'overview', component: () => import('../views/OverviewView.vue'), meta: { title: '数据总览看板' } },
        { path: 'pre-form', name: 'pre-form', component: () => import('../views/PreFormView.vue'), meta: { title: '智能预填单解析中心' } },
        { path: 'navigation', name: 'navigation', component: () => import('../views/NavigationView.vue'), meta: { title: '智能预约排队' } },
        { path: 'qrcode', name: 'qrcode', component: () => import('../views/QRCodeView.vue'), meta: { title: '业务直通码与智能网点推荐' } },
        { path: 'progress', name: 'progress', component: () => import('../views/ProgressView.vue'), meta: { title: '办理进度追踪' } },
        { path: 'history', name: 'history', component: () => import('../views/HistoryView.vue'), meta: { title: '历史预约查询' } },
        { path: 'ai-assistant', name: 'ai-assistant', component: () => import('../views/AIAssistantView.vue'), meta: { title: '"灵枢" AI数字人大堂经理' } },
      ]
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('lingmou_logged_in')
  if (to.name !== 'login' && to.name !== 'register' && token !== 'true') {
    return '/login'
  }
})

export default router
