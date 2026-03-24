import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {path: '/',name: 'home',component: () => import('../views/Camera/CameraView.vue')},
  {path: '/camera',name: 'camera',component: () => import('../views/Camera/CameraView.vue')},
]


const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router