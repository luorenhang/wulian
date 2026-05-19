<template>
  <div class="mqtt-container">
    <div class="header-section">
      <div class="header-left">
        <h2 class="page-title">📡 MQTT消息监控</h2>
        <p class="page-subtitle">实时监控MQTT消息队列，追踪数据传输状态</p>
      </div>
      <div class="status-bar">
        <div class="status-indicator" :class="{ connected: status.connected }">
          <span class="status-dot"></span>
          <span class="status-text">{{ status.connected ? '已连接' : '未连接' }}</span>
        </div>
        <button class="btn btn-secondary" @click="refreshMessages">
          <span class="btn-icon">🔄</span>
          <span>刷新消息</span>
        </button>
      </div>
    </div>

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon msg-icon">
          <span>📨</span>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ messages.length }}</span>
          <span class="stat-label">消息数量</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon topic-icon">
          <span>📑</span>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ uniqueTopics }}</span>
          <span class="stat-label">主题数量</span>
        </div>
      </div>
    </div>

    <div class="section">
      <h3 class="section-title">
        <span class="section-icon">📧</span>
        消息列表
      </h3>
      <div class="messages-list" ref="messagesContainer">
        <div 
          class="message-card" 
          v-for="msg in messages" 
          :key="msg.id"
          @mouseenter="hoveredMessage = msg.id"
          @mouseleave="hoveredMessage = null"
        >
          <div class="message-header">
            <div class="topic-badge">
              <span class="topic-icon">📌</span>
              <span class="topic-text">{{ msg.topic }}</span>
            </div>
            <div class="message-meta">
              <span class="qos-badge">QoS: {{ msg.qos }}</span>
            </div>
          </div>
          <div class="message-body">
            <pre class="payload">{{ msg.payload }}</pre>
          </div>
          <div class="message-footer">
            <span class="timestamp">{{ formatTime(msg.timestamp) }}</span>
            <div class="actions" v-if="hoveredMessage === msg.id">
              <button class="action-btn" @click="copyMessage(msg.payload)">
                <span>📋</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="messages.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p>暂无MQTT消息</p>
        <p class="empty-hint">消息将在此处显示...</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { mqttApi, type MqttMessage } from '@/api'

const messages = ref<MqttMessage[]>([])
const status = ref({ connected: false, message: '' })
const hoveredMessage = ref<number | null>(null)

let refreshInterval: ReturnType<typeof setInterval>

const uniqueTopics = computed(() => {
  const topics = new Set(messages.value.map(msg => msg.topic))
  return topics.size
})

const fetchMessages = async () => {
  try {
    messages.value = await mqttApi.getMessages()
    status.value = await mqttApi.getStatus()
  } catch {
    status.value = { connected: false, message: '获取消息失败' }
  }
}

const refreshMessages = () => {
  fetchMessages()
}

const formatTime = (timestamp: string): string => {
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const copyMessage = async (payload: string) => {
  try {
    await navigator.clipboard.writeText(payload)
    alert('已复制到剪贴板')
  } catch {
    alert('复制失败')
  }
}

onMounted(() => {
  fetchMessages()
  refreshInterval = setInterval(fetchMessages, 5000)
})

onUnmounted(() => {
  clearInterval(refreshInterval)
})
</script>

<style scoped>
.mqtt-container {
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

.msg-icon {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
}

.topic-icon {
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

.messages-list {
  max-height: 500px;
  overflow-y: auto;
  padding-right: 10px;
}

.messages-list::-webkit-scrollbar {
  width: 6px;
}

.messages-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.messages-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.messages-list::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

.message-card {
  background: #f9fafb;
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 15px;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.message-card:hover {
  background: #f3f4f6;
  border-color: #e5e7eb;
  transform: translateX(5px);
}

.message-card:last-child {
  margin-bottom: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
}

.topic-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(102, 126, 234, 0.1);
  padding: 6px 14px;
  border-radius: 20px;
}

.topic-icon {
  font-size: 0.9rem;
}

.topic-text {
  font-weight: 600;
  color: #667eea;
  font-size: 0.95rem;
}

.message-meta {
  display: flex;
  gap: 10px;
}

.qos-badge {
  font-size: 0.8rem;
  color: #6b7280;
  background: #f3f4f6;
  padding: 4px 10px;
  border-radius: 8px;
}

.message-body {
  margin-bottom: 15px;
}

.payload {
  background: #ffffff;
  border-radius: 10px;
  padding: 15px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.9rem;
  color: #374151;
  line-height: 1.6;
  word-break: break-all;
  white-space: pre-wrap;
  margin: 0;
  border: 1px solid #e5e7eb;
}

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #e5e7eb;
}

.timestamp {
  font-size: 0.85rem;
  color: #9ca3af;
}

.actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: #f3f4f6;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  transition: all 0.3s ease;
}

.action-btn:hover {
  background: #667eea;
  color: white;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 20px;
}

.empty-state p {
  color: #9ca3af;
  font-size: 1.1rem;
  margin: 0 0 8px 0;
}

.empty-hint {
  font-size: 0.9rem !important;
}
</style>