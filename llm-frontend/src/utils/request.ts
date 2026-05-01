import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'

const request: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 60000
})

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    return config
  },
  (error: any) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error: any) => {
    const message = error.response?.data?.message || error.message || '请求失败'
    // TODO: 替换为 shadcn/ui toast 或 sonner
    console.error('Request Error:', message)
    return Promise.reject(error)
  }
)

export default request
