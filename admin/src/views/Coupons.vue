<template>
  <div class="page">
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><Ticket /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">优惠券</h2>
          <div class="page-sub">{{ couponSlogan }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="RefreshRight" @click="loadList">刷新</el-button>
        <el-button :icon="Upload" @click="openImport" plain>导入</el-button>
        <el-button :icon="Download" @click="onExport" plain>导出</el-button>
        <el-button :icon="Ticket" @click="verifyVisible = true">核销券码</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate" class="btn-scale">新建券</el-button>
      </div>
    </div>

    <div class="x-card filter-wrap">
      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="状态" clearable @change="loadList" style="width: 130px">
          <el-option label="生效中" value="ACTIVE" />
          <el-option label="已停用" value="STOPPED" />
          <el-option label="已过期" value="EXPIRED" />
        </el-select>
        <el-select v-model="query.type" placeholder="类型" clearable @change="loadList" style="width: 130px">
          <el-option label="满减券" value="FULL_CUT" />
          <el-option label="折扣券" value="PERCENT" />
          <el-option label="体验券" value="EXPERIENCE" />
          <el-option label="生日券" value="BIRTHDAY" />
        </el-select>
      </div>
    </div>

    <!-- 券卡网格 -->
    <div v-loading="loading" class="coupon-grid">
      <div v-for="c in list" :key="c.id" class="coupon-card x-card hoverable">
        <div class="coupon-stripe" :class="`type-${c.type}`"></div>
        <div class="coupon-body">
          <div class="coupon-head">
            <div class="coupon-type">{{ typeText(c.type) }}</div>
            <el-tag :type="statusType(c.status)" effect="light" size="small">{{ statusText(c.status) }}</el-tag>
          </div>
          <div class="coupon-name">{{ c.name }}</div>
          <div class="coupon-rule">{{ ruleText(c) }}</div>
          <div class="coupon-valid">
            <el-icon><Clock /></el-icon>
            <span>{{ validText(c) }}</span>
          </div>
          <div class="coupon-stats">
            <div class="stat">
              <div class="stat-val val">{{ c.granted ?? 0 }}</div>
              <div class="stat-label">已发放</div>
            </div>
            <div class="stat">
              <div class="stat-val val">{{ c.used ?? 0 }}</div>
              <div class="stat-label">已使用</div>
            </div>
            <div class="stat">
              <div class="stat-val val">{{ c.total === 0 ? '∞' : (c.total ?? 0) }}</div>
              <div class="stat-label">总库存</div>
            </div>
          </div>
          <div class="coupon-actions">
            <el-button size="small" link type="primary" @click="openGrant(c)">发放</el-button>
            <el-button size="small" link type="primary" @click="openRecords(c)">记录</el-button>
            <el-button size="small" link type="primary" @click="openEdit(c)">编辑</el-button>
            <el-button size="small" link type="warning" v-if="c.status === 'ACTIVE'" @click="onStop(c)">停用</el-button>
            <el-button size="small" link type="danger" @click="onRemove(c)">删除</el-button>
          </div>
        </div>
      </div>
      <div v-if="!loading && !list.length" class="empty-state">
        <el-icon><DocumentRemove /></el-icon>
        <span>暂无券模板，点击右上角新建</span>
      </div>
    </div>

    <!-- 新建/编辑：左信息 + 右预览 -->
    <el-dialog v-model="formVisible" :title="editing ? '编辑券' : '新建券'" width="780px" :close-on-click-modal="false">
      <div class="form-with-preview">
        <el-form ref="formRef" :model="form" :rules="formRules" label-width="92px" class="form-left">
          <el-form-item label="券名称" prop="name">
            <el-input v-model="form.name" placeholder="如：满200减30" />
          </el-form-item>
          <el-form-item label="类型" prop="type">
            <el-radio-group v-model="form.type" :disabled="editing">
              <el-radio value="FULL_CUT">满减</el-radio>
              <el-radio value="PERCENT">折扣</el-radio>
              <el-radio value="EXPERIENCE">体验</el-radio>
              <el-radio value="BIRTHDAY">生日</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.type === 'FULL_CUT'" label="减免金额" prop="faceValueYuan">
            <el-input-number v-model="form.faceValueYuan" :min="0" :precision="2" :step="10" style="width: 200px" />
            <span class="unit">元</span>
          </el-form-item>
          <el-form-item v-if="form.type === 'PERCENT'" label="折扣" prop="faceValue">
            <el-input-number v-model="form.faceValue" :min="1" :max="99" :step="5" style="width: 200px" />
            <span class="unit">折（如 85 = 8.5 折）</span>
          </el-form-item>
          <el-form-item v-if="form.type === 'FULL_CUT'" label="使用门槛" prop="thresholdYuan">
            <el-input-number v-model="form.thresholdYuan" :min="0" :precision="2" :step="50" style="width: 200px" />
            <span class="unit">元（0 为无门槛）</span>
          </el-form-item>
          <el-form-item label="有效期类型" prop="validType">
            <el-radio-group v-model="form.validType" :disabled="editing">
              <el-radio value="DAYS">领取后 N 天</el-radio>
              <el-radio value="RANGE">固定时间段</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.validType === 'DAYS'" label="有效天数" prop="validDays">
            <el-input-number v-model="form.validDays" :min="1" :step="7" style="width: 200px" />
            <span class="unit">天</span>
          </el-form-item>
          <el-form-item v-if="form.validType === 'RANGE'" label="时间段" prop="validRange">
            <el-date-picker v-model="form.validRange" type="datetimerange" range-separator="—" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="发行总量" prop="total">
            <el-input-number v-model="form.total" :min="0" :step="100" style="width: 200px" />
            <span class="unit">0 表示不限</span>
          </el-form-item>
          <el-form-item label="每人限领" prop="perLimit">
            <el-input-number v-model="form.perLimit" :min="1" :step="1" style="width: 200px" />
          </el-form-item>
          <el-form-item label="适用范围">
            <el-input v-model="form.scope" placeholder="如：全场 / 指定服务" />
          </el-form-item>
        </el-form>
        <!-- 右侧预览 -->
        <div class="form-right">
          <div class="preview-label">预览效果</div>
          <div class="coupon-card preview-card">
            <div class="coupon-stripe" :class="`type-${form.type}`"></div>
            <div class="coupon-body">
              <div class="coupon-type">{{ typeText(form.type) }}</div>
              <div class="coupon-name">{{ form.name || '券名称' }}</div>
              <div class="coupon-rule">{{ ruleTextFromForm() }}</div>
              <div class="coupon-valid">
                <el-icon><Clock /></el-icon>
                <span>{{ validTextFromForm() }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 发放弹窗 -->
    <el-dialog v-model="grantVisible" title="发放优惠券" width="520px" :close-on-click-modal="false">
      <div class="grant-coupon">
        <el-tag effect="light">{{ grantTarget ? typeText(grantTarget.type) : '' }}</el-tag>
        <span class="c-name">{{ grantTarget?.name }}</span>
      </div>
      <el-form ref="grantFormRef" :model="grantForm" label-width="84px" style="margin-top: 14px">
        <el-form-item label="选择会员" required>
          <el-select
            v-model="grantForm.memberIds"
            multiple filterable remote
            :remote-method="searchMembers"
            :loading="memberSearching"
            placeholder="输入姓名/手机号搜索"
            style="width: 100%"
          >
            <el-option v-for="m in memberOptions" :key="m.id" :label="`${m.name} (${m.phone})`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发放门店">
          <el-select v-model="grantForm.storeId" clearable placeholder="可选" style="width: 100%">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :loading="granting" @click="submitGrant">确认发放</el-button>
      </template>
    </el-dialog>

    <!-- 导入弹窗 -->
    <el-dialog v-model="importVisible" title="导入券" width="540px" :close-on-click-modal="false">
      <div class="import-tip">
        CSV 列: 券名称, 类型(FULL_CUT/PERCENT/EXPERIENCE/BIRTHDAY), 面值, 门槛(元), 有效天数。<br />
        PERCENT 面值为折扣百分比(如 85 = 8.5 折)，其余为金额(元)。第一行为表头。
      </div>
      <input type="file" accept=".csv" @change="onImportFile" class="import-file" />
      <div v-if="importFileName" class="import-name">已选: {{ importFileName }}</div>
      <div v-if="importPreview.length" class="import-preview">
        <div class="ip-title">预览 ({{ importPreview.length }} 条):</div>
        <div v-for="(it, i) in importPreview.slice(0, 5)" :key="i" class="ip-row">
          {{ it.name }} · {{ it.type }} · {{ it.type === 'PERCENT' ? (it.faceValue / 10).toFixed(1) + ' 折' : '¥' + (it.faceValue / 100).toFixed(2) }} · 满 ¥{{ (it.threshold || 0) / 100 }} · {{ it.validDays }} 天
        </div>
        <div v-if="importPreview.length > 5" class="ip-more">还有 {{ importPreview.length - 5 }} 条...</div>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importPreview.length" @click="onImportSubmit" class="btn-scale">
          确认导入
        </el-button>
      </template>
    </el-dialog>

    <!-- 导入结果弹窗 -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="520px">
      <div v-if="importResult" class="import-result">
        <div class="ir-summary">
          <div class="ir-item"><span class="ir-num val">{{ importResult.success }}</span><span class="ir-label">成功</span></div>
          <div class="ir-item"><span class="ir-num val neg">{{ importResult.failed }}</span><span class="ir-label">失败</span></div>
        </div>
        <div v-if="importResult.errors?.length" class="ir-errors">
          <div class="ir-errors-title">失败明细（{{ importResult.errors.length }} 条）：</div>
          <el-scrollbar max-height="240">
            <div v-for="(e, i) in importResult.errors" :key="i" class="ir-error">{{ e }}</div>
          </el-scrollbar>
        </div>
        <div v-else class="ir-ok">全部导入成功</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="importResultVisible = false">知道了</el-button>
      </template>
    </el-dialog>

    <!-- 核销弹窗 -->
    <el-dialog v-model="verifyVisible" title="核销券码" width="440px">
      <el-form label-width="80px">
        <el-form-item label="券码" required>
          <el-input v-model="verifyCode" placeholder="输入或扫码券码" />
        </el-form-item>
        <el-form-item label="门店">
          <el-select v-model="verifyStoreId" clearable placeholder="可选" style="width: 100%">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyVisible = false">取消</el-button>
        <el-button type="primary" :loading="verifying" @click="submitVerify">核销</el-button>
      </template>
    </el-dialog>

    <!-- 展示码弹窗 -->
    <el-dialog v-model="displayVisible" width="440px" :show-close="false" align-center>
      <div class="display-card">
        <div class="display-bg"></div>
        <div class="display-content">
          <div class="display-status" :class="`s-${displayData?.status}`">
            <span class="dot" :class="displayStatusDot(displayData?.status)"></span>
            {{ displayStatusText(displayData?.status) }}
          </div>
          <div class="display-code">{{ displayData?.code || '————' }}</div>
          <div class="display-name">{{ displayData?.couponName }}</div>
          <div class="display-member">持券人：{{ displayData?.memberName }}</div>
          <div class="display-qr">
            <div class="qr-box">
              <canvas ref="qrCanvas" class="qr-canvas"></canvas>
            </div>
          </div>
          <div class="display-foot">
            <span v-if="displayData?.status === 'UNUSED'">
              有效期至 {{ formatDateTime(displayData?.expireAt) }}
            </span>
            <span v-else>——</span>
          </div>
          <div class="display-actions">
            <el-button size="small" :icon="CopyDocument" @click="copyCode">复制券码</el-button>
            <el-button size="small" :icon="Refresh" @click="refreshDisplay">刷新</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 发放记录抽屉 -->
    <el-drawer v-model="recordsVisible" title="发放 / 核销记录" size="560px">
      <el-table v-loading="recordsLoading" :data="records" stripe size="small">
        <el-table-column label="会员" prop="memberName" min-width="100" />
        <el-table-column label="券码" prop="code" width="120" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDisplay(row.code)">展示码</el-button>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="recordStatusType(row.status)" effect="light">{{ recordStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发放时间" width="135">
          <template #default="{ row }">{{ formatDateTime(row.grantedAt) }}</template>
        </el-table-column>
        <el-table-column label="核销时间" width="135">
          <template #default="{ row }">{{ formatDateTime(row.usedAt) }}</template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="recordQuery.page"
          :page-size="recordQuery.size"
          :total="recordTotal"
          layout="total, prev, pager, next"
          @current-change="loadRecords"
        />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref, watch, nextTick } from 'vue'
import QRCode from 'qrcode'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Plus, RefreshRight, Ticket, Clock, DocumentRemove,
  CopyDocument, Refresh
, Upload, Download } from '@element-plus/icons-vue'
import { couponsApi, membersApi, storesApi } from '@/api'
import { formatDateTime, yuanToFen, fenToYuan } from '@/utils/format'
import { quickCsv } from '@/utils/csv'
import type { Coupon, CouponDisplay, CouponRecord, Member, Store } from '@/types'

