<template>
  <div class="plc-container">
    <div class="header-section">
      <div class="header-left">
        <h2 class="page-title">⚡ PLC数据监控</h2>
        <p class="page-subtitle">实时监控西门子S7-1200 PLC输入点位状态</p>
      </div>
      <div class="status-bar">
        <div class="status-indicator" :class="{ connected: status.connected }">
          <span class="status-dot"></span>
          <span class="status-text">{{ status.connected ? '已连接' : '未连接' }}</span>
        </div>
        <button v-if="!status.connected" class="btn btn-primary" @click="connectPlc">
          <span class="btn-icon">🔗</span>
          <span>连接PLC</span>
        </button>
        <button v-else class="btn btn-secondary" @click="refreshData">
          <span class="btn-icon">🔄</span>
          <span>刷新数据</span>
        </button>
      </div>
    </div>

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon active-icon">
          <span>✅</span>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ activeCount }}</span>
          <span class="stat-label">激活点位</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon total-icon">
          <span>📊</span>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ plcData.length }}</span>
          <span class="stat-label">监控点位</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon time-icon">
          <span>⏱️</span>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ lastUpdateTime }}</span>
          <span class="stat-label">最后更新</span>
        </div>
      </div>
    </div>

    <div class="section">
      <h3 class="section-title">
        <span class="section-icon">🔌</span>
        PLC输入点位
      </h3>
      <div class="data-grid">
        <div 
          class="data-card" 
          v-for="item in plcData" 
          :key="item.id"
          :class="{ active: item.value === 1 }"
        >
          <div class="card-header">
            <span class="tag-name">{{ item.tagName }}</span>
            <span class="status-badge" :class="{ on: item.value === 1 }">
              {{ item.value === 1 ? 'ON' : 'OFF' }}
            </span>
          </div>
          <div class="card-body">
            <div class="point-description">{{ getPointDescription(item.tagName) }}</div>
            <div class="value-display" :class="{ active: item.value === 1 }">
              <span class="value-icon">{{ item.value === 1 ? '💡' : '💤' }}</span>
              <span class="value-text">{{ item.value === 1 ? '运行中' : '停止' }}</span>
            </div>
          </div>
          <div class="card-footer">
            <span class="data-type">{{ item.dataType }}</span>
            <span class="update-time">{{ formatTime(item.timestamp) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { plcApi, type PlcData } from '@/api'

const plcData = ref<PlcData[]>([])
const status = ref({ connected: false, message: '' })
const lastUpdateTime = ref('--:--:--')

let refreshInterval: ReturnType<typeof setInterval>

const pointDescriptions: Record<string, string> = {
  'I0.2': '复位按钮',
  'I0.6': '机械手90度到位',
  'I1.0': '机械手上限位',
  'I2.6': '检测气缸前进',
  'I3.1': '阻挡气缸放行',
  'I3.7': '回收库光电传感器'
}

const activeCount = computed(() => {
  return plcData.value.filter(item => item.value === 1).length
})

const getPointDescription = (tagName: string): string => {
  return pointDescriptions[tagName] || '未命名点位'
}

const fetchData = async () => {
  try {
    plcData.value = await plcApi.getPlcData()
    status.value = await plcApi.getPlcStatus()
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (error) {
    status.value = { connected: false, message: '获取数据失败' }
  }
}

const refreshData = () => {
  fetchData()
}

const connectPlc = async () => {
  try {
    const result = await plcApi.connectPlc()
    status.value = result
    if (result.connected) {
      fetchData()
    }
  } catch (error) {
    status.value = { connected: false, message: '连接失败' }
  }
}

const formatTime = (timestamp: string): string => {
  try {
    const date = new Date(timestamp)
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch {
    return '-'
  }
}

onMounted(() => {
  fetchData()
  refreshInterval = setInterval(fetchData, 3000)
})

onUnmounted(() => {
  clearInterval(refreshInterval)
})
</script>

<style scoped>
.plc-container {
  max-width: 1400px;
  margin: 0 auto;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 25px 30px;
  margin-bottom: 30px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.page-title {
  font-size: 1.8rem;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
}

.page-subtitle {
  font-size: 0.95rem;
  color: #6b7280;
  margin: 0;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 15px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #fef2f2;
  border-radius: 30px;
  transition: all 0.3s ease;
}

.status-indicator.connected {
  background: #ecfdf5;
}

.status-dot {
  width: 10px;
  height: 10px;
  background: #ef4444;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.status-indicator.connected .status-dot {
  background: #10b981;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  font-weight: 600;
  color: #dc2626;
}

.status-indicator.connected .status-text {
  color: #059669;
}

.btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover {
  background: #e5e7eb;
  transform: translateY(-2px);
}

.btn-icon {
  font-size: 1rem;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 15px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: transform 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.3rem;
}

.active-icon {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.total-icon {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
}

.time-icon {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.8rem;
  font-weight: 700;
  color: #1f2937;
}

.stat-label {
  font-size: 0.85rem;
  color: #6b7280;
}

.section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 25px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.3rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 20px 0;
  padding-left: 10px;
  border-left: 4px solid #667eea;
}

.section-icon {
  font-size: 1.1rem;
}

.data-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.data-card {
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  border-radius: 16px;
  padding: 20px;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.data-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
}

.data-card.active {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border-color: #10b981;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.tag-name {
  font-weight: 700;
  font-size: 1.1rem;
  color: #1f2937;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  background: #fee2e2;
  color: #dc2626;
}

.status-badge.on {
  background: #dcfce7;
  color: #16a34a;
}

.card-body {
  margin-bottom: 12px;
}

.point-description {
  font-size: 0.9rem;
  color: #6b7280;
  margin-bottom: 15px;
}

.value-display {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 10px;
}

.value-display.active {
  background: rgba(16, 185, 129, 0.1);
}

.value-icon {
  font-size: 1.5rem;
}

.value-text {
  font-size: 1.1rem;
  font-weight: 600;
  color: #374151;
}

.value-display.active .value-text {
  color: #059669;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.data-type {
  font-size: 0.75rem;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 3px 8px;
  border-radius: 4px;
}

.update-time {
  font-size: 0.75rem;
  color: #9ca3af;
}
</style>