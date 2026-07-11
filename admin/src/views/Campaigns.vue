<template>
  <div class="page">
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><Promotion /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">营销活动</h2>
          <div class="page-sub">{{ campaignSlogan }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="RefreshRight" @click="loadList">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate" class="btn-scale">新建活动</el-button>
      </div>
    </div>

    <div class="x-card filter-wrap">
      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="状态" clearable @change="loadList" style="width: 140px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </div>
    </div>

    <!-- 活动卡片列表 -->
    <div v-loading="loading" class="camp-grid">
      <div v-for="c in list" :key="c.id" class="camp-card x-card hoverable">
        <div class="camp-head">
          <div class="camp-icon" :class="`type-${c.type}`">
            <el-icon><component :is="typeIcon(c.type)" /></el-icon>
          </div>
          <div class="camp-info">
            <div class="camp-name">{{ c.name }}</div>
            <div class="camp-type">{{ typeText(c.type) }} · {{ channelText(c.channel) }}</div>
          </div>
          <el-switch
            :model-value="!!c.enabled"
            @change="(v: any) => onToggle(c, !!v)"
            inline-prompt active-text="启" inactive-text="停"
          />
        </div>
        <div class="camp-body">
          <div class="camp-row">
            <span class="lbl">触发</span>
            <span class="val">{{ c.trigger || '—' }}</span>
          </div>
          <div class="camp-row">
            <span class="lbl">人群</span>
            <span class="val">{{ c.audience || '全部会员' }}</span>
          </div>
          <div class="camp-row">
            <span class="lbl">周期</span>
            <span class="val">{{ formatDate(c.startAt) }} ~ {{ formatDate(c.endAt) }}</span>
          </div>
        </div>
        <div class="camp-stats">
          <div class="cs">
            <div class="cs-val val">{{ c.stats?.triggered ?? 0 }}</div>
            <div class="cs-label">触发</div>
          </div>
          <div class="cs">
            <div class="cs-val val">{{ c.stats?.reached ?? 0 }}</div>
            <div class="cs-label">触达</div>
          </div>
          <div class="cs">
            <div class="cs-val val">{{ c.stats?.converted ?? 0 }}</div>
            <div class="cs-label">转化</div>
          </div>
        </div>
        <div class="camp-actions">
          <el-button size="small" link type="primary" @click="onPreview(c)">预览命中</el-button>
          <el-button size="small" link type="primary" @click="openStats(c)">统计</el-button>
          <el-button size="small" link type="primary" @click="openEdit(c)">编辑</el-button>
          <el-button size="small" link type="danger" @click="onRemove(c)">删除</el-button>
        </div>
      </div>
      <div v-if="!loading && !list.length" class="empty-state">
        <el-icon><Promotion /></el-icon>
        <span>暂无营销活动，点击右上角新建</span>
      </div>
    </div>

    <!-- 新建/编辑 -->
    <el-dialog v-model="formVisible" :title="editing ? '编辑活动' : '新建活动'" width="600px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="92px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" placeholder="如：生日月专属礼" />
        </el-form-item>
        <el-form-item label="活动类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="BIRTHDAY">生日</el-radio>
            <el-radio value="DORMANT">沉睡唤醒</el-radio>
            <el-radio value="REPURCHASE">复购</el-radio>
            <el-radio value="MANUAL">手动</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="触发条件">
          <el-input v-model="form.trigger" placeholder="如：生日当天 / 30天未消费 / 消费后7天" />
        </el-form-item>
        <el-form-item label="目标人群">
          <el-input v-model="form.audience" placeholder="如：全部会员 / 金卡以上 / 指定标签" />
        </el-form-item>
        <el-form-item label="触达渠道" prop="channel">
          <el-radio-group v-model="form.channel">
            <el-radio value="SMS">短信</el-radio>
            <el-radio value="WECHAT">微信</el-radio>
            <el-radio value="IN_APP">站内</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="触达内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="短信/消息文案" />
        </el-form-item>
        <el-form-item label="活动周期">
          <el-date-picker v-model="form.range" type="datetimerange" range-separator="—" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="SOP 步骤">
          <div class="sop-edit">
            <div v-for="(s, i) in form.sopSteps" :key="i" class="sop-row" :class="`type-${s.type}`">
              <el-select v-model="s.type" size="small" style="width: 100px">
                <el-option label="触发" value="trigger" />
                <el-option label="筛选" value="filter" />
                <el-option label="动作" value="action" />
              </el-select>
              <el-input v-model="s.text" size="small" placeholder="步骤描述" />
              <el-input-number v-if="s.type === 'action'" v-model="s.delayMinutes" :min="0" :step="5" size="small" style="width: 100px" controls-position="right" />
              <el-button link type="danger" :icon="Delete" @click="form.sopSteps.splice(i, 1)" />
            </div>
            <el-button link type="primary" :icon="Plus" @click="addSop">添加步骤</el-button>
          </div>
        </el-form-item>
        <el-form-item label="立即启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- SOP 可视化预览 -->
    <el-dialog v-model="previewVisible" title="活动 SOP 预览" width="780px" :close-on-click-modal="false">
      <div v-loading="previewLoading" class="sop-content">
        <div class="sop-head">
          <div class="sop-meta">
            <div class="meta-item">
              <div class="meta-label">预计命中</div>
              <div class="meta-val val">{{ previewData?.audience?.count ?? 0 }}</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">预计触达</div>
              <div class="meta-val val">{{ previewData?.estimatedReach ?? 0 }}</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">预计成本</div>
              <div class="meta-val val">¥{{ previewData?.estimatedCost?.toFixed(2) || '0.00' }}</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">渠道</div>
              <div class="meta-val">
                <el-tag v-for="ch in previewData?.channels" :key="ch" size="small" effect="plain">{{ channelText(ch) }}</el-tag>
              </div>
            </div>
          </div>
          <div v-if="previewData?.audience?.breakdown?.length" class="sop-breakdown">
            <div class="breakdown-label">人群分布</div>
            <div class="breakdown-list">
              <div v-for="b in previewData.audience.breakdown" :key="b.key" class="bd-item">
                <span class="bd-key">{{ b.key }}</span>
                <div class="bd-bar">
                  <div class="bd-fill" :style="{ width: Math.min(100, b.count / Math.max(...previewData!.audience.breakdown.map(x=>x.count)) * 100) + '%' }"></div>
                </div>
                <span class="bd-count val">{{ b.count }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="sop-flow">
          <div v-for="(step, i) in previewData?.sop" :key="i" class="sop-step" :class="`type-${step.type}`">
            <div class="step-icon">
              <el-icon><component :is="sopIcon(step.type)" /></el-icon>
            </div>
            <div class="step-text">{{ step.text }}</div>
            <div v-if="i < (previewData?.sop?.length ?? 0) - 1" class="step-arrow">→</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 统计弹窗 -->
    <el-dialog v-model="statsVisible" title="活动统计" width="460px">
      <div v-loading="statsLoading" class="stats-grid">
        <div class="stat-item">
          <div class="stat-val">{{ stats.triggered }}</div>
          <div class="stat-label">触发人数</div>
        </div>
        <div class="stat-item">
          <div class="stat-val">{{ stats.reached }}</div>
          <div class="stat-label">触达人数</div>
        </div>
        <div class="stat-item">
          <div class="stat-val">{{ stats.converted }}</div>
          <div class="stat-label">转化人数</div>
        </div>
        <div class="stat-item">
          <div class="stat-val">{{ conversionRate }}</div>
          <div class="stat-label">转化率</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Plus, RefreshRight, Promotion, Clock, Bell, Filter, Aim, Sunny
, Delete } from '@element-plus/icons-vue'
import { campaignsApi } from '@/api'
import { formatDate } from '@/utils/format'
import type { Campaign, CampaignPreview } from '@/types'

const campaignSlogan = [
  '好的营销，是在对的时候想起对的人',
  '用心比用力更重要',
  '每一次触达，都该是一次温柔的问候'
][Math.floor(Math.random() * 3)]

const loading = ref(false)
const list = ref<Campaign[]>([])
const query = reactive({ status: undefined as number | undefined })

async function loadList() {
  loading.value = true
  try {
    const params: any = {}
    if (query.status !== undefined) params.status = query.status === 1 ? 'ENABLED' : 'DISABLED'
    list.value = (await campaignsApi.list(params)) || []
    for (const c of list.value) {
      campaignsApi.stats(c.id).then((s) => { c.stats = s }).catch(() => {})
    }
  } finally {
    loading.value = false
  }
}

function typeText(t: string) {
  return ({ BIRTHDAY: '生日营销', DORMANT: '沉睡唤醒', REPURCHASE: '复购促达', MANUAL: '手动触达' } as any)[t] || t
}
function channelText(c?: string) {
  return ({ SMS: '短信', WECHAT: '微信', IN_APP: '站内' } as any)[c || 'SMS'] || '短信'
}
function typeIcon(t: string) {
  return ({ BIRTHDAY: Sunny, DORMANT: Clock, REPURCHASE: Aim, MANUAL: Promotion } as any)[t] || Promotion
}
function sopIcon(t: string) {
  return ({ trigger: Bell, filter: Filter, action: Promotion } as any)[t] || Promotion
}

// ============ 新建/编辑 ============
const formVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: 0,
  name: '',
  type: 'BIRTHDAY' as Campaign['type'],
  trigger: '',
  audience: '',
  channel: 'SMS' as Campaign['channel'],
  content: '',
  range: [] as string[],
  enabled: true,
  sopSteps: [] as Array<{ type: 'trigger' | 'filter' | 'action'; text: string; delayMinutes?: number }>
})
const formRules: FormRules = {
  name: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  channel: [{ required: true, message: '请选择渠道', trigger: 'change' }],
  content: [{ required: true, message: '请输入触达内容', trigger: 'blur' }]
}
function addSop() {
  if (form.sopSteps.length === 0) {
    form.sopSteps.push({ type: 'trigger', text: '触发条件', delayMinutes: 0 })
  } else if (form.sopSteps.length === 1) {
    form.sopSteps.push({ type: 'filter', text: '筛选人群', delayMinutes: 0 })
  } else {
    form.sopSteps.push({ type: 'action', text: '发送短信', delayMinutes: 0 })
  }
}