const couponSlogan = [
  '一张好券，是一句问候，也是一份心意',
  '把让利做成温度，把优惠写进心里',
  '细碎的让利，自会长成回响'
][Math.floor(Math.random() * 3)]

const loading = ref(false)
const list = ref<Coupon[]>([])
const stores = ref<Store[]>([])
const query = reactive({ status: '', type: '' })

async function loadList() {
  loading.value = true
  try {
    const params: any = {}
    if (query.status) params.status = query.status
    if (query.type) params.type = query.type
    list.value = (await couponsApi.list(params)) || []
  } finally {
    loading.value = false
  }
}

function typeText(t: string) {
  return ({ FULL_CUT: '满减券', PERCENT: '折扣券', EXPERIENCE: '体验券', BIRTHDAY: '生日券' } as any)[t] || t
}
function statusText(s?: string) {
  return ({ ACTIVE: '生效中', STOPPED: '已停用', EXPIRED: '已过期' } as any)[s || 'ACTIVE'] || '生效中'
}
function statusType(s?: string) {
  return ({ ACTIVE: 'success', STOPPED: 'info', EXPIRED: 'danger' } as any)[s || 'ACTIVE'] || 'success'
}
function ruleText(row: Coupon) {
  if (row.type === 'FULL_CUT') return `满¥${fenToYuan(row.threshold)} 减¥${fenToYuan(row.faceValue)}`
  if (row.type === 'PERCENT') return `${(Number(row.faceValue) / 10).toFixed(1).replace(/\.0$/, '')} 折`
  if (row.type === 'EXPERIENCE') return '免费体验'
  return '生日专享'
}
function validText(row: Coupon) {
  if (row.validType === 'DAYS') return `领取后 ${row.validDays} 天有效`
  return `${formatDateTime(row.validStart)} ~ ${formatDateTime(row.validEnd)}`
}

