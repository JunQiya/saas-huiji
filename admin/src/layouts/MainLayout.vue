<template>
  <el-container class="layout">
    <!-- 侧边栏 -->
    <el-aside :width="collapsed ? '64px' : '224px'" class="aside">
      <div class="aside-brand" :class="{ collapsed }">
        <div class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 36 36" width="32" height="32">
            <circle cx="9" cy="9" r="0.8" fill="currentColor" opacity="0.4" />
            <circle cx="27" cy="6" r="0.7" fill="currentColor" opacity="0.32" />
            <circle cx="30" cy="22" r="0.8" fill="currentColor" opacity="0.36" />
            <circle cx="7" cy="28" r="0.7" fill="currentColor" opacity="0.28" />
            <g stroke="currentColor" stroke-width="0.7" opacity="0.5" fill="none">
              <line x1="13" y1="14" x2="18" y2="20" />
              <line x1="18" y1="20" x2="23" y2="14" />
              <line x1="18" y1="20" x2="18" y2="26" />
            </g>
            <circle cx="13" cy="14" r="1.6" fill="currentColor" />
            <circle cx="23" cy="14" r="1.6" fill="currentColor" />
            <circle cx="18" cy="20" r="2.2" fill="currentColor" />
            <circle cx="18" cy="26" r="1.2" fill="currentColor" opacity="0.7" />
          </svg>
        </div>
        <div v-if="!collapsed" class="brand-text">
          <div class="brand-name">星河·会记</div>
          <div class="brand-sub">经营后台 · 夜读手记</div>
        </div>
      </div>

      <el-scrollbar class="aside-scroll">
        <el-menu
          :default-active="route.path"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="transparent"
          text-color="var(--ink-2)"
          active-text-color="var(--brand-ink)"
          router
        >
          <template v-for="group in menuGroups" :key="group.title">
            <div v-if="!collapsed" class="menu-group-title">
              <span class="mg-dot"></span>
              <span>{{ group.title }}</span>
            </div>
            <div v-else class="menu-group-divider"></div>
            <el-menu-item v-for="m in group.items" :key="m.path" :index="m.path">
              <el-icon><component :is="m.icon" /></el-icon>
              <template #title>{{ m.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>

      <div v-if="!collapsed" class="aside-foot">
        <span class="af-quote">{{ asideQuote }}</span>
        <span class="af-version">v{{ version }}</span>
      </div>
    </el-aside>

    <el-container class="main-container">
      <!-- 顶栏 -->
      <el-header class="header" height="56px">
        <div class="header-left">
          <el-button text class="collapse-btn" @click="collapsed = !collapsed">
            <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator=" / " class="crumbs">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="(c, i) in crumbs" :key="i">{{ c }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="onStoreCmd" trigger="click" class="store-picker">
            <span class="store-info">
              <el-icon><Shop /></el-icon>
              <span class="store-name">{{ currentStore.name || '选择门店' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="s in stores" :key="s.id" :command="s.id" :disabled="s.id === currentStore.storeId">
                  {{ s.name }}
                </el-dropdown-item>
                <el-dropdown-item divided command="manage">
                  <el-icon><Setting /></el-icon> 门店管理
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-tooltip content="暗色模式（即将开放）" placement="bottom">
            <el-button text class="icon-btn" @click="toggleDark">
              <el-icon><Moon /></el-icon>
            </el-button>
          </el-tooltip>
          <el-dropdown @command="onUserCmd">
            <span class="user-info">
              <el-avatar :size="28" class="avatar">{{ avatarText }}</el-avatar>
              <span class="username">{{ userStore.user?.username || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown, Expand, Fold, Moon, SwitchButton, Shop, Setting, QuestionFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { settingsPlanApi, storesApi } from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)
const isDark = ref(false)

const menuGroups = [
  {
    title: '经营总览',
    items: [
      { path: '/dashboard', title: '仪表盘', icon: 'DataLine' },
      { path: '/pos', title: '收银台', icon: 'Cashier' },
      { path: '/products', title: '商品服务', icon: 'Goods' },
      { path: '/orders', title: '订单流水', icon: 'List' },
      { path: '/dining-tables', title: '桌台管理', icon: 'Grid' },
      { path: '/kitchen-orders', title: '厨房工单', icon: 'Food' },
      { path: '/mall-admin', title: '线上商城', icon: 'Shop' }
    ]
  },
  {
    title: '会员与储值',
    items: [
      { path: '/members', title: '会员管理', icon: 'User' },
      { path: '/wallet', title: '储值流水', icon: 'Wallet' },
      { path: '/referrals', title: '推荐裂变', icon: 'Share' }
    ]
  },
  {
    title: '营销',
    items: [
      { path: '/coupons', title: '优惠券', icon: 'Ticket' },
      { path: '/campaigns', title: '营销活动', icon: 'Promotion' },
      { path: '/games', title: '赢奖小游戏', icon: 'Trophy' },
      { path: '/marketing-calendar', title: '营销日历', icon: 'Calendar' }
    ]
  },
  {
    title: '组织',
    items: [
      { path: '/stores', title: '门店管理', icon: 'Shop' },
      { path: '/employees', title: '员工权限', icon: 'UserFilled' },
      { path: '/agents', title: '代理商', icon: 'Connection' },
      { path: '/audit', title: '操作审计', icon: 'Document' }
    ]
  },
  {
    title: '系统',
    items: [
      { path: '/messages', title: '消息中心', icon: 'ChatLineRound' },
      { path: '/reports', title: '报表中心', icon: 'DataAnalysis' },
      { path: '/wx-account', title: '微信公众号', icon: 'ChatDotRound' },
      { path: '/settings', title: '系统设置', icon: 'Setting' }
    ]
  }
]

const crumbs = computed(() => {
  const map: Record<string, string> = {
    '/dashboard': '仪表盘',
    '/members': '会员管理',
    '/wallet': '储值流水',
    '/coupons': '优惠券',
    '/campaigns': '营销活动',
    '/games': '赢奖小游戏',
    '/pos': '收银台',
    '/products': '商品服务',
    '/orders': '订单流水',
    '/dining-tables': '桌台管理',
    '/kitchen-orders': '厨房工单',
    '/mall-admin': '线上商城',
    '/marketing-calendar': '营销日历',
    '/stores': '门店管理',
    '/employees': '员工权限',
    '/audit': '操作审计',
    '/settings': '系统设置',
    '/messages': '消息中心',
    '/reports': '报表中心',
    '/referrals': '推荐裂变',
    '/agents': '代理商',
    '/wx-account': '微信公众号'
  }
  return [map[route.path] || ''].filter(Boolean)
})

const avatarText = computed(() => {
  const name = userStore.user?.username || 'A'
  return name.charAt(0).toUpperCase()
})

const asideQuote = computed(() => {
  const quotes = [
    '把数字读成故事',
    '把到店谱成日常',
    '用心的服务会被记住',
    '慢一点的生意也走得远'
  ]
  return quotes[Math.floor(Math.random() * quotes.length)]
})

function toggleDark() {
  isDark.value = !isDark.value
  document.body.classList.toggle('theme-dark-mock', isDark.value)
  ElMessage.info(isDark.value ? '已开启暗色占位（完整暗色模式即将上线）' : '已关闭暗色')
}

const currentStore = reactive<any>({ storeId: null, name: '' })
const stores = ref<any[]>([])
async function loadStore() {
  try {
    const cur: any = await settingsPlanApi.currentStore()
    if (cur) {
      currentStore.storeId = cur.storeId
      currentStore.name = cur.name || ''
    }
  } catch {}
  try { stores.value = await storesApi.list() || [] } catch {}
}
async function onStoreCmd(cmd: any) {
  if (cmd === 'manage') { router.push('/stores'); return }
  await settingsPlanApi.switchStore(Number(cmd))
  ElMessage.success('已切换门店')
  await loadStore()
  router.replace('/dashboard').then(() => router.go(0))
}

async function onUserCmd(cmd: string) {
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        type: 'warning',
        closeOnPressEscape: true,
        confirmButtonText: '退出',
        cancelButtonText: '再看看'
      })
      userStore.logout()
      ElMessage.success('已安全退出')
      router.replace('/login')
    } catch {}
  }
}