function openCreate() {
  editing.value = false
  Object.assign(form, { id: 0, name: '', type: 'BIRTHDAY', trigger: '', audience: '', channel: 'SMS', content: '', range: [], enabled: true, sopSteps: [] })
  formVisible.value = true
}
function openEdit(row: Campaign) {
  editing.value = true
  Object.assign(form, {
    id: row.id, name: row.name, type: row.type, trigger: row.trigger || '',
    audience: row.audience || '', channel: row.channel || 'SMS',
    content: row.content || '',
    range: row.startAt && row.endAt ? [row.startAt, row.endAt] : [],
    enabled: !!row.enabled,
    sopSteps: (row as any).sopSteps || []
  })
  formVisible.value = true
}
async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload: any = {
      name: form.name, type: form.type, trigger: form.trigger,
      audience: form.audience, channel: form.channel, content: form.content,
      startAt: form.range?.[0], endAt: form.range?.[1], enabled: form.enabled,
      sopSteps: form.sopSteps
    }
    if (editing.value) {
      await campaignsApi.update(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await campaignsApi.create(payload)
      ElMessage.success('已创建')
    }
    formVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}
async function onToggle(row: Campaign, enabled: boolean) {
  try {
    await campaignsApi.toggle(row.id, enabled)
    row.enabled = enabled
    ElMessage.success(enabled ? '已启用' : '已停用')
  } catch {/* 容错 */}
}
async function onRemove(row: Campaign) {
  try {
    await ElMessageBox.confirm(`确认删除活动「${row.name}」？`, '提示', { type: 'warning', closeOnPressEscape: true })
  } catch { return }
  await campaignsApi.remove(row.id)
  ElMessage.success('已删除')
  loadList()
}

