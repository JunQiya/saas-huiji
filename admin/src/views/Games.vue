<template>
  <div class="page">
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><Trophy /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">赢奖小游戏</h2>
          <div class="page-sub">{{ slogan }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="RefreshRight" @click="loadList">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate" class="btn-scale">新建游戏</el-button>
      </div>
    </div>

    <div class="x-card filter-wrap">
      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="状态" clearable @change="loadList" style="width: 140px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-select v-model="query.type" placeholder="游戏类型" clearable @change="loadList" style="width: 160px">
          <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </div>
    </div>

    <!-- 游戏卡片列表 -->
    <div v-loading="loading" class="game-grid">
      <div v-for="g in list" :key="g.id" class="game-card x-card hoverable">
        <div class="game-head">
          <div class="game-cover" :class="`type-${g.type}`">
            <img v-if="g.coverImage" :src="g.coverImage" :alt="g.name" />
            <el-icon v-else><Trophy /></el-icon>
          </div>
          <div class="game-info">
            <div class="game-name">{{ g.name }}</div>
            <div class="game-type">{{ typeText(g.type) }}<span v-if="g.subtitle"> · {{ g.subtitle }}</span></div>
          </div>
          <el-switch
            :model-value="g.status === 'ENABLED'"
            @change="(v: any) => onToggleStatus(g, !!v)"
            inline-prompt active-text="启" inactive-text="停"
          />
        </div>
        <div class="game-body">
          <div class="game-row">
            <span class="lbl">周期</span>
            <span class="val">{{ formatDate(g.startTime) }} ~ {{ formatDate(g.endTime) }}</span>
          </div>
          <div class="game-row">
            <span class="lbl">每日</span>
            <span class="val">{{ g.dailyLimit ?? 0 }} 次 / 总 {{ g.totalLimit ?? 0 }} 次</span>
          </div>
          <div class="game-row">
            <span class="lbl">消耗</span>
            <span class="val">{{ g.pointsCost ?? 0 }} 积分 / 次</span>
          </div>
        </div>
        <div class="game-actions">
          <el-button size="small" link type="primary" @click="openEdit(g)">编辑</el-button>
          <el-button size="small" link type="primary" @click="openPrizes(g)">奖品配置</el-button>
          <el-button size="small" link type="primary" @click="openStats(g)">统计</el-button>
          <el-button size="small" link type="danger" @click="onRemove(g)">删除</el-button>
        </div>
      </div>
      <div v-if="!loading && !list.length" class="empty-state">
        <el-icon><Trophy /></el-icon>
        <span>暂无游戏，点击右上角新建</span>
      </div>
    </div>

    <!-- 新建/编辑游戏 -->
    <el-dialog v-model="formVisible" :title="editing ? '编辑游戏' : '新建游戏'" width="620px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="92px">
        <el-form-item label="游戏名称" prop="name">
          <el-input v-model="form.name" placeholder="如：周年庆大转盘" />
        </el-form-item>
        <el-form-item label="游戏类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio v-for="t in typeOptions" :key="t.value" :value="t.value">{{ t.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" placeholder="一句话描述，如：每日一抽，好运不断" />
        </el-form-item>
        <el-form-item label="封面图 URL">
          <el-input v-model="form.coverImage" placeholder="可选，留空使用默认图标" />
        </el-form-item>
        <el-form-item label="背景图 URL">
          <el-input v-model="form.bgImage" placeholder="可选，H5 游戏页背景图" />
        </el-form-item>
        <el-form-item label="活动周期">
          <el-date-picker v-model="form.range" type="datetimerange" range-separator="—" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每日次数" prop="dailyLimit">
          <el-input-number v-model="form.dailyLimit" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="总次数" prop="totalLimit">
          <el-input-number v-model="form.totalLimit" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="消耗积分">
          <el-input-number v-model="form.pointsCost" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="所属门店">
          <el-select v-model="form.storeId" placeholder="不选则全门店通用" clearable style="width: 100%">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则说明">
          <el-input v-model="form.rules" type="textarea" :rows="3" placeholder="如：1. 每日 3 次免费抽奖机会&#10;2. 中奖后优惠券将自动发放至账户" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.status" active-value="ENABLED" inactive-value="DISABLED" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 奖品配置 -->
    <el-dialog v-model="prizeVisible" :title="`奖品配置 · ${currentGame?.name || ''}`" width="820px" :close-on-click-modal="false">
      <div class="prize-toolbar">
        <div class="prize-tip">奖品概率之和建议等于 100%；未中奖用 EMPTY 类型表示</div>
        <el-button type="primary" size="small" :icon="Plus" @click="openPrizeCreate">添加奖品</el-button>
      </div>
      <el-table :data="prizeList" v-loading="prizeLoading" size="small" border>
        <el-table-column type="index" label="#" width="44" />
        <el-table-column label="奖品名称" prop="name" min-width="120" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="prizeTagType(row.type)">{{ prizeTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联" min-width="120">
          <template #default="{ row }">{{ row.refName || '—' }}</template>
        </el-table-column>
        <el-table-column label="概率" width="80">
          <template #default="{ row }"><span class="val">{{ row.probability ?? 0 }}%</span></template>
        </el-table-column>
        <el-table-column label="数量" width="80">
          <template #default="{ row }"><span class="val">{{ row.amount ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column label="排序" width="70">
          <template #default="{ row }"><span class="val">{{ row.sortOrder ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="120" class-name="row-actions">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openPrizeEdit(row)">编辑</el-button>
            <el-button size="small" link type="danger" @click="onRemovePrize(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="prize-summary">
        <span>共 {{ prizeList.length }} 项奖品</span>
        <span>概率合计：<span class="val" :class="probSumClass">{{ probSum }}%</span></span>
      </div>
      <template #footer>
        <el-button @click="prizeVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 奖品编辑 -->
    <el-dialog v-model="prizeFormVisible" :title="prizeEditing ? '编辑奖品' : '添加奖品'" width="500px" :close-on-click-modal="false" append-to-body>
      <el-form ref="prizeFormRef" :model="prizeForm" :rules="prizeFormRules" label-width="88px">
        <el-form-item label="奖品名称" prop="name">
          <el-input v-model="prizeForm.name" placeholder="如：10 元代金券" />
        </el-form-item>
        <el-form-item label="奖品类型" prop="type">
          <el-radio-group v-model="prizeForm.type">
            <el-radio value="COUPON">优惠券</el-radio>
            <el-radio value="POINTS">积分</el-radio>
            <el-radio value="EMPTY">未中奖</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="prizeForm.type === 'COUPON'" label="关联券 ID">
          <el-input v-model="prizeForm.refId" placeholder="优惠券模板 ID" />
        </el-form-item>
        <el-form-item v-if="prizeForm.type === 'COUPON'" label="关联名称">
          <el-input v-model="prizeForm.refName" placeholder="如：10 元代金券" />
        </el-form-item>
        <el-form-item v-if="prizeForm.type === 'POINTS'" label="积分数量">
          <el-input-number v-model="prizeForm.refId" :min="0" :step="10" controls-position="right" />
        </el-form-item>
        <el-form-item label="中奖概率" prop="probability">
          <el-input-number v-model="prizeForm.probability" :min="0" :max="100" :step="1" controls-position="right" />
          <span class="form-tip">0-100，单位 %</span>
        </el-form-item>
        <el-form-item label="奖品数量" prop="amount">
          <el-input-number v-model="prizeForm.amount" :min="0" :step="1" controls-position="right" />
          <span class="form-tip">0 表示不限</span>
        </el-form-item>
        <el-form-item label="图片 URL">
          <el-input v-model="prizeForm.imageUrl" placeholder="可选，奖品展示图" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="prizeForm.sortOrder" :min="0" :step="1" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="prizeFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="prizeSaving" @click="submitPrizeForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 统计弹窗 -->
    <el-dialog v-model="statsVisible" :title="`统计 · ${currentGame?.name || ''}`" width="560px">
      <div v-loading="statsLoading" class="stats-content">
        <div class="stats-cards">
          <div class="stat-item">
            <div class="stat-val val">{{ stats.totalPlays ?? 0 }}</div>
            <div class="stat-label">总参与人次</div>
          </div>
          <div class="stat-item">
            <div class="stat-val val">{{ stats.winPlays ?? 0 }}</div>
            <div class="stat-label">中奖人次</div>
          </div>
          <div class="stat-item">
            <div class="stat-val val">{{ winRate }}</div>
            <div class="stat-label">中奖率</div>
          </div>
        </div>
        <div class="section-title">各奖品发放量</div>
        <el-table :data="stats.prizes || []" size="small" border>
          <el-table-column label="奖品名称" prop="name" min-width="140" />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">{{ prizeTypeText(row.type) }}</template>
          </el-table-column>
          <el-table-column label="发放量" width="90">
            <template #default="{ row }"><span class="val">{{ row.granted ?? 0 }}</span></template>
          </el-table-column>
        </el-table>
        <div v-if="!stats.prizes?.length" class="empty-state" style="padding: 24px;">
          <span>暂无发放记录</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, RefreshRight, Trophy } from '@element-plus/icons-vue'
import { gameApi, storesApi } from '@/api'
import { formatDate } from '@/utils/format'

const slogan = [
  '让每一次互动，都成为一次温柔的回馈',
  '小惊喜里，藏着大用心',
  '把好运，轻轻递到会员手里'
][Math.floor(Math.random() * 3)]

// 游戏类型
const typeOptions = [
  { value: 'WHEEL', label: '大转盘' },
  { value: 'SCRATCH', label: '刮刮乐' },
  { value: 'EGG', label: '砸金蛋' },
  { value: 'SHAKE', label: '摇一摇' }
]
function typeText(t: string) {
  return ({ WHEEL: '大转盘', SCRATCH: '刮刮乐', EGG: '砸金蛋', SHAKE: '摇一摇' } as any)[t] || t
}
function prizeTypeText(t: string) {
  return ({ COUPON: '优惠券', POINTS: '积分', EMPTY: '未中奖' } as any)[t] || t
}
function prizeTagType(t: string): '' | 'success' | 'warning' | 'info' {
  return ({ COUPON: 'warning', POINTS: 'success', EMPTY: 'info' } as any)[t] || ''
}

// ============ 列表 ============
const loading = ref(false)
const list = ref<any[]>([])
const query = reactive({ status: undefined as string | undefined, type: undefined as string | undefined })
const stores = ref<any[]>([])

async function loadList() {
  loading.value = true
  try {
    const params: any = {}
    if (query.status !== undefined) params.status = query.status
    if (query.type) params.type = query.type
    list.value = (await gameApi.list(params)) || []
  } finally {
    loading.value = false
  }
}

async function loadStores() {
  try { stores.value = (await storesApi.list()) || [] } catch {/* */}
}

// 切换状态：调用专用端点, 避免整对象回传造成意外字段覆盖
async function onToggleStatus(row: any, status: boolean) {
  const newStatus = status ? 'ENABLED' : 'DISABLED'
  try {
    await gameApi.toggleStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(status ? '已启用' : '已停用')
  } catch {/* 容错 */}
}

async function onRemove(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除游戏「${row.name}」？删除后无法恢复。`, '提示', { type: 'warning', closeOnPressEscape: true })
  } catch { return }
  await gameApi.remove(row.id)
  ElMessage.success('已删除')
  loadList()
}

// ============ 新建/编辑 ============
const formVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: 0,
  name: '',
  type: 'WHEEL',
  subtitle: '',
  coverImage: '',
  bgImage: '',
  range: [] as string[],
  dailyLimit: 1,
  totalLimit: 0,
  pointsCost: 0,
  storeId: undefined as number | undefined,
  rules: '',
  status: 'ENABLED'
})
const formRules: FormRules = {
  name: [{ required: true, message: '请输入游戏名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择游戏类型', trigger: 'change' }],
  dailyLimit: [{ required: true, message: '请填写每日次数', trigger: 'blur' }],
  totalLimit: [{ required: true, message: '请填写总次数', trigger: 'blur' }]
}

function openCreate() {
  editing.value = false
  Object.assign(form, {
    id: 0, name: '', type: 'WHEEL', subtitle: '', coverImage: '', bgImage: '',
    range: [], dailyLimit: 1, totalLimit: 0, pointsCost: 0, storeId: undefined,
    rules: '', status: 'ENABLED'
  })
  formVisible.value = true
}

function openEdit(row: any) {
  editing.value = true
  Object.assign(form, {
    id: row.id, name: row.name, type: row.type, subtitle: row.subtitle || '',
    coverImage: row.coverImage || '', bgImage: row.bgImage || '',
    range: row.startTime && row.endTime ? [row.startTime, row.endTime] : [],
    dailyLimit: row.dailyLimit ?? 1, totalLimit: row.totalLimit ?? 0,
    pointsCost: row.pointsCost ?? 0, storeId: row.storeId, rules: row.rules || '',
    status: row.status ?? 'ENABLED'
  })
  formVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload: any = {
      name: form.name, type: form.type, subtitle: form.subtitle,
      coverImage: form.coverImage, bgImage: form.bgImage,
      startTime: form.range?.[0], endTime: form.range?.[1],
      dailyLimit: form.dailyLimit, totalLimit: form.totalLimit,
      pointsCost: form.pointsCost, storeId: form.storeId,
      rules: form.rules, status: form.status
    }
    if (editing.value) payload.id = form.id
    await gameApi.save(payload)
    ElMessage.success(editing.value ? '已更新' : '已创建')
    formVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}

// ============ 奖品配置 ============
const prizeVisible = ref(false)
const prizeLoading = ref(false)
const prizeList = ref<any[]>([])
const currentGame = ref<any>(null)

async function openPrizes(row: any) {
  currentGame.value = row
  prizeVisible.value = true
  await loadPrizes(row.id)
}

async function loadPrizes(gameId: number) {
  prizeLoading.value = true
  try {
    prizeList.value = (await gameApi.prizes(gameId)) || []
  } finally {
    prizeLoading.value = false
  }
}

const probSum = computed(() => {
  return prizeList.value.reduce((s, p) => s + (Number(p.probability) || 0), 0)
})
const probSumClass = computed(() => {
  const sum = probSum.value
  if (sum === 100) return 'pos'
  if (sum > 100) return 'neg'
  return 'muted'
})

// 奖品编辑
const prizeFormVisible = ref(false)
const prizeEditing = ref(false)
const prizeSaving = ref(false)
const prizeFormRef = ref<FormInstance>()
const prizeForm = reactive({
  id: 0,
  name: '',
  type: 'COUPON',
  refId: undefined as number | undefined,
  refName: '',
  amount: 0,
  probability: 0,
  imageUrl: '',
  sortOrder: 0
})
const prizeFormRules: FormRules = {
  name: [{ required: true, message: '请输入奖品名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择奖品类型', trigger: 'change' }],
  probability: [{ required: true, message: '请填写概率', trigger: 'blur' }]
}

function openPrizeCreate() {
  prizeEditing.value = false
  Object.assign(prizeForm, {
    id: 0, name: '', type: 'COUPON', refId: undefined, refName: '',
    amount: 0, probability: 0, imageUrl: '', sortOrder: prizeList.value.length
  })
  prizeFormVisible.value = true
}

function openPrizeEdit(row: any) {
  prizeEditing.value = true
  Object.assign(prizeForm, {
    id: row.id, name: row.name, type: row.type,
    refId: row.refId, refName: row.refName || '',
    amount: row.amount ?? 0, probability: row.probability ?? 0,
    imageUrl: row.imageUrl || '', sortOrder: row.sortOrder ?? 0
  })
  prizeFormVisible.value = true
}

async function submitPrizeForm() {
  const valid = await prizeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!currentGame.value) return
  prizeSaving.value = true
  try {
    const payload: any = {
      name: prizeForm.name, type: prizeForm.type,
      refId: prizeForm.refId, refName: prizeForm.refName,
      amount: prizeForm.amount, probability: prizeForm.probability,
      imageUrl: prizeForm.imageUrl, sortOrder: prizeForm.sortOrder
    }
    if (prizeEditing.value) payload.id = prizeForm.id
    await gameApi.savePrize(currentGame.value.id, payload)
    ElMessage.success(prizeEditing.value ? '已更新' : '已添加')
    prizeFormVisible.value = false
    loadPrizes(currentGame.value.id)
  } finally {
    prizeSaving.value = false
  }
}

async function onRemovePrize(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除奖品「${row.name}」？`, '提示', { type: 'warning', closeOnPressEscape: true })
  } catch { return }
  await gameApi.removePrize(row.id)
  ElMessage.success('已删除')
  if (currentGame.value) loadPrizes(currentGame.value.id)
}

