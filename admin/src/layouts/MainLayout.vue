<template>
  <el-container class="layout">
    <!-- 侧边栏 -->
    <el-aside :width="collapsed ? '64px' : '224px'" class="aside">
      <div class="aside-brand" :class="{ collapsed }">
        <div class="brand-mark">
          <span class="star"></span>
        </div>
        <div v-if="!collapsed" class="brand-text">
          <div class="brand-name">星河·会记</div>
          <div class="brand-sub">会员营销后台</div>
        </div>
      </div>

      <el-scrollbar class="aside-scroll">
        <el-menu
          :default-active="route.path"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="transparent"
          text-color="var(--ink-2)"
          active-text-color="var(--primary-action)"
          router
        >
          <template v-for="group in menuGroups" :key="group.title">
            <div v-if="!collapsed" class="menu-group-title">{{ group.title }}</div>
            <div v-else class="menu-group-divider"></div>
            <el-menu-item v-for="m in group.items" :key="m.path" :index="m.path">
              <el-icon><component :is="m.icon" /></el-icon>
              <template #title>{{ m.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="main-container">
      <!-- 顶栏 -->
      <el-header class="header" height="52px">
        <div class="header-left">
          <el-button text class="collapse-btn" @click="collapsed = !collapsed">
            <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/" class="crumbs">
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
  ArrowDown, Expand, Fold, Moon, SwitchButton, Shop, Setting
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { settingsPlanApi, storesApi } from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)
const isDark = ref(false)

// 菜单分组
const menuGroups = [
  {
    title: '经营总览',
    items: [
      { path: '/dashboard', title: '仪表盘', icon: 'DataLine' },
      { path: '/pos', title: '收银台', icon: 'Cashier' },
      { path: '/products', title: '商品服务', icon: 'Goods' },
      { path: '/orders', title: '订单流水', icon: 'List' }
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
      { path: '/marketing-calendar', title: '营销日历', icon: 'Calendar' }
    ]
  },
  {
    title: '组织',
    items: [
      { path: '/stores', title: '门店管理', icon: 'Shop' },
      { path: '/employees', title: '员工权限', icon: 'UserFilled' },
      { path: '/audit', title: '操作审计', icon: 'Document' }
    ]
  },
  {
    title: '系统',
    items: [
      { path: '/messages', title: '消息中心', icon: 'ChatLineRound' },
      { path: '/reports', title: '报表中心', icon: 'DataAnalysis' },
      { path: '/settings', title: '系统设置', icon: 'Setting' }
    ]
  }
]

// 顶栏面包屑
const crumbs = computed(() => {
  const map: Record<string, string> = {
    '/dashboard': '仪表盘',
    '/members': '会员管理',
    '/wallet': '储值流水',
    '/coupons': '优惠券',
    '/campaigns': '营销活动',
    '/pos': '收银台',
    '/products': '商品服务',
    '/orders': '订单流水',
    '/marketing-calendar': '营销日历',
    '/stores': '门店管理',
    '/employees': '员工权限',
    '/audit': '操作审计',
    '/settings': '系统设置',
    '/messages': '消息中心',
    '/reports': '报表中心',
    '/referrals': '推荐裂变'
  }
  return [map[route.path] || ''].filter(Boolean)
})

const avatarText = computed(() => {
  const name = userStore.user?.username || 'A'
  return name.charAt(0).toUpperCase()
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
  // 简单刷新当前页
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
    } catch {/* 取消 */}
  }
}

onMounted(() => {
  loadStore()
  // 从 store 恢复折叠态
  const saved = localStorage.getItem('aside-collapsed')
  if (saved) collapsed.value = saved === '1'
})
</script>

<style scoped>
.layout { height: 100vh; }

.aside {
  background: var(--card-bg);
  border-right: 1px solid var(--line);
  transition: width 0.2s ease-out;
  display: flex; flex-direction: column;
}
.aside-brand {
  height: 56px;
  display: flex; align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid var(--line);
}
.aside-brand.collapsed { justify-content: center; padding: 0; }
.brand-mark {
  width: 32px; height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #6f94b8 0%, #4a6a87 100%);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.star {
  width: 14px; height: 14px;
  background: #fff;
  clip-path: polygon(50% 0, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%);
}
.brand-text { line-height: 1.2; }
.brand-name { font-size: 14px; font-weight: 600; color: var(--ink); }
.brand-sub { font-size: 11px; color: var(--muted); }

.aside-scroll { flex: 1; padding: 8px 0; }
.aside :deep(.el-menu) { border-right: none; }

.menu-group-title {
  font-size: 11px;
  color: var(--muted-2);
  letter-spacing: 1px;
  padding: 12px 20px 4px;
  font-weight: 500;
}
.menu-group-divider {
  height: 1px;
  background: var(--line);
  margin: 10px 12px;
}

.aside :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  border-radius: 8px;
  margin: 2px 8px;
  padding-left: 16px !important;
  position: relative;
  font-size: 13.5px;
}
.aside :deep(.el-menu-item:hover) {
  background: rgba(111, 148, 184, 0.06) !important;
  color: var(--primary-action) !important;
}
.aside :deep(.el-menu-item.is-active) {
  background: var(--primary-action-soft) !important;
  color: var(--primary-action) !important;
  font-weight: 500;
}
.aside :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0; top: 50%;
  transform: translateY(-50%);
  width: 3px; height: 18px;
  background: var(--primary-action);
  border-radius: 0 2px 2px 0;
}
.aside :deep(.el-menu--collapse) .el-menu-item {
  margin: 4px 8px;
  padding-left: 0 !important;
  display: flex; justify-content: center;
}

/* 顶栏 */
.header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 22px;
  background: var(--card-bg);
  border-bottom: 1px solid var(--line);
  height: 52px;
}
.header-left { display: flex; align-items: center; gap: 12px; }
.collapse-btn { font-size: 16px; }
.crumbs :deep(.el-breadcrumb__item) { font-size: 13px; }
.crumbs :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) { color: var(--ink); font-weight: 500; }
.crumbs :deep(.el-breadcrumb__item:not(:last-child) .el-breadcrumb__inner) { color: var(--muted); }

.header-right { display: flex; align-items: center; gap: 4px; }
.icon-btn { font-size: 16px; padding: 8px; color: var(--ink-2); }
.store-picker { margin-right: 4px; }
.store-info { display: flex; align-items: center; gap: 4px; cursor: pointer; padding: 4px 10px; border-radius: 6px; font-size: 13px; color: var(--ink-2); transition: background 0.2s ease-out; }
.store-info:hover { background: rgba(111, 148, 184, 0.06); color: var(--primary-action); }
.store-name { max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }


.user-info {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s ease-out;
}
.user-info:hover { background: rgba(111, 148, 184, 0.06); }
.avatar { background: var(--primary-action); color: #fff; font-size: 12px; font-weight: 600; }
.username { font-size: 13px; color: var(--ink); }

/* 主体淡入 */
.main { padding: 0; background: transparent; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.18s ease-out; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 折叠态 - 标题 tooltip */
.aside :deep(.el-menu--collapse) .el-menu-item [class^='el-icon'] {
  margin-right: 0;
}
</style>
