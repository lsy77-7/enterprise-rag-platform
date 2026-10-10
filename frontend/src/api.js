import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 60000,
  withCredentials: true
})

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message =
      error.response?.data?.message ||
      error.message ||
      '请求失败，请检查后端服务是否已启动'

    return Promise.reject(new Error(message))
  }
)

export default api