function onHelp() {
  ElMessageBox.alert(
    '使用要点：\n\n1. 收银台：会员手机号可快速核销券、扣储值\n2. 会员管理：支持等级筛选、批量导出、详情 RFM 分析\n3. 优惠券：可批量导入 / 导出 CSV\n4. 报表中心：实时图表 + 报表下载\n5. 订单/储值：均支持导出 CSV\n\n需要更多帮助？联系客服 xinghe@mail.lxxno.cn',
    '使用说明 · 星河·会记',
    { confirmButtonText: '知道了', dangerouslyUseHTMLString: false, customClass: 'help-dialog' }
  ).catch(() => {})
}

// 版本号
const version = '1.0.0'

// 首次登录欢迎（只弹一次）
function onFirstLogin() {
  const key = 'huiji-welcomed'
  if (localStorage.getItem(key)) return
  localStorage.setItem(key, '1')
  setTimeout(() => {
    ElMessage({
      message: '欢迎使用星河·会记 — 一份给会员管理的夜读手记',
      type: 'success',
      duration: 3000
    })
  }, 400)
}

onMounted(() => {
  loadStore()
  onFirstLogin()
  const saved = localStorage.getItem('aside-collapsed')
  if (saved) collapsed.value = saved === '1'
})
</script>

<style scoped>
.layout { height: 100vh; }

