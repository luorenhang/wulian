<template>
  <div class="home-container">
    <header class="header">
      <h1>工业物联网监控平台</h1>
      <p class="subtitle">PLC数据采集与MQTT消息监控</p>
    </header>
    
    <div class="cards">
      <div class="card" @click="navigateTo('/plc')">
        <div class="card-icon plc-icon">📊</div>
        <h3>PLC数据监控</h3>
        <p>实时查看PLC设备采集的数据</p>
        <div class="status-dot" :class="{ active: plcStatus.connected }"></div>
      </div>
      
      <div class="card" @click="navigateTo('/mqtt')">
        <div class="card-icon mqtt-icon">📡</div>
        <h3>MQTT消息监控</h3>
        <p>监控和管理MQTT消息队列</p>
        <div class="status-dot" :class="{ active: mqttStatus.connected }"></div>
      </div>
    </div>

    <div class="info-panel">
      <div class="info-item">
        <span class="label">PLC连接状态:</span>
        <span class="value" :class="{ success: plcStatus.connected, error: !plcStatus.connected }">
          {{ plcStatus.connected ? '已连接' : '未连接' }}
        </span>
      </div>
      <div class="info-item">
        <span class="label">MQTT连接状态:</span>
        <span class="value" :class="{ success: mqttStatus.connected, error: !mqttStatus.connected }">
          {{ mqttStatus.connected ? '已连接' : '未连接' }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { plcApi, mqttApi } from '@/api'

const router = useRouter()
const plcStatus = ref({ connected: false, message: '' })
const mqttStatus = ref({ connected: false, message: '' })

let refreshInterval: ReturnType<typeof setInterval>

const fetchStatus = async () => {
  try {
    const [plc, mqtt] = await Promise.all([plcApi.getPlcStatus(), mqttApi.getStatus()])
    plcStatus.value = plc
    mqttStatus.value = mqtt
  } catch {
    plcStatus.value = { connected: false, message: '连接失败' }
    mqttStatus.value = { connected: false, message: '连接失败' }
  }
}

const navigateTo = (path: string) => {
  router.push(path)
}

onMounted(() => {
  fetchStatus()
  refreshInterval = setInterval(fetchStatus, 5000)
})

onUnmounted(() => {
  clearInterval(refreshInterval)
})
</script>

<style scoped>
.home-container {
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  text-align: center;
  margin-bottom: 40px;
}

.header h1 {
  font-size: 2.5rem;
  color: #1e3a5f;
  margin-bottom: 10px;
}

.subtitle {
  color: #6b7280;
  font-size: 1.1rem;
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
  margin-bottom: 40px;
}

.card {
  background: white;
  border-radius: 16px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.card-icon {
  font-size: 3rem;
  margin-bottom: 20px;
}

.card h3 {
  color: #1e3a5f;
  font-size: 1.3rem;
  margin-bottom: 10px;
}

.card p {
  color: #6b7280;
  font-size: 0.95rem;
}

.status-dot {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #ef4444;
  transition: background 0.3s;
}

.status-dot.active {
  background: #22c55e;
}

.info-panel {
  display: flex;
  justify-content: center;
  gap: 40px;
  flex-wrap: wrap;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.label {
  color: #6b7280;
  font-size: 0.95rem;
}

.value {
  font-weight: 600;
  font-size: 0.95rem;
  padding: 4px 12px;
  border-radius: 20px;
}

.value.success {
  background: #dcfce7;
  color: #16a34a;
}

.value.error {
  background: #fee2e2;
  color: #dc2626;
}
</style>
