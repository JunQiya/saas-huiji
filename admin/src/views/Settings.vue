<template>
  <div class="page settings-page">
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><Setting /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">系统设置</h2>
          <div class="page-sub">{{ settingsSlogan }}</div>
        </div>
      </div>
    </div>

    <el-tabs v-model="tab" class="settings-tabs">
      <!-- 基础 / 品牌 -->
      <el-tab-pane label="基础 / 品牌" name="basic">
        <div v-loading="loading" class="settings-grid">
          <div class="x-card section-card">
            <div class="section-title">租户信息</div>
            <el-form label-width="100px">
              <el-form-item label="租户名称">
                <el-input v-model="form.tenantName" />
              </el-form-item>
              <el-form-item label="品牌主色">
                <div class="color-row">
                  <el-color-picker v-model="form.brandColor" />
                  <el-input v-model="form.brandColor" style="width: 120px" />
                  <el-button size="small" @click="form.brandColor = '#8a8278'">恢复默认</el-button>
                </div>
              </el-form-item>
              <el-form-item label="短信签名">
                <el-input v-model="form.smsSign" placeholder="如: 星河会记" />
              </el-form-item>
            </el-form>
          </div>

          <div class="x-card section-card">
            <div class="section-title">
              <span>等级规则</span>
              <el-button link type="primary" :icon="Plus" @click="addLevel">添加等级</el-button>
            </div>
            <div class="rule-tip">按累计消费(元)自动升级, 阈值需逐级递增</div>
            <el-table :data="form.levelRules" size="small" style="margin-top: 10px">
              <el-table-column label="等级" width="70">
                <template #default="{ row }">
                  <el-input-number v-model="row.level" :min="1" :max="9" size="small" controls-position="right" style="width: 70px" />
                </template>
              </el-table-column>
              <el-table-column label="名称">
                <template #default="{ row }"><el-input v-model="row.name" size="small" /></template>
              </el-table-column>
              <el-table-column label="累计消费阈值(元)">
                <template #default="{ row }">
                  <el-input-number v-model="row.thresholdYuan" :min="0" :precision="2" size="small" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="70">
                <template #default="{ $index }">
                  <el-button link type="danger" :icon="Delete" @click="form.levelRules.splice($index, 1)" />
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="x-card section-card">
            <div class="section-title">
              <span>储值赠送规则</span>
              <el-button link type="primary" :icon="Plus" @click="addRecharge">添加规则</el-button>
            </div>
            <div class="rule-tip">充值满指定金额赠送对应金额</div>
            <el-table :data="form.rechargeRules" size="small" style="margin-top: 10px">
              <el-table-column label="充值金额(元)">
                <template #default="{ row }">
                  <el-input-number v-model="row.amountYuan" :min="0" :precision="2" size="small" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="赠送金额(元)">
                <template #default="{ row }">
                  <el-input-number v-model="row.giftYuan" :min="0" :precision="2" size="small" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="70">
                <template #default="{ $index }">
                  <el-button link type="danger" :icon="Delete" @click="form.rechargeRules.splice($index, 1)" />
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div class="footer-actions">
            <el-button @click="loadBasic">重置</el-button>
            <el-button type="primary" :loading="saving" @click="saveBasic" class="btn-scale">保存</el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- 功能设置 -->
      <el-tab-pane label="功能设置" name="features">
        <div v-loading="featuresLoading" class="settings-grid">
          <div class="x-card section-card">
            <div class="section-title">功能模块开关</div>
            <div class="rule-tip">关闭后对应模块在前台和后台均不可用，已存在数据不受影响</div>
            <div class="feature-list">
              <div v-for="f in featureList" :key="f.key" class="feature-item">
                <div class="fi-info">
                  <div class="fi-name">{{ f.label }}</div>
                  <div class="fi-desc">{{ f.desc }}</div>
                </div>
                <el-switch v-model="featureFlags[f.key]" :loading="featuresSaving" />
              </div>
            </div>
          </div>
          <div class="footer-actions">
            <el-button @click="loadFeatures">重置</el-button>
            <el-button type="primary" :loading="featuresSaving" @click="saveFeatures" class="btn-scale">保存</el-button>
          </div>
        </div>
      </el-tab-pane>

      <!-- 计费 -->
      <el-tab-pane label="计费" name="plan">
        <div v-loading="loading" class="settings-grid">
          <div class="x-card plan-card">
            <div class="section-title">当前套餐</div>
            <div class="plan-current">
              <div class="pc-name">{{ planInfo.plan || 'FREE' }}</div>
              <div class="pc-meta">
                <span>到期: {{ planInfo.expiresAt || '永久' }}</span>
                <span>·</span>
                <span>短信余额: {{ planInfo.smsBalance ?? 0 }}</span>
              </div>
              <div class="pc-limits">
                <div v-for="(v, k) in (planInfo.limits || {})" :key="k" class="limit-pill">
                  <span class="lp-k">{{ limitLabel(k) }}</span>
                  <span class="lp-v val">{{ v }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="plan-grid">
            <div v-for="p in plans" :key="p.value" class="x-card plan-tile" :class="{ active: planInfo.plan === p.value }">
              <div class="pt-head">
                <span class="pt-name">{{ p.label }}</span>
                <span v-if="planInfo.plan === p.value" class="pt-badge">当前</span>
              </div>
              <div class="pt-price">¥ <span class="num">{{ p.price }}</span><span class="unit">/月</span></div>
              <ul class="pt-list">
                <li v-for="(f, idx) in p.features" :key="idx">{{ f }}</li>
              </ul>
              <el-button :type="planInfo.plan === p.value ? '' : 'primary'" :disabled="planInfo.plan === p.value" class="btn-scale" @click="openUpgrade(p)">
                {{ planInfo.plan === p.value ? '当前套餐' : '升级到此' }}
              </el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 多店 -->
      <el-tab-pane label="多店" name="store">
        <div v-loading="loading" class="settings-grid">
          <div class="x-card section-card">
            <div class="section-title">当前门店</div>
            <div class="store-current">
              <div class="sc-name">{{ currentStore.name || '未选择' }}</div>
              <div class="sc-meta">
                <span>地址: {{ currentStore.address || '-' }}</span>
                <span>·</span>
                <span>电话: {{ currentStore.phone || '-' }}</span>
              </div>
            </div>
          </div>

          <div class="x-card section-card">
            <div class="section-title">切换门店</div>
            <el-table :data="stores" size="small" @row-click="onSwitch">
              <el-table-column label="门店" prop="name" />
              <el-table-column label="地址" prop="address" />
              <el-table-column label="电话" prop="phone" width="140" />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button link type="primary" :disabled="row.id === currentStore.storeId" @click.stop="onSwitch(row)">
                    {{ row.id === currentStore.storeId ? '当前' : '切换' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 升级弹窗 -->
    <el-dialog v-model="upgradeVisible" :title="`升级到 ${upgradeTarget?.label}`" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="套餐">
          <span>{{ upgradeTarget?.label }} · ¥{{ upgradeTarget?.price }}/月</span>
        </el-form-item>
        <el-form-item label="月数">
          <el-input-number v-model="upgradeMonths" :min="1" :max="36" />
        </el-form-item>
        <el-form-item label="合计">
          <span class="val">¥ {{ ((upgradeTarget?.price || 0) * upgradeMonths).toFixed(2) }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="upgradeVisible = false">取消</el-button>
        <el-button type="primary" :loading="upgrading" @click="confirmUpgrade" class="btn-scale">确认升级</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Setting } from '@element-plus/icons-vue'
import { settingsApi, settingsPlanApi, storesApi } from '@/api'
import { fenToYuan, yuanToFen } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import type { TenantSettings } from '@/types'

const settingsSlogan = [
  '让设定保持简洁，让心意保持清晰',
  '把系统调成最像自己的样子',
  '不喧哗，自有分量'
][Math.floor(Math.random() * 3)]

const tab = ref('basic')
const loading = ref(false)
const saving = ref(false)
const upgrading = ref(false)
const userStore = useUserStore()
const router = useRouter()

const form = reactive({
  tenantName: '',
  brandColor: '#8a8278',
  smsSign: '',
  levelRules: [] as any[],
  rechargeRules: [] as any[]
})

// 品牌色实时生效
watch(() => form.brandColor, (color) => {
  if (color) {
    document.documentElement.style.setProperty('--brand', color)
    document.documentElement.style.setProperty('--primary-action', color)
  }
}, { immediate: true })

const plans = [
  { value: 'FREE', label: '免费版', price: 0, features: ['会员 500', '门店 3', '商品 30', '基础数据看板'] },
  { value: 'BASIC', label: '基础版', price: 199, features: ['会员 5000', '门店 10', '商品 100', '短信 200/月', '生日营销'] },
  { value: 'GROWTH', label: '成长版', price: 599, features: ['会员 50000', '门店 30', '商品 500', '短信 1000/月', '智能分群', '群发'] },
  { value: 'FLAGSHIP', label: '旗舰版', price: 1499, features: ['不限会员', '不限门店', '商品 1000', '短信 5000/月', '全功能', '专属客服'] }
]

const planInfo = reactive<any>({ plan: 'FREE', expiresAt: null, smsBalance: 0, limits: {} })
const upgradeVisible = ref(false)
const upgradeTarget = ref<any>(null)
const upgradeMonths = ref(12)

const stores = ref<any[]>([])
const currentStore = reactive<any>({ storeId: null, name: '', address: '', phone: '' })

function limitLabel(k: string) {
  return ({ members: '会员上限', stores: '门店上限', products: '商品上限', employees: '员工上限' } as any)[k] || k
}

async function loadBasic() {
  loading.value = true
  try {
    const s: any = await settingsApi.get()
    form.tenantName = s.tenantName || ''
    form.brandColor = s.brandColor || '#8a8278'
    form.smsSign = s.smsSign || ''
    form.levelRules = (s.levelRules || []).map((r: any) => ({
      level: r.level, name: r.name, threshold: r.threshold, thresholdYuan: Number(fenToYuan(r.threshold))
    }))
    form.rechargeRules = (s.rechargeRules || []).map((r: any) => ({
      amount: r.amount, gift: r.gift, amountYuan: Number(fenToYuan(r.amount)), giftYuan: Number(fenToYuan(r.gift))
    }))
  } finally {
    loading.value = false
  }
}

async function loadPlan() {
  try {
    const data: any = await settingsPlanApi.get()
    Object.assign(planInfo, data || {})
  } catch (e) { /* 静默 */ }
}

async function loadStores() {
  try {
    stores.value = await storesApi.list()
  } catch { stores.value = [] }
  try {
    const cur: any = await settingsPlanApi.currentStore()
    if (cur) {
      currentStore.storeId = cur.storeId
      currentStore.name = cur.name || ''
      currentStore.address = cur.address || ''
      currentStore.phone = cur.phone || ''
    }
  } catch {}
}

function addLevel() {
  const next = form.levelRules.length + 1
  form.levelRules.push({ level: next, name: `等级${next}`, threshold: 0, thresholdYuan: 0 })
}
function addRecharge() {
  form.rechargeRules.push({ amount: 0, gift: 0, amountYuan: 0, giftYuan: 0 })
}

async function saveBasic() {
  if (!form.tenantName.trim()) { ElMessage.warning('请填写租户名称'); return }
  saving.value = true
  try {
    const payload: TenantSettings = {
      tenantName: form.tenantName,
      brandColor: form.brandColor,
      smsSign: form.smsSign,
      levelRules: form.levelRules.map(r => ({ level: r.level, name: r.name, threshold: yuanToFen(r.thresholdYuan) })).sort((a, b) => a.level - b.level),
      rechargeRules: form.rechargeRules.map(r => ({ amount: yuanToFen(r.amountYuan), gift: yuanToFen(r.giftYuan) }))
    }
    await settingsApi.update(payload)
    ElMessage.success('已保存')
    loadBasic()
  } finally {
    saving.value = false
  }
}

function openUpgrade(p: any) {
  upgradeTarget.value = p
  upgradeMonths.value = 12
  upgradeVisible.value = true
}

async function confirmUpgrade() {
  if (!upgradeTarget.value) return
  upgrading.value = true
  try {
    await settingsPlanApi.upgrade({ plan: upgradeTarget.value.value, months: upgradeMonths.value })
    ElMessage.success(`已升级到 ${upgradeTarget.value.label}`)
    upgradeVisible.value = false
    loadPlan()
  } finally {
    upgrading.value = false
  }
}

// ============ 功能设置 ============
const featuresLoading = ref(false)
const featuresSaving = ref(false)
const featureFlags = reactive<any>({
  pointsEnabled: true,
  rechargeEnabled: true,
  couponsEnabled: true,
  campaignsEnabled: true,
  referralEnabled: true,
  birthdayMarketingEnabled: true,
  smsEnabled: false,
  selfRegisterEnabled: true,
  autoUpgradeEnabled: true,
  receiptPrintEnabled: true
})
const featureList = [
  { key: 'pointsEnabled', label: '积分系统', desc: '会员消费累计积分，支持手动调整' },
  { key: 'rechargeEnabled', label: '储值功能', desc: '会员储值充值、余额支付' },
  { key: 'couponsEnabled', label: '优惠券', desc: '创建、发放、核销优惠券' },
  { key: 'campaignsEnabled', label: '营销活动', desc: '自动化营销活动与 SOP 触达' },
  { key: 'referralEnabled', label: '推荐裂变', desc: '老带新推荐关系与奖励' },
  { key: 'birthdayMarketingEnabled', label: '生日营销', desc: '会员生日自动关怀与赠礼' },
  { key: 'smsEnabled', label: '短信通知', desc: '消耗短信余额发送通知' },
  { key: 'selfRegisterEnabled', label: '会员自助注册', desc: '允许会员通过 H5 自主注册' },
  { key: 'autoUpgradeEnabled', label: '自动升级等级', desc: '消费达标后自动提升会员等级' },
  { key: 'receiptPrintEnabled', label: '小票打印', desc: '收银台结算后支持打印小票' }
]
async function loadFeatures() {
  featuresLoading.value = true
  try {
    const data = await settingsApi.getFeatures()
    Object.assign(featureFlags, data || {})
  } finally {
    featuresLoading.value = false
  }
}
async function saveFeatures() {
  featuresSaving.value = true
  try {
    await settingsApi.updateFeatures({ ...featureFlags })
    ElMessage.success('功能设置已保存')
  } finally {
    featuresSaving.value = false
  }
}

async function onSwitch(row: any) {
  const res: any = await settingsPlanApi.switchStore(row.id)
  if (res?.token) {
    userStore.setToken(res.token)
  }
  ElMessage.success(`已切换到: ${row.name}`)
  // 平滑刷新：重载本 tab 的数据 + 兜底刷一次路由（避免整页 reload 的闪烁）
  await Promise.all([loadBasic(), loadPlan(), loadStores(), loadFeatures()])
  // 通知其它视图：storeId 变化，可重新拉数
  router.replace({ query: { ...router.currentRoute.value.query, _storeTs: String(Date.now()) } })
}

onMounted(() => { loadBasic(); loadPlan(); loadStores(); loadFeatures() })
</script>

<style scoped>
.settings-tabs :deep(.el-tabs__header) { background: var(--card-bg); border-radius: var(--radius-lg); padding: 4px 12px; border: 1px solid var(--card-border); margin-bottom: 12px; }
.settings-grid { display: flex; flex-direction: column; gap: 14px; }
.section-card { padding: 18px 20px; }
.section-title { font-size: 15px; font-weight: 600; color: var(--ink); margin-bottom: 8px; display: flex; align-items: center; justify-content: space-between; }
.rule-tip { font-size: 12px; color: var(--muted); }
.color-row { display: flex; align-items: center; gap: 10px; }
.footer-actions { display: flex; justify-content: flex-end; gap: 10px; padding: 4px 0 12px; }

.plan-card { padding: 18px 20px; }
.plan-current { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; }
.pc-name { font-size: 28px; font-weight: 600; color: var(--primary-action); }
.pc-meta { color: var(--muted); font-size: 13px; display: flex; gap: 6px; align-items: center; }
.pc-limits { display: flex; flex-wrap: wrap; gap: 6px; }
.limit-pill { display: inline-flex; gap: 4px; align-items: center; padding: 4px 10px; background: var(--surface-2); border-radius: 999px; font-size: 12px; }
.limit-pill .lp-k { color: var(--muted); }
.limit-pill .lp-v { color: var(--ink); font-weight: 600; }

.plan-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 14px; }
.plan-tile { padding: 18px; display: flex; flex-direction: column; gap: 10px; }
.plan-tile.active { border-color: var(--primary-action); box-shadow: 0 0 0 1px var(--primary-action); }
.pt-head { display: flex; align-items: center; justify-content: space-between; }
.pt-name { font-size: 16px; font-weight: 600; color: var(--ink); }
.pt-badge { font-size: 11px; color: var(--primary-action); background: var(--primary-action-soft); padding: 2px 8px; border-radius: 999px; }
.pt-price { color: var(--ink-2); }
.pt-price .num { font-size: 28px; color: var(--primary-action); font-weight: 600; }
.pt-price .unit { font-size: 12px; color: var(--muted); }
.pt-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 4px; }
.pt-list li { font-size: 13px; color: var(--ink-2); padding-left: 14px; position: relative; }
.pt-list li::before { content: '✓'; color: var(--success); position: absolute; left: 0; }

.store-current { padding: 10px 0; }
.sc-name { font-size: 20px; font-weight: 600; color: var(--ink); }
.sc-meta { color: var(--muted); font-size: 13px; margin-top: 4px; display: flex; gap: 6px; }

.feature-list { display: flex; flex-direction: column; gap: 2px; margin-top: 10px; }
.feature-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 0; border-bottom: 1px dashed var(--line);
}
.feature-item:last-child { border-bottom: none; }
.fi-info { flex: 1; }
.fi-name { font-size: 14px; font-weight: 500; color: var(--ink); }
.fi-desc { font-size: 12px; color: var(--muted); margin-top: 2px; }
</style>