// ============ 新建/编辑 ============
const formVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: 0,
  name: '',
  type: 'FULL_CUT' as Coupon['type'],
  faceValueYuan: 0,
  faceValue: 85,
  thresholdYuan: 0,
  validType: 'DAYS' as Coupon['validType'],
  validDays: 30,
  validRange: [] as string[],
  total: 0,
  perLimit: 1,
  scope: ''
})
const formRules: FormRules = {
  name: [{ required: true, message: '请输入券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  validType: [{ required: true, message: '请选择有效期类型', trigger: 'change' }]
}
function ruleTextFromForm() {
  if (form.type === 'FULL_CUT') return `满¥${form.thresholdYuan} 减¥${form.faceValueYuan}`
  if (form.type === 'PERCENT') return `${form.faceValue} 折`
  if (form.type === 'EXPERIENCE') return '免费体验'
  return '生日专享'
}
function validTextFromForm() {
  if (form.validType === 'DAYS') return `领取后 ${form.validDays} 天有效`
  if (form.validRange?.length === 2) return `${formatDateTime(form.validRange[0])} ~ ${formatDateTime(form.validRange[1])}`
  return '请选择时间段'
}
function openCreate() {
  editing.value = false
  Object.assign(form, {
    id: 0, name: '', type: 'FULL_CUT', faceValueYuan: 0, faceValue: 85, thresholdYuan: 0,
    validType: 'DAYS', validDays: 30, validRange: [], total: 0, perLimit: 1, scope: ''
  })
  formVisible.value = true
}

// CSV 导出当前列表
function onExport() {
  if (!list.value.length) { ElMessage.warning('暂无可导出数据'); return }
  const typeName = (t: string) => ({ FULL_CUT: '满减', PERCENT: '折扣', EXPERIENCE: '体验', BIRTHDAY: '生日' } as any)[t] || t
  quickCsv(`优惠券-${new Date().toISOString().slice(0, 10)}`, [
    '券名称', '类型', '面值', '门槛', '有效天数', '发放总量', '状态', '创建时间'
  ], list.value.map(c => [
    c.name || '',
    typeName(c.type),
    c.type === 'PERCENT' ? `${(Number(c.faceValue) / 10).toFixed(1).replace(/\.0$/, '')} 折` : fenToYuan(c.faceValue || 0),
    c.type === 'FULL_CUT' ? fenToYuan(c.threshold || 0) : '',
    c.validDays || '',
    c.total || 0,
    statusText(c.status),
    formatDateTime(c.createdAt)
  ]))
  ElMessage.success('已导出 CSV')
}

// CSV 导入(本地解析预览 + 后端批量接口)
const importVisible = ref(false)
const importPreview = ref<any[]>([])
const importFileName = ref('')
const importing = ref(false)
const importResultVisible = ref(false)
const importResult = ref<any>(null)
function openImport() {
  importPreview.value = []
  importFileName.value = ''
  importVisible.value = true
}
function parseCsvLine(line: string): string[] {
  const result: string[] = []
  let cur = ''
  let inQuote = false
  for (let i = 0; i < line.length; i++) {
    const ch = line[i]
    if (ch === '"') {
      if (inQuote && line[i + 1] === '"') { cur += '"'; i++; continue }
      inQuote = !inQuote
      continue
    }
    if (ch === ',' && !inQuote) { result.push(cur); cur = ''; continue }
    cur += ch
  }
  result.push(cur)
  return result
}

function onImportFile(ev: Event) {
  const file = (ev.target as HTMLInputElement).files?.[0]
  if (!file) return
  importFileName.value = file.name
  const reader = new FileReader()
  reader.onload = (e) => {
    const text = String(e.target?.result || '')
    // 去掉 BOM
    const lines = text.replace(/^\uFEFF/, '').split(/\r?\n/).filter(l => l.trim())
    if (!lines.length) { importPreview.value = []; return }
    const data = lines.slice(1).map(line => {
      const cells = parseCsvLine(line)
      const clean = (s: string) => s.trim()
      const name = clean(cells[0] || '')
      const type = clean(cells[1] || 'FULL_CUT')
      const face = Number(clean(cells[2] || '0')) || 0
      const thresh = Number(clean(cells[3] || '0')) || 0
      const days = Number(clean(cells[4] || '30')) || 30
      // FULL_CUT/EXPERIENCE/BIRTHDAY 面值为元; PERCENT 面值为折扣百分比(如 85 = 8.5 折)
      const payload: any = { name, type, validType: 'DAYS', validDays: days }
      if (type === 'PERCENT') {
        payload.faceValue = Math.min(99, Math.max(1, Math.round(face)))
      } else {
        payload.faceValue = face * 100
        payload.threshold = thresh * 100
      }
      return payload
    }).filter(x => x.name)
    importPreview.value = data
  }
  reader.readAsText(file, 'UTF-8')
}
async function onImportSubmit() {
  if (!importPreview.value.length) { ElMessage.warning('请先选择文件'); return }
  importVisible.value = false
  importing.value = true
  try {
    const res = await couponsApi.import(importPreview.value)
    importResult.value = res
    if (res.failed > 0 || res.errors?.length) {
      importResultVisible.value = true
    } else {
      ElMessage.success(`已导入 ${res.success} 条优惠券`)
    }
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '导入失败')
  } finally {
    importing.value = false
  }
}
function openEdit(row: Coupon) {
  editing.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    type: row.type,
    faceValueYuan: row.faceValue ? fenToYuan(row.faceValue) : 0,
    faceValue: row.faceValue || 85,
    thresholdYuan: row.threshold ? fenToYuan(row.threshold) : 0,
    validType: row.validType,
    validDays: row.validDays || 30,
    validRange: row.validStart && row.validEnd ? [row.validStart, row.validEnd] : [],
    total: row.total || 0,
    perLimit: row.perLimit || 1,
    scope: row.scope || ''
  })
  formVisible.value = true
}
async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload: any = {
      name: form.name,
      type: form.type,
      validType: form.validType,
      total: form.total,
      perLimit: form.perLimit,
      scope: form.scope
    }
    if (form.type === 'FULL_CUT') {
      payload.faceValue = yuanToFen(form.faceValueYuan)
      payload.threshold = yuanToFen(form.thresholdYuan)
    } else if (form.type === 'PERCENT') {
      payload.faceValue = form.faceValue
    }
    if (form.validType === 'DAYS') {
      payload.validDays = form.validDays
    } else {
      payload.validStart = form.validRange?.[0]
      payload.validEnd = form.validRange?.[1]
    }
    if (editing.value) {
      await couponsApi.update(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await couponsApi.create(payload)
      ElMessage.success('已创建')
    }
    formVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}
