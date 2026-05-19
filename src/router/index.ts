import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import PlcMonitor from '@/views/PlcMonitor.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/plc',
    name: 'PlcMonitor',
    component: PlcMonitor
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router