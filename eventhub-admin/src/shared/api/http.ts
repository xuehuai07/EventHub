import { client } from './generated/client.gen'

client.setConfig({
  baseURL: import.meta.env.VITE_API_ORIGIN || '',
  timeout: 10_000,
  withCredentials: true,
})

client.instance.interceptors.request.use((config) => {
  config.headers.set('X-Request-Id', crypto.randomUUID())
  return config
})

export { client as http }
