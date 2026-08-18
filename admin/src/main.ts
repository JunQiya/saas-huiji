import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { useUserStore } from '@/stores/user'
import './style.css'

const app = createApp(App)
const pinia = createPinia()

// 注册全部 Element Plus 图标为全局组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// Element Plus 按需引入由 unplugin-vue-components 处理（见 vite.config），
// 全局默认尺寸配置见 App.vue 中的 <el-config-provider>
app.use(pinia)
app.use(router)
app.mount('#app')

// ===== 登录过期自动退出: 启动时检查 + 每 30 秒轮询 =====
function startSessionWatcher() {
  const check = () => {
    const store = useUserStore()
    // 已登录但 token 前端可判断已过期 -> 自动退出并跳登录
    if (store.token && store.tokenExpired) {
      store.forceLogout('登录已过期，请重新登录')
    }
  }
  // 立即检查一次
  check()
  // 每 30 秒轮询(页面可见时才检查)
  window.setInterval(() => {
    if (document.visibilityState === 'visible') check()
  }, 30000)
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') check()
  })
}
startSessionWatcher()