// ============ 统计 ============
const statsVisible = ref(false)
const statsLoading = ref(false)
const stats = reactive<any>({ totalPlays: 0, winPlays: 0, prizes: [] })

const winRate = computed(() => {
  if (!stats.totalPlays) return '0%'
  return ((stats.winPlays / stats.totalPlays) * 100).toFixed(1) + '%'
})

async function openStats(row: any) {
  currentGame.value = row
  statsVisible.value = true
  statsLoading.value = true
  try {
    const s = await gameApi.stats(row.id)
    Object.assign(stats, s || { totalPlays: 0, winPlays: 0, prizes: [] })
  } finally {
    statsLoading.value = false
  }
}

onMounted(() => {
  loadList()
  loadStores()
})
</script>

<style scoped>
.filter-wrap { padding: 14px 18px; margin-bottom: 14px; }

.game-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 14px;
}
.game-card { padding: 16px 18px; display: flex; flex-direction: column; gap: 12px; }
.game-head { display: flex; align-items: center; gap: 12px; }
.game-cover {
  width: 52px; height: 52px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
  overflow: hidden;
  background: var(--brand-soft);
  color: var(--brand-ink);
}
.game-cover img { width: 100%; height: 100%; object-fit: cover; }
.game-cover.type-WHEEL { background: rgba(90, 122, 156, 0.12); color: var(--brand-ink); }
.game-cover.type-SCRATCH { background: rgba(184, 132, 92, 0.12); color: #8a5a3a; }
.game-cover.type-EGG { background: rgba(184, 154, 90, 0.13); color: var(--warning-deep); }
.game-cover.type-SHAKE { background: rgba(139, 126, 163, 0.13); color: #5e5278; }
.game-info { flex: 1; min-width: 0; }
.game-name { font-size: 14.5px; font-weight: 600; color: var(--ink); line-height: 1.3; }
.game-type { font-size: 12px; color: var(--muted); margin-top: 2px; }
.game-body { display: flex; flex-direction: column; gap: 6px; }
.game-row { display: flex; font-size: 12.5px; }
.game-row .lbl { color: var(--muted); width: 36px; flex-shrink: 0; }
.game-row .val { color: var(--ink-2); flex: 1; }
.game-actions { display: flex; flex-wrap: wrap; gap: 0; border-top: 1px dashed var(--line); padding-top: 10px; }
.game-actions .el-button { padding: 0 6px; font-size: 12px; }

/* 奖品配置 */
.prize-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
}
.prize-tip { font-size: 12px; color: var(--muted); }
.prize-summary {
  display: flex; justify-content: space-between;
  margin-top: 12px; font-size: 12.5px; color: var(--ink-3);
}
.form-tip { font-size: 11.5px; color: var(--muted); margin-left: 8px; }

/* 统计 */
.stats-content { display: flex; flex-direction: column; gap: 12px; }
.stats-cards {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.stat-item {
  background: var(--surface-2); border: 1px solid var(--line);
  border-radius: var(--r-md); padding: 16px 10px; text-align: center;
}
.stat-val { font-size: 22px; font-weight: 600; color: var(--ink); }
.stat-label { font-size: 11.5px; color: var(--muted); margin-top: 4px; }
</style>