async function onRemove(row: Coupon) {
  try {
    await ElMessageBox.confirm(`确认删除券「${row.name}」？已发放的券将停用`, '提示', {
      type: 'warning', closeOnPressEscape: true
    })
  } catch { return }
  await couponsApi.remove(row.id)
  ElMessage.success('已删除')
  loadList()
}
async function onStop(row: Coupon) {
  try {
    await ElMessageBox.confirm(`确认停用券「${row.name}」？`, '提示', {
      type: 'warning', closeOnPressEscape: true
    })
  } catch { return }
  await couponsApi.stop(row.id)
  ElMessage.success('已停用')
  loadList()
}

// ============ 发放 ============
const grantVisible = ref(false)
const grantTarget = ref<Coupon>()
const granting = ref(false)
const grantFormRef = ref<FormInstance>()
const grantForm = reactive({ memberIds: [] as number[], storeId: undefined as number | undefined })
const memberOptions = ref<Member[]>([])
const memberSearching = ref(false)
async function openGrant(row: Coupon) {
  grantTarget.value = row
  grantForm.memberIds = []
  grantForm.storeId = undefined
  memberOptions.value = []
  grantVisible.value = true
}
async function searchMembers(kw: string) {
  if (!kw) { memberOptions.value = []; return }
  memberSearching.value = true
  try {
    const res = await membersApi.list({ keyword: kw, page: 1, size: 30 })
    memberOptions.value = res.list || []
  } finally {
    memberSearching.value = false
  }
}
async function submitGrant() {
  if (!grantTarget.value || !grantForm.memberIds.length) { ElMessage.warning('请选择会员'); return }
  granting.value = true
  try {
    await couponsApi.grant(grantTarget.value.id, { memberIds: grantForm.memberIds, storeId: grantForm.storeId })
    ElMessage.success(`已发放给 ${grantForm.memberIds.length} 位会员`)
    grantVisible.value = false
    loadList()
  } finally {
    granting.value = false
  }
}

