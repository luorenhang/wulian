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
  }
}

export default api
