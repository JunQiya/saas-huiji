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
        <el-button type="primary" :icon="Search" @click="onSearch" class="btn-scale">查询</el-button>
        <el-button :icon="RefreshRight" @click="loadList">刷新</el-button>
        <el-button :icon="Download" @click="onExport" :disabled="!list.length" class="btn-scale">导出</el-button>
        <el-button type="primary" :icon="Link" @click="openBindDialog" class="btn-scale">手动绑定</el-button>
      </div>
    </div>

    <!-- KPI 卡片区：因后端 adminStats 必传 memberId，无法取全局统计，这里基于列表数据汇总 -->
    <div class="x-card kpi-wrap">
      <div class="kpi-strip">
        <div class="kpi-item">
          <div class="k">总推荐数</div>
          <div class="v val">{{ total }}</div>
        </div>
        <div class="kpi-item">
          <div class="k">本月新增</div>
          <div class="v val">{{ monthCount }}</div>
        </div>
        <div class="kpi-item">
          <div class="k">成功绑定</div>
          <div class="v val">{{ boundCount }}</div>
        </div>
        <div class="kpi-item">
          <div class="k">已发奖励</div>
          <div class="v val">{{ rewardedCount }}</div>
        </div>
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

    <!-- 手动绑定弹窗 -->
    <el-dialog v-model="bindVisible" title="手动绑定推荐关系" width="420px">
      <el-form @submit.prevent="confirmBind">
        <el-form-item label="推荐码">
          <el-input v-model="bindCode" placeholder="请输入推荐码" clearable autofocus />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindLoading" @click="confirmBind" class="btn-scale">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight, Share, Download, Link } from '@element-plus/icons-vue'
import { referralsApi } from '@/api'
import { formatDateTime, formatMoney } from '@/utils/format'
import { exportCsv } from '@/utils/csv'

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

// 绑定弹窗状态
const bindVisible = ref(false)
const bindCode = ref('')
const bindLoading = ref(false)

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

// 打开绑定弹窗
function openBindDialog() {
  bindCode.value = ''
  bindVisible.value = true
}

// 确认绑定：调后端 adminBind
async function confirmBind() {
  const code = bindCode.value.trim()
  if (!code) {
    ElMessage.warning('请输入推荐码')
    return
  }
  bindLoading.value = true
  try {
    await referralsApi.adminBind(code)
    ElMessage.success('绑定成功')
    bindVisible.value = false
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '绑定失败')
  } finally {
    bindLoading.value = false
  }
}

// KPI 汇总：基于当前列表数据（后端 adminStats 必传 memberId，无法取全局统计）
const monthCount = computed(() => {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth()
  return list.value.filter(r => {
    if (!r.createdAt) return false
    const d = new Date(r.createdAt)
    return d.getFullYear() === y && d.getMonth() === m
  }).length
})
const boundCount = computed(() => {
  // 已活跃 + 已奖励 视为成功绑定
  return list.value.filter(r => r.status === 'ACTIVE' || r.status === 'REWARDED').length
})
const rewardedCount = computed(() => {
  return list.value.filter(r => r.status === 'REWARDED').length
})

// 导出当前列表为 CSV
function onExport() {
  if (!list.value.length) return
  exportCsv(`推荐关系-${new Date().toLocaleDateString('zh-CN')}`, list.value, [
    { key: 'referrerName', header: '推荐人', format: r => `${r.referrerName || ''} (#${r.referrerId ?? ''})` },
    { key: 'refereeName', header: '被推荐人', format: r => `${r.refereeName || ''} (#${r.refereeId ?? ''})` },
    { key: 'refereePhone', header: '被推荐人手机号' },
    { key: 'code', header: '推荐码' },
    { key: 'status', header: '状态', format: r => statusText(r.status) },
    { key: 'rewardType', header: '奖励类型' },
    { key: 'rewardAmount', header: '奖励金额(元)', format: r => r.rewardAmount ? Number(formatMoney(r.rewardAmount).replace(/,/g, '')) : '' },
    { key: 'createdAt', header: '绑定时间', format: r => formatDateTime(r.createdAt) }
  ])
  ElMessage.success(`已导出 ${list.value.length} 条记录`)
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
.kpi-wrap { padding: 16px 20px; margin-bottom: 16px; }
.kpi-strip { display: flex; gap: 32px; }
.kpi-item { display: flex; flex-direction: column; gap: 4px; }
.kpi-item .k { color: var(--muted); font-size: 12px; letter-spacing: 0.04em; }
.kpi-item .v { font-size: 22px; color: var(--ink); font-weight: 500; }
</style>
