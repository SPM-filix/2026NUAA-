import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
  { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { public: true } },
  { path: '/', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { tab: 'home' } },
  { path: '/me/qrcode', name: 'qrcode', component: () => import('@/views/QrCodeView.vue') },
  { path: '/me/deliveries', name: 'deliveries', component: () => import('@/views/DeliveryListView.vue') },
  { path: '/me/deliveries/:id', name: 'delivery-detail', component: () => import('@/views/DeliveryDetailView.vue') },
  { path: '/me/points', name: 'points', component: () => import('@/views/PointsView.vue') },
  { path: '/mall', name: 'mall', component: () => import('@/views/MallView.vue'), meta: { tab: 'mall' } },
  { path: '/mall/result/:orderId', name: 'exchange-result', component: () => import('@/views/ExchangeResultView.vue') },
  { path: '/mall/:id', name: 'product-detail', component: () => import('@/views/ProductDetailView.vue') },
  { path: '/me/exchanges', name: 'exchanges', component: () => import('@/views/ExchangeListView.vue') },
  { path: '/announcements', name: 'announcements', component: () => import('@/views/AnnouncementsView.vue') },
  { path: '/announcements/:id', name: 'announcement-detail', component: () => import('@/views/AnnouncementDetailView.vue') },
  { path: '/me/appeals/new', name: 'appeal-new', component: () => import('@/views/AppealNewView.vue') },
  { path: '/me/appeals', name: 'appeals', component: () => import('@/views/AppealsView.vue') },
  { path: '/me', name: 'mine', component: () => import('@/views/MineView.vue'), meta: { tab: 'mine' } },
  { path: '/me/profile', name: 'profile', component: () => import('@/views/ProfileView.vue') },
  { path: '/me/password', name: 'password', component: () => import('@/views/PasswordView.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    if (auth.isLoggedIn && (to.name === 'login' || to.name === 'register')) {
      return { path: '/' }
    }
    return true
  }
  if (!auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (auth.mustChangePassword && to.name !== 'password') {
    return { name: 'password', query: { forced: '1' } }
  }
  return true
})

export default router