// ============ SOP 预览 ============
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewData = ref<CampaignPreview>()
async function onPreview(row: Campaign) {
  previewVisible.value = true
  previewLoading.value = true
  previewData.value = undefined
  try {
    previewData.value = await campaignsApi.preview(row.id)
  } catch { ElMessage.error('预览失败') }
  finally { previewLoading.value = false }
}

// ============ 统计 ============
const statsVisible = ref(false)
const statsLoading = ref(false)
const stats = reactive({ triggered: 0, reached: 0, converted: 0 })
const conversionRate = computed(() => {
  if (!stats.triggered) return '0%'
  return ((stats.converted / stats.triggered) * 100).toFixed(1) + '%'
})
async function openStats(row: Campaign) {
  statsVisible.value = true
  statsLoading.value = true
  try {
    const s = await campaignsApi.stats(row.id)
    Object.assign(stats, s)
  } finally { statsLoading.value = false }
}

onMounted(() => loadList())
</script>

<style scoped>
.filter-wrap { padding: 14px 18px; margin-bottom: 14px; }

.camp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}
.camp-card { padding: 16px 18px; display: flex; flex-direction: column; gap: 12px; }
.camp-head { display: flex; align-items: center; gap: 12px; }
.camp-icon {
  width: 40px; height: 40px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.camp-icon.type-BIRTHDAY { background: rgba(192,133,116,0.10); color: var(--danger); }
.camp-icon.type-DORMANT { background: rgba(184,161,106,0.10); color: var(--warning); }
.camp-icon.type-REPURCHASE { background: rgba(111,148,184,0.10); color: var(--primary-action); }
.camp-icon.type-MANUAL { background: rgba(138,130,120,0.12); color: var(--primary); }
.camp-info { flex: 1; min-width: 0; }
.camp-name { font-size: 14.5px; font-weight: 600; color: var(--ink); line-height: 1.3; }
.camp-type { font-size: 12px; color: var(--muted); margin-top: 2px; }
.camp-body { display: flex; flex-direction: column; gap: 6px; }
.camp-row { display: flex; font-size: 12.5px; }
.camp-row .lbl { color: var(--muted); width: 36px; flex-shrink: 0; }
.camp-row .val { color: var(--ink-2); flex: 1; }
.camp-stats {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 8px; padding: 8px 0;
  border-top: 1px dashed var(--line);
  border-bottom: 1px dashed var(--line);
}
.cs { text-align: center; }
.cs-val { font-size: 15px; font-weight: 600; color: var(--ink); }
.cs-label { font-size: 11px; color: var(--muted); margin-top: 2px; }
.camp-actions { display: flex; flex-wrap: wrap; gap: 0; }
.camp-actions .el-button { padding: 0 4px; font-size: 12px; }

/* SOP 预览 */
.sop-content { display: flex; flex-direction: column; gap: 18px; }
.sop-head { display: flex; flex-direction: column; gap: 12px; }
.sop-meta {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  background: var(--surface-2);
  border-radius: var(--radius-md);
  padding: 14px;
}
.meta-item { text-align: center; }
.meta-label { font-size: 11px; color: var(--muted); margin-bottom: 4px; }
.meta-val { font-size: 16px; font-weight: 600; color: var(--ink); }
.meta-val .el-tag { font-weight: 400; }
.sop-breakdown { padding: 12px 14px; border: 1px solid var(--line); border-radius: var(--radius-md); }
.breakdown-label { font-size: 12px; color: var(--muted); margin-bottom: 8px; }
.breakdown-list { display: flex; flex-direction: column; gap: 6px; }
.bd-item { display: flex; align-items: center; gap: 10px; font-size: 12px; }
.bd-key { color: var(--ink-2); width: 80px; flex-shrink: 0; }
.bd-bar { flex: 1; height: 6px; background: var(--line); border-radius: 3px; overflow: hidden; }
.bd-fill { height: 100%; background: var(--brand); border-radius: 3px; }
.bd-count { color: var(--ink); font-weight: 500; min-width: 36px; text-align: right; }

.sop-flow { display: flex; align-items: stretch; gap: 6px; flex-wrap: wrap; }
.sop-step {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px;
  border: 1px dashed var(--card-border-hover);
  border-radius: var(--radius);
  background: var(--surface-2);
  flex: 1 1 30%;
  min-width: 180px;
}
.sop-step.type-trigger { border-color: rgba(192,133,116,0.4); background: rgba(192,133,116,0.04); }
.sop-step.type-filter { border-color: rgba(184,161,106,0.4); background: rgba(184,161,106,0.04); }
.sop-step.type-action { border-color: rgba(111,148,184,0.4); background: rgba(111,148,184,0.04); }
.step-icon {
  width: 28px; height: 28px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  background: var(--card-bg);
  color: var(--ink-2);
}
.sop-step.type-trigger .step-icon { color: var(--danger); }
.sop-step.type-filter .step-icon { color: var(--warning); }
.sop-step.type-action .step-icon { color: var(--primary-action); }
.step-text { font-size: 13px; color: var(--ink); }
.step-arrow { display: none; }

.sop-edit { display: flex; flex-direction: column; gap: 6px; width: 100%; }
.sop-row { display: flex; align-items: center; gap: 8px; padding: 6px; border-radius: 6px; border: 1px dashed var(--line); }
.sop-row.type-trigger { border-color: rgba(192,133,116,0.4); }
.sop-row.type-filter { border-color: rgba(184,161,106,0.4); }
.sop-row.type-action { border-color: rgba(111,148,184,0.4); }
.stats-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.stat-item {
  background: var(--surface-2); border: 1px solid var(--card-border);
  border-radius: 8px; padding: 16px; text-align: center;
}
.stat-val { font-size: 24px; font-weight: 600; color: var(--ink); }
.stat-label { font-size: 12px; color: var(--muted); margin-top: 4px; }
</style>
