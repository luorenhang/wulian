import { createRouter, createWebHistory } from 'vue-router'
import PlcMonitor from '@/views/PlcMonitor.vue'

const routes = [
  {
    path: '/',
    name: 'PlcMonitor',
    component: PlcMonitor
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
