import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export interface PlcData {
  id: number
  tagName: string
  value: number | string
  timestamp: string
  dataType: string
}

export interface ProductCountData {
  tagName: string
  value: number | string
  unit: string
  dataType: string
  timestamp: string
}

export interface MqttMessage {
  id: number
  topic: string
  payload: string
  timestamp: string
  qos: number
}

export const plcApi = {
  getPlcData: async (): Promise<PlcData[]> => {
    const response = await api.get('/plc/data')
    return response.data
  },
  getPlcStatus: async (): Promise<{ connected: boolean; message: string }> => {
    const response = await api.get('/plc/status')
    return response.data
  },
  connectPlc: async (): Promise<{ connected: boolean; message: string }> => {
    const response = await api.post('/plc/connect')
    return response.data
  },
  getProductCount: async (): Promise<ProductCountData[]> => {
    const response = await api.get('/plc/product-count')
    return response.data
  }
}

export const mqttApi = {
  getMessages: async (): Promise<MqttMessage[]> => {
    const response = await api.get('/mqtt/messages')
    return response.data
  },
  getStatus: async (): Promise<{ connected: boolean; message: string }> => {
    const response = await api.get('/mqtt/status')
    return response.data
  },
  publishMessage: async (topic: string, payload: string): Promise<void> => {
    await api.post('/mqtt/publish', { topic, payload })
  }
}

export default api
