<template>
  <div class="plc-container">
    <div class="header-section">
      <h2>PLC数据监控</h2>
      <div class="status-bar">
        <span class="status-text">连接状态:</span>
        <span class="status-badge" :class="{ connected: status.connected }">
          {{ status.connected ? '已连接' : '未连接' }}
        </span>
        <button v-if="!status.connected" class="connect-btn" @click="connectPlc">🔗 连接PLC</button>
        <button v-else class="refresh-btn" @click="refreshData">🔄 刷新</button>
      </div>
    </div>

    <div class="section">
      <h3 class="section-title">⚡ PLC输入点位</h3>
      <div class="data-grid">
        <div class="data-card" v-for="item in plcData" :key="item.id">
          <div class="data-header">
            <span class="tag-name">{{ item.tagName }}</span>
            <span class="data-type">{{ item.dataType }}</span>
          </div>
          <div class="data-description">{{ getPointDescription(item.tagName) }}</div>
          <div class="data-value" :class="{ active: item.value === 1 }">{{ item.value === 1 ? 'ON' : 'OFF' }}</div>
          <div class="data-time">{{ formatTime(item.timestamp) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { plcApi, type PlcData } from '@/api'

const plcData = ref<PlcData[]>([])
const status = ref({ connected: false, message: '' })

let refreshInterval: ReturnType<typeof setInterval>

const pointDescriptions: Record<string, string> = {
  'I0.2': '复位按钮',
  'I0.6': '机械手90度到位',
  'I1.0': '机械手上限位',
  'I2.6': '检测气缸前进',
  'I3.1': '阻挡气缸放行',
  'I3.7': '回收库光电传感器'
}

const getPointDescription = (tagName: string): string => {
  return pointDescriptions[tagName] || ''
}

const fetchData = async () => {
  try {
    plcData.value = await plcApi.getPlcData()
    status.value = await plcApi.getPlcStatus()
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
    return date.toLocaleString('zh-CN', {
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
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.header-section h2 {
  color: #1e3a5f;
  font-size: 1.8rem;
}

.status-bar {
  display: flex;
  align-items: center;
  gap: 15px;
}

.status-text {
  color: #6b7280;
}

.status-badge {
  padding: 6px 16px;
  border-radius: 20px;
  background: #fee2e2;
  color: #dc2626;
  font-weight: 600;
  font-size: 0.85rem;
}

.status-badge.connected {
  background: #dcfce7;
  color: #16a34a;
}

.refresh-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  background: #3b82f6;
  color: white;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.3s;
}

.refresh-btn:hover {
  background: #2563eb;
}

.connect-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  background: #10b981;
  color: white;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.3s;
}

.connect-btn:hover {
  background: #059669;
}

.section {
  margin-bottom: 30px;
}

.section-title {
  color: #1e3a5f;
  font-size: 1.2rem;
  margin-bottom: 15px;
  padding-left: 10px;
  border-left: 4px solid #3b82f6;
}

.data-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.data-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.data-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.tag-name {
  font-weight: 600;
  color: #1e3a5f;
}

.data-type {
  font-size: 0.8rem;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
}

.data-description {
  font-size: 0.9rem;
  color: #6b7280;
  margin-bottom: 12px;
}

.data-value {
  font-size: 2rem;
  font-weight: 700;
  color: #3b82f6;
  margin-bottom: 8px;
}

.data-value.active {
  color: #16a34a;
}

.data-time {
  font-size: 0.8rem;
  color: #9ca3af;
}
</style>