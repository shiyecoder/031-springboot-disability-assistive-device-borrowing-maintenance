import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('./views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('./layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('./views/Dashboard.vue'), meta: { title: '辅具服务看板' } },
      { path: 'device/list', component: () => import('./views/DeviceList.vue'), meta: { title: '器具资源目录' } },
      { path: 'device/inspection', component: () => import('./views/Inspection.vue'), meta: { title: '检测消毒记录' } },
      { path: 'device/reservation', component: () => import('./views/Reservation.vue'), meta: { title: '库存预留' } },
      { path: 'borrow/list', component: () => import('./views/BorrowList.vue'), meta: { title: '借用维护单' } },
      { path: 'borrow/create', component: () => import('./views/BorrowForm.vue'), meta: { title: '新建借用申请' } },
      { path: 'borrow/detail/:id', component: () => import('./views/BorrowDetail.vue'), meta: { title: '借用单详情' } },
      { path: 'borrow/assessment', component: () => import('./views/Assessment.vue'), meta: { title: '适配评估表' } },
      { path: 'borrow/approval', component: () => import('./views/Approval.vue'), meta: { title: '借用审批台' } },
      { path: 'borrow/handover', component: () => import('./views/Handover.vue'), meta: { title: '交接管理' } },
      { path: 'borrow/return', component: () => import('./views/ReturnCheck.vue'), meta: { title: '归还检查' } },
      { path: 'repair/list', component: () => import('./views/RepairList.vue'), meta: { title: '故障维修工单' } },
      { path: 'lifecycle/list', component: () => import('./views/Lifecycle.vue'), meta: { title: '适配与维护提醒' } },
      { path: 'feedback/list', component: () => import('./views/Feedback.vue'), meta: { title: '回访评价' } },
      { path: 'admin/rule', component: () => import('./views/RuleConfig.vue'), meta: { title: '规则配置' } },
      { path: 'admin/user', component: () => import('./views/UserList.vue'), meta: { title: '用户与角色' } },
      { path: 'admin/menu', component: () => import('./views/MenuList.vue'), meta: { title: '菜单管理' } },
      { path: 'profile', component: () => import('./views/Profile.vue'), meta: { title: '个人中心' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) return '/login'
  if (to.path === '/login' && token) return '/dashboard'
  return true
})

export default router
