

/**
 * Vue Router 路由配置模块
 *
 * 定义应用的路由规则，使用 HTML5 History 模式
 * 包含首页和PLC监控页面的路由映射
 */
import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import PlcMonitor from '@/views/PlcMonitor.vue'

// 路由规则配置：定义路径与组件的映射关系
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

// 创建路由实例，配置 History 模式和路由规则
const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