.aside {
  background: var(--surface);
  border-right: 1px solid var(--line);
  transition: width 0.2s var(--ease-out);
  display: flex; flex-direction: column;
}
.aside-brand {
  height: 60px;
  display: flex; align-items: center;
  gap: 11px;
  padding: 0 18px;
  border-bottom: 1px solid var(--line);
  position: relative;
}
.aside-brand::after {
  content: '';
  position: absolute;
  left: 18px; bottom: -1px;
  width: 24px; height: 1px;
  background: var(--brand);
  opacity: 0.7;
}
.aside-brand.collapsed { justify-content: center; padding: 0; }
.brand-mark {
  width: 32px; height: 32px;
  color: var(--brand-deep);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.brand-text { line-height: 1.2; }
.brand-name {
  font-family: var(--font-serif);
  font-size: 14.5px; font-weight: 500;
  color: var(--ink);
  letter-spacing: 0.08em;
}
.brand-sub {
  font-family: var(--font-serif);
  font-size: 10.5px; color: var(--muted);
  letter-spacing: 0.10em;
  margin-top: 2px;
}

.aside-scroll { flex: 1; padding: 8px 0; }
.aside :deep(.el-menu) { border-right: none; }

.menu-group-title {
  font-family: var(--font-serif);
  font-size: 11px;
  color: var(--muted-2);
  letter-spacing: 0.24em;
  padding: 14px 20px 4px;
  font-weight: 400;
  display: flex; align-items: center; gap: 6px;
}
.mg-dot {
  display: inline-block; width: 4px; height: 4px;
  border-radius: 50%;
  background: var(--brand);
  opacity: 0.5;
}
.menu-group-divider {
  height: 1px;
  background: var(--line);
  margin: 10px 12px;
}

.aside :deep(.el-menu-item) {
  height: 40px;
  line-height: 40px;
  border-radius: 6px;
  margin: 1px 8px;
  padding-left: 16px !important;
  position: relative;
  font-size: 13.5px;
  font-family: var(--font-serif);
  letter-spacing: 0.04em;
}
.aside :deep(.el-menu-item:hover) {
  background: var(--surface-2) !important;
  color: var(--ink) !important;
}
.aside :deep(.el-menu-item.is-active) {
  background: var(--brand-soft) !important;
  color: var(--brand-ink) !important;
  font-weight: 500;
}
.aside :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0; top: 50%;
  transform: translateY(-50%);
  width: 2px; height: 16px;
  background: var(--brand);
  border-radius: 0 2px 2px 0;
}
.aside :deep(.el-menu--collapse) .el-menu-item {
  margin: 2px 8px;
  padding-left: 0 !important;
  display: flex; justify-content: center;
}

.aside-foot {
  padding: 14px 18px 16px;
  border-top: 1px dashed var(--line-2);
}
.af-quote {
  font-family: var(--font-serif);
  font-size: 11px;
  color: var(--muted-2);
  letter-spacing: 0.16em;
  display: block;
  text-align: center;
}

/* 顶栏 */
.header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
  height: 56px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.collapse-btn { font-size: 16px; color: var(--ink-2); }
.crumbs :deep(.el-breadcrumb__item) { font-size: 13px; }
.crumbs :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--ink); font-weight: 500;
  font-family: var(--font-serif);
  letter-spacing: 0.04em;
}
.crumbs :deep(.el-breadcrumb__item:not(:last-child) .el-breadcrumb__inner) { color: var(--muted); }
.crumbs :deep(.el-breadcrumb__separator) { color: var(--muted-2); }

.header-right { display: flex; align-items: center; gap: 4px; }
.icon-btn { font-size: 16px; padding: 8px; color: var(--ink-2); }
.store-picker { margin-right: 4px; }
.store-info {
  display: flex; align-items: center; gap: 5px;
  cursor: pointer; padding: 5px 10px; border-radius: 6px;
  font-size: 13px; color: var(--ink-2);
  transition: background var(--dur) var(--ease-out);
}
.store-info:hover { background: var(--surface-2); color: var(--ink); }
.store-name {
  max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  font-family: var(--font-serif); letter-spacing: 0.04em;
}

.user-info {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; padding: 4px 8px 4px 4px;
  border-radius: 18px;
  border: 1px solid var(--line);
  transition: background var(--dur) var(--ease-out);
}
.user-info:hover { background: var(--surface-2); }
.avatar {
  background: var(--brand) !important; color: #fff !important;
  font-size: 12px; font-weight: 500;
  font-family: var(--font-num);
}
.username { font-size: 13px; color: var(--ink); font-family: var(--font-serif); }

.main { padding: 0; background: transparent; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.18s var(--ease-out); }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.aside :deep(.el-menu--collapse) .el-menu-item [class^='el-icon'] {
  margin-right: 0;
}
</style>