// ============ 核销 ============
const verifyVisible = ref(false)
const verifyCode = ref('')
const verifyStoreId = ref<number | undefined>(undefined)
const verifying = ref(false)
async function submitVerify() {
  if (!verifyCode.value) { ElMessage.warning('请输入券码'); return }
  verifying.value = true
  try {
    await couponsApi.verify({ code: verifyCode.value, storeId: verifyStoreId.value })
    ElMessage.success('核销成功')
    verifyVisible.value = false
    verifyCode.value = ''
  } finally {
    verifying.value = false
  }
}

// ============ 展示码 ============
const displayVisible = ref(false)
const displayData = ref<CouponDisplay>()
const qrCanvas = ref<HTMLCanvasElement>()
let displayTimer: number | null = null
async function openDisplay(code: string) {
  displayVisible.value = true
  await refreshDisplay(code)
}
async function drawQr() {
  const code = displayData.value?.code
  if (!qrCanvas.value || !code) return
  try {
    await QRCode.toCanvas(qrCanvas.value, code, { width: 200, margin: 1 })
  } catch {/* */}
}
async function refreshDisplay(code?: string) {
  const c = code || displayData.value?.code
  if (!c) return
  try {
    displayData.value = await couponsApi.display(c)
    await nextTick(drawQr)
  } catch {
    displayData.value = { code: c, couponName: '券码查询', memberName: '——', status: 'EXPIRED', expireAt: '' } as any
  }
}
function startDisplayTimer() {
  stopDisplayTimer()
  displayTimer = window.setInterval(() => {
    refreshDisplay()
  }, 5000)
}
function stopDisplayTimer() {
  if (displayTimer) {
    clearInterval(displayTimer)
    displayTimer = null
  }
}
watch(displayVisible, (v) => {
  if (v) startDisplayTimer()
  else stopDisplayTimer()
})
function displayStatusText(s?: string) {
  return ({ UNUSED: '待核销', USED: '已核销', EXPIRED: '已过期' } as any)[s || 'UNUSED'] || '待核销'
}
function displayStatusDot(s?: string) {
  return ({ UNUSED: 'success', USED: 'info', EXPIRED: 'danger' } as any)[s || 'UNUSED'] || 'info'
}
function copyCode() {
  if (!displayData.value?.code) return
  navigator.clipboard?.writeText(displayData.value.code).then(
    () => ElMessage.success('已复制到剪贴板'),
    () => ElMessage.warning('复制失败，请手动选择')
  )
}

