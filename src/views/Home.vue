<template>
  <div class="home-container">
    <header class="hero-section">
      <div class="hero-content">
        <div class="hero-icon">
          <span>🏭</span>
        </div>
        <h1>工业物联网监控平台</h1>
        <p class="subtitle">实时监控PLC数据采集</p>
      </div>
    </header>

    <div class="stats-overview">
      <div class="stat-item">
        <div class="stat-circle" :class="{ active: plcStatus.connected }">
          <span class="stat-number">{{ plcStatus.connected ? 'ON' : 'OFF' }}</span>
        </div>
        <span class="stat-label">PLC设备</span>
      </div>
    </div>

    <div class="cards-grid">
      <div class="feature-card" @click="navigateTo('/plc')">
        <div class="card-icon-wrapper plc">
          <span>📊</span>
        </div>
        <div class="card-content">
          <h3>PLC数据监控</h3>
          <p>实时查看西门子S7-1200 PLC输入点位状态，监控生产设备运行情况</p>
        </div>
        <div class="card-arrow">
          <span>→</span>
        </div>
      </div>
    </div>

    <div class="info-section">
      <h2 class="section-title">系统概览</h2>
      <div class="info-grid">
        <div class="info-card">
          <div class="info-header">
            <span class="info-icon">🔧</span>
            <span class="info-title">PLC设备</span>
          </div>
          <div class="info-status" :class="{ success: plcStatus.connected }">
            <span class="status-indicator"></span>
            <span>{{ plcStatus.connected ? '已连接' : '离线' }}</span>
          </div>
          <p class="info-desc">西门子S7-1200 PLC数据采集</p>
        </div>

        <div class="info-card">
          <div class="info-header">
            <span class="info-icon">💾</span>
            <span class="info-title">数据存储</span>
          </div>
          <div class="info-status success">
            <span class="status-indicator"></span>
            <span>正常</span>
          </div>
          <p class="info-desc">PostgreSQL数据库</p>
        </div>

        <div class="info-card">
          <div class="info-header">
            <span class="info-icon">🌐</span>
            <span class="info-title">网络通信</span>
          </div>
          <div class="info-status success">
            <span class="status-indicator"></span>
            <span>正常</span>
          </div>
          <p class="info-desc">TCP/IP协议通信</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { plcApi } from '@/api'

const router = useRouter()
const plcStatus = ref({ connected: false, message: '' })

let refreshInterval: ReturnType<typeof setInterval>

const fetchStatus = async () => {
  try {
    plcStatus.value = await plcApi.getPlcStatus()
  } catch {
    plcStatus.value = { connected: false, message: '连接失败' }
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
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px;
}

.hero-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  padding: 50px 30px;
  text-align: center;
  margin-bottom: 30px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
}

.hero-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.hero-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
}

.hero-section h1 {
  font-size: 2.5rem;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
}

.subtitle {
  font-size: 1.1rem;
  color: #6b7280;
  margin: 0;
}

.stats-overview {
  display: flex;
  justify-content: center;
  gap: 80px;
  margin-bottom: 30px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.stat-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 15px rgba(239, 68, 68, 0.3);
  transition: all 0.3s ease;
}

.stat-circle.active {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  box-shadow: 0 4px 15px rgba(16, 185, 129, 0.3);
}

.stat-number {
  font-size: 1.1rem;
  font-weight: 700;
  color: #dc2626;
}

.stat-circle.active .stat-number {
  color: #059669;
}

.stat-label {
  font-size: 1rem;
  color: #4b5563;
  font-weight: 500;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 25px;
  margin-bottom: 30px;
}

.feature-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 25px;
  display: flex;
  align-items: center;
  gap: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.card-icon-wrapper {
  width: 65px;
  height: 65px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.8rem;
}

.card-icon-wrapper.plc {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
}

.card-content {
  flex: 1;
}

.card-content h3 {
  font-size: 1.3rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.card-content p {
  font-size: 0.9rem;
  color: #6b7280;
  margin: 0;
  line-height: 1.5;
}

.card-arrow {
  width: 40px;
  height: 40px;
  background: #f3f4f6;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  color: #6b7280;
  transition: all 0.3s ease;
}

.feature-card:hover .card-arrow {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  transform: translateX(5px);
}

.info-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 25px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 1.3rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 20px 0;
  padding-left: 10px;
  border-left: 4px solid #667eea;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}

.info-card {
  background: #f9fafb;
  border-radius: 16px;
  padding: 22px;
  transition: transform 0.3s ease;
}

.info-card:hover {
  transform: translateY(-3px);
}

.info-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
}

.info-icon {
  font-size: 1.3rem;
}

.info-title {
  font-weight: 600;
  color: #1f2937;
  font-size: 1.05rem;
}

.info-status {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ef4444;
  animation: pulse 2s infinite;
}

.info-status.success .status-indicator {
  background: #10b981;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.info-status span:last-child {
  font-weight: 600;
  color: #dc2626;
  font-size: 0.95rem;
}

.info-status.success span:last-child {
  color: #059669;
}

.info-desc {
  font-size: 0.85rem;
  color: #6b7280;
  margin: 0;
}
</style>