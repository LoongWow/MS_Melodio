import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Index',
    component: () => import('../views/index/Index.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue')
  },
  {
    path: '/my',
    name: 'My',
    component: () => import('../views/my/My.vue')
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('../views/search/Search.vue')
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/settings/Settings.vue')
  },
  {
    path: '/rank',
    name: 'Rank',
    component: () => import('../views/rank/Rank.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
