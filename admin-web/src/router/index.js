import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/change-password',
    name: 'change-password',
    component: () => import('@/views/ChangePasswordView.vue'),
    meta: { requiresAuth: true, title: '修改密码' }
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/ForbiddenView.vue'),
    meta: { requiresAuth: true, title: '无权访问' }
  },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'workbench',
        name: 'workbench',
        component: () => import('@/views/recycler/WorkbenchView.vue'),
        meta: { title: '工作台', roles: ['RECYCLER'] }
      },
      {
        path: 'workbench/identify',
        name: 'identify',
        component: () => import('@/views/recycler/IdentifyView.vue'),
        meta: { title: '识别用户', roles: ['RECYCLER'] }
      },
      {
        path: 'workbench/delivery',
        name: 'delivery-create',
        component: () => import('@/views/recycler/DeliveryCreateView.vue'),
        meta: { title: '投递登记', roles: ['RECYCLER'] }
      },
      {
        path: 'workbench/redeem',
        name: 'redeem',
        component: () => import('@/views/recycler/RedeemView.vue'),
        meta: { title: '兑换核销', roles: ['RECYCLER'] }
      },
      {
        path: 'workbench/records',
        name: 'my-records',
        component: () => import('@/views/recycler/MyRecordsView.vue'),
        meta: { title: '我的流水', roles: ['RECYCLER'] }
      },
      {
        path: 'workbench/rules',
        name: 'my-rules',
        component: () => import('@/views/recycler/RulesView.vue'),
        meta: { title: '积分规则', roles: ['RECYCLER'] }
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: { title: '数据看板', roles: ['ADMIN'] }
      },
      {
        path: 'config/categories',
        name: 'categories',
        component: () => import('@/views/admin/CategoriesView.vue'),
        meta: { title: '垃圾类别', roles: ['ADMIN'] }
      },
      {
        path: 'config/rules',
        name: 'rules',
        component: () => import('@/views/admin/RulesView.vue'),
        meta: { title: '积分规则', roles: ['ADMIN'] }
      },
      {
        path: 'config/sites',
        name: 'sites',
        component: () => import('@/views/admin/SitesView.vue'),
        meta: { title: '回收站点', roles: ['ADMIN'] }
      },
      {
        path: 'mall/products',
        name: 'products',
        component: () => import('@/views/admin/ProductsView.vue'),
        meta: { title: '商品管理', roles: ['ADMIN'] }
      },
      {
        path: 'accounts/users',
        name: 'accounts-users',
        component: () => import('@/views/admin/UsersView.vue'),
        meta: { title: '用户账号', roles: ['ADMIN'] }
      },
      {
        path: 'accounts/recyclers',
        name: 'accounts-recyclers',
        component: () => import('@/views/admin/RecyclersView.vue'),
        meta: { title: '回收员审核', roles: ['ADMIN'] }
      },
      {
        path: 'audit/deliveries',
        name: 'audit-deliveries',
        component: () => import('@/views/admin/AuditDeliveriesView.vue'),
        meta: { title: '流水审计', roles: ['ADMIN'] }
      },
      {
        path: 'audit/points',
        name: 'audit-points',
        component: () => import('@/views/admin/PointsAdjustView.vue'),
        meta: { title: '积分调整', roles: ['ADMIN'] }
      },
      {
        path: 'audit/orders',
        name: 'audit-orders',
        component: () => import('@/views/admin/OrdersView.vue'),
        meta: { title: '兑换订单', roles: ['ADMIN'] }
      },
      {
        path: 'audit/appeals',
        name: 'audit-appeals',
        component: () => import('@/views/admin/AppealsView.vue'),
        meta: { title: '申诉处理', roles: ['ADMIN'] }
      },
      {
        path: 'content/announcements',
        name: 'announcements',
        component: () => import('@/views/admin/AnnouncementsView.vue'),
        meta: { title: '公告管理', roles: ['ADMIN'] }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    if (auth.token) return auth.homePath
    return true
  }

  if (!auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (!auth.profile) {
    try {
      await auth.refreshProfile()
    } catch {
      auth.clearSession()
      return { name: 'login' }
    }
  }

  if (to.name === 'change-password') return true

  if (auth.mustChangePassword) {
    return { name: 'change-password' }
  }

  if (to.meta.roles && !to.meta.roles.includes(auth.role)) {
    return { name: 'forbidden' }
  }

  if (to.path === '/') return auth.homePath

  return true
})

router.afterEach((to) => {
  document.title = `${to.meta.title || '管理台'} · 绿积分回收管理台`
})

export default router
