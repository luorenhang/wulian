<template>
  <div class="mqtt-container">
    <div class="header-section">
      <h2>MQTT消息监控</h2>
      <div class="status-bar">
        <span class="status-text">连接状态:</span>
        <span class="status-badge" :class="{ connected: status.connected }">
          {{ status.connected ? '已连接' : '未连接' }}
        </span>
        <button class="refresh-btn" @click="refreshMessages">🔄 刷新</button>
      </div>
    </div>

    <div class="messages-section">
      <h3>消息列表</h3>
      <div class="messages-list">
        <div class="message-item" v-for="msg in messages" :key="msg.id">
          <div class="message-header">
            <span class="topic">{{ msg.topic }}</span>
            <span class="qos">QoS: {{ msg.qos }}</span>
          </div>
          <div class="message-payload">{{ msg.payload }}</div>
          <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
        </div>
      </div>

      <div v-if="messages.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p>暂无MQTT消息</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { mqttApi, type MqttMessage } from '@/api'

const messages = ref<MqttMessage[]>([])
const status = ref({ connected: false, message: '' })

let refreshInterval: ReturnType<typeof setInterval>

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

.messages-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.messages-section h3 {
  color: #1e3a5f;
  margin-bottom: 15px;
}

.messages-list {
  max-height: 400px;
  overflow-y: auto;
}

.message-item {
  padding: 15px;
  border-bottom: 1px solid #f3f4f6;
}

.message-item:last-child {
  border-bottom: none;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.topic {
  font-weight: 600;
  color: #3b82f6;
}

.qos {
  font-size: 0.8rem;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
}

.message-payload {
  color: #1e3a5f;
  font-family: monospace;
  word-break: break-all;
  margin-bottom: 8px;
}

.message-time {
  font-size: 0.8rem;
  color: #9ca3af;
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
}
</style>
