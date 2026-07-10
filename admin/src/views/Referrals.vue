<template>
  <div class="page">
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><Share /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">推荐裂变</h2>
          <div class="page-sub">{{ refSlogan }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-input v-model="query.memberId" placeholder="按推荐人 id 筛选" clearable style="width: 180px" @keyup.enter="onSearch" />
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="RefreshRight" @click="loadList">刷新</el-button>
      </div>
    </div>

    <div class="x-card table-wrap">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="推荐人" min-width="180">
          <template #default="{ row }">
            <div class="ref-name">{{ row.referrerName || '—' }}</div>
            <div class="ref-sub">id #{{ row.referrerId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="被推荐人" min-width="180">
          <template #default="{ row }">
            <div class="ref-name">{{ row.refereeName || '—' }}</div>
            <div class="ref-sub">id #{{ row.refereeId }} · {{ row.refereePhone || '' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="推荐码" width="140">
          <template #default="{ row }">
            <el-tag effect="plain" type="info">{{ row.code || '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="奖励" width="140" align="right">
          <template #default="{ row }">
            <span v-if="row.rewardAmount">{{ row.rewardType === 'COUPON' ? '券' : '余额' }} ¥{{ formatMoney(row.rewardAmount) }}</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="绑定时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadList"
          @size-change="loadList"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="推荐关系详情" size="500px">
      <div v-if="detail" class="ref-detail">
        <div class="rd-row"><span class="rd-lbl">推荐人</span><span>{{ detail.referrerName || '—' }} (id #{{ detail.referrerId }})</span></div>
        <div class="rd-row"><span class="rd-lbl">被推荐人</span><span>{{ detail.refereeName || '—' }} (id #{{ detail.refereeId }})</span></div>
        <div class="rd-row"><span class="rd-lbl">手机号</span><span>{{ detail.refereePhone || '—' }}</span></div>
        <div class="rd-row"><span class="rd-lbl">推荐码</span><span>{{ detail.code || '—' }}</span></div>
        <div class="rd-row"><span class="rd-lbl">状态</span><span>{{ statusText(detail.status) }}</span></div>
        <div class="rd-row"><span class="rd-lbl">奖励</span>
          <span v-if="detail.rewardAmount">{{ detail.rewardType }} · ¥{{ formatMoney(detail.rewardAmount) }} · 关联 id #{{ detail.rewardId }}</span>
          <span v-else class="muted">未发放</span>
        </div>
        <div class="rd-row"><span class="rd-lbl">绑定时间</span><span>{{ formatDateTime(detail.createdAt) }}</span></div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight, Share } from '@element-plus/icons-vue'
import { referralsApi } from '@/api'
import { formatDateTime, formatMoney } from '@/utils/format'

const refSlogan = [
  '一个人愿意把朋友带来，本身就是赞美',
  '把推荐当成问候，把裂变写成人情',
  '所有看得见的增长，背后是看不见的用心'
][Math.floor(Math.random() * 3)]

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ memberId: '' as any, page: 1, size: 20 })
const detailVisible = ref(false)
const detail = ref<any>(null)

async function loadList() {
  loading.value = true
  try {
    const params: any = {
      page: query.page,
      size: query.size,
      memberId: query.memberId ? Number(query.memberId) : undefined
    }
    const res: any = await referralsApi.adminAll(params)
    list.value = res?.list || []
    total.value = res?.total || 0
  } catch {/* */}
  finally { loading.value = false }
}

function onSearch() { query.page = 1; loadList() }

function statusText(s: string) {
  return ({ REGISTERED: '已注册', ACTIVE: '已活跃', REWARDED: '已奖励' } as any)[s] || s || '—'
}
function statusTagType(s: string) {
  return ({ REGISTERED: 'info', ACTIVE: 'warning', REWARDED: 'success' } as any)[s] || 'info'
}
function openDetail(row: any) {
  detail.value = row
  detailVisible.value = true
}

onMounted(loadList)
</script>

<style scoped>
.ref-name { font-size: 13.5px; color: var(--ink); font-weight: 500; }
.ref-sub { font-size: 11.5px; color: var(--muted); margin-top: 2px; }
.muted { color: var(--muted); }
.ref-detail { display: flex; flex-direction: column; gap: 10px; }
.rd-row { display: flex; font-size: 13px; }
.rd-lbl { width: 84px; color: var(--muted); flex-shrink: 0; }
</style>
