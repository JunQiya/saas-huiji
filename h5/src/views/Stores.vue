<template>
  <div class="page stores">
    <NavBar title="附近门店" back />

    <div class="page-padding">
      <div class="page-tip">把你最常去的那家，放在心里最稳的位置。</div>

      <div v-if="loading" class="empty-box"><van-loading color="#6f94b8" /></div>
      <EmptyState v-else-if="!list.length" title="暂无可显示的门店" sub="门店正陆续上线中" art="leaf" />

      <div v-else class="store-list">
        <div v-for="s in list" :key="s.id" class="store-card ui-card hoverable">
          <div class="store-head">
            <div class="store-cover" :class="`tone-${s.status || 'OPEN'}`">
              <van-icon name="shop-o" size="22" />
            </div>
            <div class="store-meta">
              <div class="store-name">{{ s.name }}</div>
              <div class="store-status" :class="`s-${s.status || 'OPEN'}`">
                <span class="dot"></span>{{ statusText(s.status) }}
              </div>
            </div>
          </div>
          <div class="store-info">
            <div class="info-row">
              <van-icon name="location-o" />
              <span>{{ s.address || '-' }}</span>
            </div>
            <div class="info-row">
              <van-icon name="clock-o" />
              <span>{{ s.businessHours || '营业中 09:00 - 21:00' }}</span>
            </div>
            <div v-if="s.phone" class="info-row">
              <van-icon name="phone-o" />
              <span>{{ s.phone }}</span>
            </div>
          </div>
          <div class="store-foot">
            <a v-if="s.phone" :href="`tel:${s.phone}`" class="action-link">
              <van-icon name="phone-o" /> 拨号
            </a>
            <span v-if="s.distance" class="distance">{{ formatDist(s.distance) }}</span>
            <span v-else class="distance">—</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { h5Api, type Store } from '@/api/h5'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(false)
const list = ref<Store[]>([])

async function load() {
  loading.value = true
  try {
    list.value = await h5Api.stores()
    calcDistances()
  } catch (e: any) {
    showToast(e?.message || '加载门店失败')
  }
  finally { loading.value = false }
}

// haversine 公式计算两点间距离（米）
function haversine(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371000
  const toRad = (d: number) => d * Math.PI / 180
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2
  return Math.round(2 * R * Math.asin(Math.sqrt(a)))
}

// 获取用户定位后为每个门店计算距离
function calcDistances() {
  if (!navigator.geolocation) return
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const { latitude: ulat, longitude: ulng } = pos.coords
      list.value = list.value.map(s => {
        if (s.latitude != null && s.longitude != null) {
          return { ...s, distance: haversine(ulat, ulng, s.latitude, s.longitude) }
        }
        return s
      })
    },
    () => {/* 用户拒绝或获取失败，静默处理 */},
    { enableHighAccuracy: false, timeout: 5000, maximumAge: 60000 }
  )
}

function statusText(s?: string) {
  return ({ OPEN: '营业中', CLOSED: '休息中', RENOVATING: '装修中' } as any)[s || 'OPEN'] || '营业中'
}
function formatDist(m: number) {
  if (m < 1000) return `${m.toFixed(0)} m`
  return `${(m / 1000).toFixed(1)} km`
}

onMounted(load)
onActivated(load)
</script>

<style scoped>
.page-tip {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 0.04em;
  margin-bottom: 14px;
  font-family: 'Songti SC', serif;
  opacity: 0.85;
  padding-left: 2px;
}

.store-list { display: flex; flex-direction: column; gap: 12px; }
.store-card { padding: 14px 16px 12px; }
.store-head { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.store-cover {
  width: 40px; height: 40px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.store-cover.tone-OPEN { background: var(--brand); }
.store-cover.tone-CLOSED { background: var(--muted); }
.store-cover.tone-RENOVATING { background: var(--accent-clay); }
.store-meta { flex: 1; min-width: 0; }
.store-name {
  font-size: 14.5px; font-weight: 600; color: var(--ink);
  letter-spacing: 0.02em;
}
.store-status {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 10.5px;
  margin-top: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--success-soft); color: var(--success-deep);
  letter-spacing: 0.04em;
}
.store-status .dot { width: 5px; height: 5px; border-radius: 50%; background: var(--success); }
.store-status.s-CLOSED { background: var(--surface-3); color: var(--muted); }
.store-status.s-CLOSED .dot { background: var(--muted); }
.store-status.s-RENOVATING { background: var(--warning-soft); color: var(--warning-deep); }
.store-status.s-RENOVATING .dot { background: var(--warning); }

.store-info { padding: 10px 0; border-top: 1px dashed var(--line); }
.info-row {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: var(--ink-2);
  line-height: 1.6;
}
.info-row .van-icon { color: var(--muted); font-size: 13px; flex-shrink: 0; }
.store-foot {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
}
.action-link {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 12px;
  color: var(--brand-deep);
  padding: 3px 10px;
  background: var(--brand-soft);
  border-radius: 999px;
  letter-spacing: 0.04em;
  transition: opacity var(--dur) var(--ease);
}
.distance {
  font-size: 11.5px;
  color: var(--muted);
  letter-spacing: 0.04em;
  font-variant-numeric: tabular-nums;
}
</style>