// ============ 发放记录 ============
const recordsVisible = ref(false)
const recordsLoading = ref(false)
const records = ref<CouponRecord[]>([])
const recordTotal = ref(0)
const recordQuery = reactive({ page: 1, size: 20 })
let recordCouponId = 0
async function openRecords(row: Coupon) {
  recordCouponId = row.id
  recordsVisible.value = true
  recordQuery.page = 1
  await loadRecords()
}
async function loadRecords() {
  recordsLoading.value = true
  try {
    const res = await couponsApi.records(recordCouponId, { page: recordQuery.page, size: recordQuery.size })
    records.value = res.list || []
    recordTotal.value = res.total || 0
  } finally {
    recordsLoading.value = false
  }
}
function recordStatusText(s: string) {
  return ({ UNUSED: '未使用', USED: '已核销', EXPIRED: '已过期' } as any)[s] || s
}
function recordStatusType(s: string) {
  return ({ UNUSED: 'success', USED: 'info', EXPIRED: 'danger' } as any)[s] || 'info'
}

onMounted(async () => {
  stores.value = await storesApi.list().catch(() => [])
  loadList()
})

onBeforeUnmount(() => {
  stopDisplayTimer()
})
</script>

<style scoped>
.filter-wrap { padding: 14px 18px; margin-bottom: 14px; }

.import-tip { font-size: 12px; color: var(--muted); background: var(--surface-2); padding: 8px 12px; border-radius: 6px; margin-bottom: 12px; line-height: 1.7; }
.import-file { display: block; padding: 6px 0; }
.import-name { font-size: 12px; color: var(--ink-2); margin: 8px 0; }
.import-preview { background: var(--surface-2); padding: 10px 12px; border-radius: 6px; max-height: 180px; overflow-y: auto; }
.ip-title { font-size: 12px; color: var(--ink-2); font-weight: 500; margin-bottom: 6px; }
.ip-row { font-size: 11.5px; color: var(--ink-2); padding: 3px 0; border-bottom: 1px dashed var(--line); }
.ip-row:last-child { border-bottom: none; }
.ip-more { font-size: 11px; color: var(--muted); margin-top: 6px; }
.import-result .ir-summary { display: flex; gap: 24px; margin-bottom: 14px; }
.import-result .ir-item { display: flex; flex-direction: column; gap: 2px; }
.import-result .ir-num { font-size: 26px; font-weight: 600; color: var(--success); }
.import-result .ir-num.neg { color: var(--danger); }
.import-result .ir-label { font-size: 12px; color: var(--muted); }
.import-result .ir-ok { color: var(--success); font-size: 13px; padding: 12px 0; }
.import-result .ir-errors-title { font-size: 12.5px; color: var(--muted); margin-bottom: 8px; }
.import-result .ir-error {
  font-size: 12.5px; color: var(--danger-deep);
  padding: 5px 8px; border-left: 2px solid var(--danger-soft);
  background: var(--surface-2); border-radius: 4px; margin-bottom: 4px;
}

/* 券卡网格 */
.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}
.coupon-card {
  display: flex;
  border-radius: var(--radius-lg);
  overflow: hidden;
  position: relative;
  min-height: 220px;
}
.coupon-stripe {
  width: 6px;
  flex-shrink: 0;
}
.type-FULL_CUT { background: var(--brand); }
.type-PERCENT { background: var(--warning); }
.type-EXPERIENCE { background: var(--success); }
.type-BIRTHDAY { background: var(--accent-rose); }

.coupon-body { padding: 14px 16px 12px; flex: 1; }
.coupon-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.coupon-type { font-size: 11.5px; color: var(--muted); letter-spacing: 1px; }
.coupon-name { font-size: 15px; font-weight: 600; color: var(--ink); margin-bottom: 6px; line-height: 1.4; }
.coupon-rule { font-size: 13px; color: var(--ink-2); margin-bottom: 8px; }
.coupon-valid {
  display: flex; align-items: center; gap: 4px;
  font-size: 11.5px; color: var(--muted);
  margin-bottom: 12px;
}
.coupon-stats {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 8px 0;
  border-top: 1px dashed var(--line);
  border-bottom: 1px dashed var(--line);
  margin-bottom: 8px;
}
.stat { text-align: center; }
.stat-val { font-size: 14px; font-weight: 600; color: var(--ink); }
.stat-label { font-size: 11px; color: var(--muted); margin-top: 2px; }
.coupon-actions { display: flex; gap: 0; flex-wrap: wrap; }
.coupon-actions .el-button { padding: 0 4px; font-size: 12px; }

/* 表单 + 预览 */
.form-with-preview { display: flex; gap: 18px; }
.form-left { flex: 1.4; }
.form-right {
  flex: 1;
  padding: 18px 14px;
  background: var(--surface-2);
  border-radius: var(--radius-md);
  display: flex; flex-direction: column; align-items: center;
}
.preview-label { font-size: 12px; color: var(--muted); margin-bottom: 12px; }
.preview-card { width: 240px; min-height: 200px; }
.unit { margin-left: 8px; font-size: 12px; color: var(--muted); }

/* 展示码弹窗 */
.display-card {
  position: relative;
  border-radius: var(--r-lg);
  overflow: hidden;
  min-height: 380px;
  background: var(--brand);
  color: #fff;
  padding: 28px 24px;
}
.display-content { position: relative; z-index: 1; text-align: center; }
.display-status {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 12px;
  padding: 4px 10px;
  background: rgba(255,255,255,0.18);
  border-radius: 999px;
  margin-bottom: 18px;
}
.display-status .dot { background: #fff; }
.display-status.s-USED { background: rgba(255,255,255,0.10); }
.display-status.s-EXPIRED { background: rgba(0,0,0,0.18); }
.display-code {
  font-family: -apple-system, 'SF Mono', Menlo, monospace;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 4px;
  margin: 6px 0 14px;
}
.display-name { font-size: 16px; font-weight: 500; margin-bottom: 4px; }
.display-member { font-size: 12px; opacity: 0.85; margin-bottom: 18px; }
.display-qr { margin: 14px 0; }
.qr-box {
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  padding: 10px;
  display: flex; align-items: center; justify-content: center;
}
.qr-canvas { display: block; width: 200px; height: 200px; }
.display-foot { font-size: 12px; opacity: 0.85; margin-top: 10px; }
.display-actions { margin-top: 18px; display: flex; justify-content: center; gap: 8px; }
.display-actions .el-button { background: rgba(255,255,255,0.18); color: #fff; border-color: rgba(255,255,255,0.25); }
.display-actions .el-button:hover { background: rgba(255,255,255,0.30); color: #fff; }

.c-name { font-size: 13px; color: var(--ink); font-weight: 500; }
.grant-coupon {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; background: var(--surface-2); border-radius: 8px;
}
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
