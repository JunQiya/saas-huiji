<template>
  <div class="page">
    <!-- 增强版 page-header -->
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><User /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">会员管理</h2>
          <div class="page-sub">{{ memberSlogan }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="Download" @click="onExport">导出</el-button>
        <el-button :icon="Upload" @click="onImportClick">导入</el-button>
        <input ref="fileInputRef" type="file" accept=".csv" hidden @change="onFileChange" />
        <el-button type="primary" :icon="Plus" @click="openCreate" class="btn-scale">新增会员</el-button>
      </div>
    </div>

    <!-- 批量操作栏 -->
    <transition name="slide-down">
      <div v-if="selected.length" class="batch-bar x-card">
        <div class="batch-text">已选 {{ selected.length }} 位会员</div>
        <div class="batch-actions">
          <el-button size="small" @click="openBatchTag">
            <el-icon><PriceTag /></el-icon> 批量打标签
          </el-button>
          <el-button size="small" @click="openBatchLevel">
            <el-icon><Medal /></el-icon> 批量调整等级
          </el-button>
          <el-button size="small" type="danger" link @click="clearSelection">取消选择</el-button>
        </div>
      </div>
    </transition>

    <div class="x-card table-wrap">
      <!-- 筛选 -->
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="姓名/手机号" clearable @keyup.enter="onSearch" />
        <el-select v-model="query.level" placeholder="等级" clearable @change="onSearch">
          <el-option label="普通" :value="1" />
          <el-option label="银卡" :value="2" />
          <el-option label="金卡" :value="3" />
          <el-option label="钻石" :value="4" />
        </el-select>
        <el-input v-model="query.tag" placeholder="标签" clearable @keyup.enter="onSearch" />
        <el-select v-model="query.storeIds" placeholder="门店" clearable multiple collapse-tags @change="onSearch">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
        <div class="filter-spacer"></div>
        <el-tooltip content="切换表格密度" placement="top">
          <el-switch
            v-model="compact"
            inline-prompt
            active-text="紧凑"
            inactive-text="默认"
            size="small"
          />
        </el-tooltip>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="list"
        :size="compact ? 'small' : 'default'"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="42" />
        <el-table-column label="会员" min-width="160">
          <template #default="{ row }">
            <div class="cell-member">
              <el-avatar :size="32" class="m-avatar">{{ row.name?.charAt(0) }}</el-avatar>
              <div>
                <div class="m-name">{{ row.name }}</div>
                <div class="m-phone">{{ row.phone }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.level)" effect="light" round>{{ row.levelName || levelName(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="储值余额" width="110" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.balance) }}</template>
        </el-table-column>
        <el-table-column label="积分" width="80" align="right">
          <template #default="{ row }">{{ row.points ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="累计消费" width="120" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="标签" min-width="140">
          <template #default="{ row }">
            <el-tag v-for="t in row.tags" :key="t" size="small" class="m-tag" effect="plain">{{ t }}</el-tag>
            <span v-if="!row.tags?.length" class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="最近消费" width="150">
          <template #default="{ row }">{{ formatDateTime(row.lastConsumeAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" class-name="row-actions">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openRecharge(row)">储值</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onRemove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadList"
          @size-change="loadList"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="editing ? '编辑会员' : '新增会员'" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="84px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio value="M">男</el-radio>
            <el-radio value="F">女</el-radio>
            <el-radio value="U">未知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" placeholder="选择生日" style="width: 100%" />
        </el-form-item>
        <el-form-item label="所属门店">
          <el-select v-model="form.storeIds" multiple collapse-tags placeholder="可多选" style="width: 100%">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tags" multiple filterable allow-create default-first-option placeholder="输入回车添加" style="width: 100%">
            <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 储值充值弹窗 -->
    <el-dialog v-model="rechargeVisible" title="储值充值" width="440px" :close-on-click-modal="false">
      <div class="recharge-member">
        <el-avatar :size="36" class="m-avatar">{{ rechargeTarget?.name?.charAt(0) }}</el-avatar>
        <div>
          <div class="m-name">{{ rechargeTarget?.name }}</div>
          <div class="m-phone">当前余额 ¥{{ formatMoney(rechargeTarget?.balance) }}</div>
        </div>
      </div>
      <el-form ref="rechargeFormRef" :model="rechargeForm" :rules="rechargeRules" label-width="84px" style="margin-top: 14px">
        <el-form-item label="充值金额" prop="amountYuan">
          <el-input-number v-model="rechargeForm.amountYuan" :min="0" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="赠送金额">
          <el-input-number v-model="rechargeForm.giftYuan" :min="0" :precision="2" :step="50" style="width: 100%" />
          <div class="form-tip">按设置中的赠送规则可自动带入，可手动调整</div>
        </el-form-item>
        <el-form-item label="支付方式" prop="payMethod">
          <el-select v-model="rechargeForm.payMethod" style="width: 100%">
            <el-option label="现金" value="CASH" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="银行卡" value="BANK" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="rechargeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="submitRecharge">确认充值</el-button>
      </template>
    </el-dialog>

    <!-- 批量打标签 -->
    <el-dialog v-model="batchTagVisible" title="批量打标签" width="420px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="覆盖策略">
          <el-radio-group v-model="batchTagMode">
            <el-radio value="cover">覆盖现有标签</el-radio>
            <el-radio value="append">追加到现有标签</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="选择标签">
          <el-select v-model="batchTagValue" multiple filterable allow-create default-first-option placeholder="输入回车添加" style="width: 100%">
            <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="form-tip">将对 {{ selected.length }} 位会员应用</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchTagVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSaving" @click="submitBatchTag">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量调等级 -->
    <el-dialog v-model="batchLevelVisible" title="批量调整等级" width="380px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="目标等级">
          <el-select v-model="batchLevelValue" style="width: 100%">
            <el-option label="普通" :value="1" />
            <el-option label="银卡" :value="2" />
            <el-option label="金卡" :value="3" />
            <el-option label="钻石" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="form-tip">将对 {{ selected.length }} 位会员应用</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchLevelVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSaving" @click="submitBatchLevel">确定</el-button>
      </template>
    </el-dialog>

    <!-- 积分调整弹窗 -->
    <el-dialog v-model="pointsVisible" title="调整积分" width="400px" :close-on-click-modal="false">
      <div class="recharge-member" v-if="detail">
        <el-avatar :size="36" class="m-avatar">{{ detail.name?.charAt(0) }}</el-avatar>
        <div>
          <div class="m-name">{{ detail.name }}</div>
          <div class="m-phone">当前积分 {{ detail.points ?? 0 }}</div>
        </div>
      </div>
      <el-form ref="pointsFormRef" :model="pointsForm" :rules="pointsRules" label-width="80px" style="margin-top: 14px">
        <el-form-item label="调整方式" prop="mode">
          <el-radio-group v-model="pointsForm.mode">
            <el-radio value="add">增加</el-radio>
            <el-radio value="sub">扣减</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="积分数" prop="amount">
          <el-input-number v-model="pointsForm.amount" :min="1" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原因" prop="reason">
          <el-input v-model="pointsForm.reason" type="textarea" :rows="2" placeholder="如：活动赠送、手工调整、消费补偿" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsVisible = false">取消</el-button>
        <el-button type="primary" :loading="pointsSaving" @click="submitPoints">确认调整</el-button>
      </template>
    </el-dialog>

    <!-- 单会员等级编辑弹窗 -->
    <el-dialog v-model="levelEditVisible" title="修改等级" width="360px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="当前等级">
          <el-tag :type="levelTagType(detail?.level)" effect="light" round size="small">{{ detail?.levelName || levelName(detail?.level) }}</el-tag>
        </el-form-item>
        <el-form-item label="目标等级">
          <el-select v-model="levelEditValue" style="width: 100%">
            <el-option label="普通" :value="1" />
            <el-option label="银卡" :value="2" />
            <el-option label="金卡" :value="3" />
            <el-option label="钻石" :value="4" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelEditVisible = false">取消</el-button>
        <el-button type="primary" :loading="levelSaving" @click="submitLevelEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="会员详情" size="520px" :close-on-press-escape="true">
      <div v-loading="detailLoading">
        <div class="detail-head">
          <el-avatar :size="52" class="m-avatar lg">{{ detail?.name?.charAt(0) }}</el-avatar>
          <div>
            <div class="detail-name">
              {{ detail?.name }}
              <el-tag :type="levelTagType(detail?.level)" effect="light" round size="small">{{ detail?.levelName || levelName(detail?.level) }}</el-tag>
              <el-button link type="primary" size="small" @click="openLevelEdit">改等级</el-button>
            </div>
            <div class="m-phone">{{ detail?.phone }}</div>
          </div>
        </div>
        <div class="detail-stats">
          <div class="ds-item">
            <div class="ds-val">¥{{ formatMoney(detail?.balance) }}</div>
            <div class="ds-label">储值余额</div>
          </div>
          <div class="ds-item">
            <div class="ds-val">{{ detail?.points ?? 0 }}</div>
            <div class="ds-label">
              积分
              <el-button link type="primary" size="small" @click="openPointsAdjust">调整</el-button>
            </div>
          </div>
          <div class="ds-item">
            <div class="ds-val">{{ detail?.consumeCount ?? 0 }}</div>
            <div class="ds-label">消费次数</div>
          </div>
          <div class="ds-item">
            <div class="ds-val">¥{{ formatMoney(detail?.totalAmount) }}</div>
            <div class="ds-label">累计消费</div>
          </div>
        </div>

        <!-- 会员画像 -->
        <div class="profile-section x-card" v-loading="profileLoading">
          <div class="profile-title">
            <el-icon><DataAnalysis /></el-icon>
            <span>会员画像</span>
          </div>
          <div class="profile-content">
            <div class="score-ring-wrap">
              <div class="ring" :style="{ '--p': consumeScoreColor }">
                <svg viewBox="0 0 64 64">
                  <circle cx="32" cy="32" r="27" stroke="rgba(108,120,108,0.12)" stroke-width="6" fill="none" />
                  <circle
                    cx="32" cy="32" r="27"
                    :stroke="consumeScoreColor"
                    stroke-width="6" fill="none"
                    :stroke-dasharray="2 * Math.PI * 27"
                    :stroke-dashoffset="ringOffset"
                    stroke-linecap="round"
                    transform="rotate(-90 32 32)"
                  />
                </svg>
                <div class="ring-num">
                  <div class="ring-val val">{{ profile?.consumeScore ?? 0 }}</div>
                  <div class="ring-tip">消费力</div>
                </div>
              </div>
              <div class="profile-meta">
                <div class="meta-row">
                  <span class="meta-label">活跃度</span>
                  <span class="meta-val val">{{ profile?.activeScore ?? 0 }} / 100</span>
                </div>
                <div class="meta-row">
                  <span class="meta-label">生命周期</span>
                  <span class="meta-val">{{ profile?.lifecycle || '—' }}</span>
                </div>
                <div v-if="profile?.nextActionHint" class="hint">
                  <el-icon><MagicStick /></el-icon>
                  <span>{{ profile.nextActionHint }}</span>
                </div>
              </div>
            </div>
            <div v-if="profileTrend.length" class="profile-trend">
              <div class="trend-title">最近 30 天消费</div>
              <div ref="trendEl" class="trend-host"></div>
            </div>
          </div>
        </div>

        <el-descriptions :column="1" border size="small" class="detail-desc">
          <el-descriptions-item label="性别">{{ genderText(detail?.gender) }}</el-descriptions-item>
          <el-descriptions-item label="生日">{{ formatDate(detail?.birthday) }}</el-descriptions-item>
          <el-descriptions-item label="最近消费">{{ formatDateTime(detail?.lastConsumeAt) }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDateTime(detail?.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 标签管理 -->
        <div class="detail-section">
          <div class="ds-title">
            <span>标签</span>
            <el-button link type="primary" size="small" @click="openTagEdit">编辑</el-button>
          </div>
          <div v-if="tagEditing" class="tag-edit">
            <el-select v-model="tagEditValue" multiple filterable allow-create default-first-option placeholder="输入回车添加" style="width: 100%">
              <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
            </el-select>
            <div class="tag-edit-actions">
              <el-button size="small" @click="tagEditing = false">取消</el-button>
              <el-button size="small" type="primary" @click="saveTags">保存</el-button>
            </div>
          </div>
          <div v-else>
            <el-tag v-for="t in detail?.tags" :key="t" size="small" class="m-tag" effect="plain">{{ t }}</el-tag>
            <span v-if="!detail?.tags?.length" class="muted">暂无标签</span>
          </div>
        </div>

        <!-- 持有优惠券 -->
        <div class="detail-section">
          <div class="ds-title"><span>持有优惠券</span></div>
          <el-table :data="memberCoupons" size="small" max-height="180">
            <el-table-column label="券名称" prop="couponName" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="couponStatusType(row.status)" effect="light">{{ couponStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="到期" width="100">
              <template #default="{ row }">{{ formatDate(row.expireAt) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 消费/资金流水 -->
        <div class="detail-section">
          <div class="ds-title">
            <span>资金流水</span>
            <el-button link type="primary" size="small" :loading="txLoading" @click="loadMoreTx">查看更多</el-button>
          </div>
          <el-table :data="transactions" size="small" max-height="220">
            <el-table-column label="时间" width="135">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="类型" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="txTypeType(row.type)" effect="plain">{{ txTypeText(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="90" align="right">
              <template #default="{ row }">
                <span :class="row.amount >= 0 ? 'pos' : 'neg'">{{ row.amount >= 0 ? '+' : '' }}¥{{ formatMoney(Math.abs(row.amount)) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
    </el-drawer>

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
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Plus, Search, RefreshLeft, User, Download, Upload,
  PriceTag, Medal, DataAnalysis, MagicStick
} from '@element-plus/icons-vue'
import * as echarts from '@/utils/echarts'
import { membersApi, storesApi } from '@/api'
import { formatMoney, formatDateTime, formatDate, yuanToFen } from '@/utils/format'
import type { Member, MemberProfile, Store, Transaction, CouponRecord } from '@/types'

const loading = ref(false)
const list = ref<Member[]>([])
const total = ref(0)
const stores = ref<Store[]>([])

const memberSlogan = [
  '把每一位会员，都当作一段值得悉心维护的关系',
  '名单是死的，会员是活的 — 用心才能长久',
  '把会员日常，谱成一段可被回望的故事'
][Math.floor(Math.random() * 3)]
const tagOptions = ref<string[]>(['VIP', '高频', '待回访', '高客单', '流失风险'])
const compact = ref(false)

const selected = ref<Member[]>([])
const tableRef = ref()

const query = reactive({
  keyword: '',
  level: undefined as number | undefined,
  tag: '',
  storeIds: [] as number[],
  page: 1,
  size: 20
})

async function loadList() {
  loading.value = true
  try {
    const params: any = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.level) params.level = query.level
    if (query.tag) params.tag = query.tag
    if (query.storeIds.length) params.storeIds = query.storeIds.join(',')
    const res = await membersApi.list(params)
    list.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}
function onSearch() {
  query.page = 1
  loadList()
}
function onReset() {
  query.keyword = ''
  query.level = undefined
  query.tag = ''
  query.storeIds = []
  query.page = 1
  loadList()
}
function onSelectionChange(rows: Member[]) {
  selected.value = rows
}
function clearSelection() {
  selected.value = []
  tableRef.value?.clearSelection()
}

function levelName(level?: number) {
  return ({ 1: '普通', 2: '银卡', 3: '金卡', 4: '钻石' } as any)[level || 1] || '普通'
}
// 按规范: 1=info(灰), 2=default(白), 3=warning(哑金), 4=success(鼠尾草)
function levelTagType(level?: number) {
  return ({ 1: 'info', 2: '', 3: 'warning', 4: 'success' } as any)[level || 1] || 'info'
}
function genderText(g?: string) {
  return ({ M: '男', F: '女', U: '未知' } as any)[g || 'U'] || '未知'
}

// ============ 导入 / 导出 ============
const fileInputRef = ref<HTMLInputElement>()
const importResultVisible = ref(false)
const importResult = ref<any>(null)
function onImportClick() {
  fileInputRef.value?.click()
}
async function onFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  try {
    const res = await membersApi.import(fd)
    importResult.value = res
    if (res.failed > 0 || res.errors?.length) {
      importResultVisible.value = true
    } else {
      ElMessage.success(`导入完成：成功 ${res.success} 条`)
    }
    loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '导入失败')
  } finally {
    target.value = ''
  }
}
async function onExport() {
  try {
    const params: any = {}
    if (query.keyword) params.keyword = query.keyword
    if (query.level) params.level = query.level
    if (query.tag) params.tag = query.tag
    const blob: any = await membersApi.export(params)
    if (!blob) { ElMessage.error('导出失败：无数据'); return }
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `会员列表-${Date.now()}.csv`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error(e?.message || '导出失败')
  }
}

// ============ 新增/编辑 ============
const formVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: 0,
  name: '',
  phone: '',
  gender: 'U',
  birthday: '',
  storeIds: [] as number[],
  tags: [] as string[],
  remark: ''
})
const formRules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}
function openCreate() {
  editing.value = false
  Object.assign(form, { id: 0, name: '', phone: '', gender: 'U', birthday: '', storeIds: [], tags: [], remark: '' })
  formVisible.value = true
}
function openEdit(row: Member) {
  editing.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    phone: row.phone,
    gender: row.gender || 'U',
    birthday: row.birthday || '',
    storeIds: row.storeIds ? [...row.storeIds] : [],
    tags: row.tags ? [...row.tags] : [],
    remark: (row as any).remark || ''
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
      phone: form.phone,
      gender: form.gender,
      birthday: form.birthday || undefined,
      storeIds: form.storeIds,
      tags: form.tags,
      remark: form.remark
    }
    if (editing.value) {
      await membersApi.update(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await membersApi.create(payload)
      ElMessage.success('已新增')
    }
    formVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}
async function onRemove(row: Member) {
  try {
    await ElMessageBox.confirm(`确认删除会员「${row.name}」？`, '提示', {
      type: 'warning',
      closeOnPressEscape: true
    })
  } catch { return }
  await membersApi.remove(row.id)
  ElMessage.success('已删除')
  loadList()
}

// ============ 储值充值 ============
const rechargeVisible = ref(false)
const rechargeTarget = ref<Member>()
const recharging = ref(false)
const rechargeFormRef = ref<FormInstance>()
const rechargeForm = reactive({
  amountYuan: 0,
  giftYuan: 0,
  payMethod: 'WECHAT',
  remark: ''
})
const rechargeRules: FormRules = {
  amountYuan: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  payMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}
function openRecharge(row: Member) {
  rechargeTarget.value = row
  rechargeForm.amountYuan = 0
  rechargeForm.giftYuan = 0
  rechargeForm.payMethod = 'WECHAT'
  rechargeForm.remark = ''
  rechargeVisible.value = true
}
async function submitRecharge() {
  const valid = await rechargeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!rechargeTarget.value) return
  recharging.value = true
  try {
    const res = await membersApi.recharge(rechargeTarget.value.id, {
      amount: yuanToFen(rechargeForm.amountYuan),
      gift: yuanToFen(rechargeForm.giftYuan),
      payMethod: rechargeForm.payMethod,
      remark: rechargeForm.remark
    })
    ElMessage.success(`充值成功，当前余额 ¥${formatMoney(res.balance)}`)
    rechargeVisible.value = false
    loadList()
    if (detailVisible.value && detail.value?.id === rechargeTarget.value.id) {
      detail.value = { ...detail.value, balance: res.balance } as Member
    }
  } finally {
    recharging.value = false
  }
}

// ============ 批量操作 ============
const batchTagVisible = ref(false)
const batchTagValue = ref<string[]>([])
const batchTagMode = ref<'cover' | 'append'>('cover')
const batchLevelVisible = ref(false)
const batchLevelValue = ref(1)
const batchSaving = ref(false)
function openBatchTag() { batchTagValue.value = []; batchTagMode.value = 'cover'; batchTagVisible.value = true }
function openBatchLevel() { batchLevelValue.value = 1; batchLevelVisible.value = true }
async function submitBatchTag() {
  if (!batchTagValue.value.length) { ElMessage.warning('请选择标签'); return }
  batchSaving.value = true
  try {
    await membersApi.batchTags({ memberIds: selected.value.map(s => s.id), tags: batchTagValue.value })
    ElMessage.success(`已对 ${selected.value.length} 位会员设置标签`)
    batchTagVisible.value = false
    clearSelection()
    loadList()
  } finally { batchSaving.value = false }
}
async function submitBatchLevel() {
  batchSaving.value = true
  try {
    await membersApi.batchLevel({ memberIds: selected.value.map(s => s.id), level: batchLevelValue.value })
    ElMessage.success(`已对 ${selected.value.length} 位会员调整等级`)
    batchLevelVisible.value = false
    clearSelection()
    loadList()
  } finally { batchSaving.value = false }
}

// ============ 详情抽屉 ============
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<Member>()
const transactions = ref<Transaction[]>([])
const memberCoupons = ref<CouponRecord[]>([])
const txQuery = reactive({ page: 1, size: 10 })
const txTotal = ref(0)
const txLoading = ref(false)

// 画像
const profileLoading = ref(false)
const profile = ref<MemberProfile>()
const profileTrend = computed(() => profile.value?.trend30d || [])
const consumeScoreColor = computed(() => {
  const s = profile.value?.consumeScore ?? 0
  if (s >= 70) return 'var(--success)'
  if (s >= 40) return 'var(--primary-action)'
  return 'var(--warning)'
})
const ringOffset = computed(() => {
  const s = profile.value?.consumeScore ?? 0
  const C = 2 * Math.PI * 27
  return C * (1 - s / 100)
})
const trendEl = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null

async function openDetail(row: Member) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = row
  transactions.value = []
  memberCoupons.value = []
  profile.value = undefined
  txQuery.page = 1
  tagEditing.value = false
  try {
    const [d, tx, cp, pr] = await Promise.all([
      membersApi.detail(row.id),
      membersApi.transactions(row.id, { page: txQuery.page, size: txQuery.size }),
      membersApi.coupons(row.id).catch(() => []),
      loadProfile(row.id)
    ])
    detail.value = d
    transactions.value = tx.list || []
    txTotal.value = tx.total || 0
    memberCoupons.value = (cp as any) || []
  } finally {
    detailLoading.value = false
  }
}
async function loadProfile(id: number) {
  profileLoading.value = true
  try {
    profile.value = await membersApi.profile(id)
    await nextTick()
    renderProfileTrend()
  } catch { /* 容错 */ }
  finally { profileLoading.value = false }
}
function renderProfileTrend() {
  if (!trendEl.value || !profileTrend.value.length) return
  if (!trendChart) trendChart = echarts.init(trendEl.value)
  trendChart.setOption({
    grid: { left: 30, right: 8, top: 8, bottom: 18 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category', data: profileTrend.value.map(d => d.date.slice(5)),
      axisLine: { show: false }, axisTick: { show: false },
      axisLabel: { color: '#8a8e85', fontSize: 10, interval: 6 }
    },
    yAxis: { type: 'value', show: false },
    series: [{
      type: 'bar', data: profileTrend.value.map(d => d.amount),
      barWidth: '60%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#6f94b8' }, { offset: 1, color: 'rgba(111,148,184,0.3)' }
        ]), borderRadius: [2, 2, 0, 0]
      }
    }]
  }, true)
}
async function loadMoreTx() {
  if (!detail.value || txLoading.value) return
  if (transactions.value.length >= txTotal.value) { ElMessage.info('已加载全部流水'); return }
  txLoading.value = true
  try {
    txQuery.page += 1
    const res = await membersApi.transactions(detail.value.id, { page: txQuery.page, size: txQuery.size })
    transactions.value = transactions.value.concat(res.list || [])
    if (res.total != null) txTotal.value = res.total
  } catch { /* 拦截器处理 */ } finally {
    txLoading.value = false
  }
}

// ============ 标签编辑 ============
const tagEditing = ref(false)
const tagEditValue = ref<string[]>([])
function openTagEdit() {
  tagEditValue.value = detail.value?.tags ? [...detail.value.tags] : []
  tagEditing.value = true
}
async function saveTags() {
  if (!detail.value) return
  await membersApi.setTags(detail.value.id, tagEditValue.value)
  ElMessage.success('标签已更新')
  detail.value = { ...detail.value, tags: [...tagEditValue.value] }
  tagEditing.value = false
  loadList()
}

// ============ 积分调整 ============
const pointsVisible = ref(false)
const pointsSaving = ref(false)
const pointsFormRef = ref<FormInstance>()
const pointsForm = reactive({
  mode: 'add' as 'add' | 'sub',
  amount: 10,
  reason: ''
})
const pointsRules: FormRules = {
  amount: [{ required: true, message: '请输入积分数', trigger: 'blur' }],
  reason: [{ required: true, message: '请填写调整原因', trigger: 'blur' }, { min: 2, message: '至少 2 个字', trigger: 'blur' }]
}
function openPointsAdjust() {
  pointsForm.mode = 'add'
  pointsForm.amount = 10
  pointsForm.reason = ''
  pointsVisible.value = true
}
async function submitPoints() {
  if (!detail.value) return
  const valid = await pointsFormRef.value?.validate().catch(() => false)
  if (!valid) return
  pointsSaving.value = true
  try {
    const delta = pointsForm.mode === 'add' ? pointsForm.amount : -pointsForm.amount
    const res = await membersApi.adjustPoints(detail.value.id, { delta, reason: pointsForm.reason.trim() })
    ElMessage.success(`积分已${pointsForm.mode === 'add' ? '增加' : '扣减'} ${pointsForm.amount}`)
    detail.value = { ...detail.value, points: res.points } as Member
    pointsVisible.value = false
    loadList()
  } finally {
    pointsSaving.value = false
  }
}

// ============ 单会员等级编辑 ============
const levelEditVisible = ref(false)
const levelSaving = ref(false)
const levelEditValue = ref(1)
function openLevelEdit() {
  levelEditValue.value = detail.value?.level || 1
  levelEditVisible.value = true
}
async function submitLevelEdit() {
  if (!detail.value) return
  levelSaving.value = true
  try {
    const res = await membersApi.setLevel(detail.value.id, levelEditValue.value)
    ElMessage.success('等级已更新')
    detail.value = { ...detail.value, level: res.level, levelName: res.levelName } as Member
    levelEditVisible.value = false
    loadList()
  } finally {
    levelSaving.value = false
  }
}

// 流水/券状态文案
function txTypeText(t: string) {
  return ({ RECHARGE: '充值', CONSUME: '消费', GIFT: '赠送', REFUND: '退款' } as any)[t] || t
}
function txTypeType(t: string) {
  return ({ RECHARGE: 'success', CONSUME: 'warning', GIFT: 'info', REFUND: 'danger' } as any)[t] || 'info'
}
function couponStatusText(s: string) {
  return ({ UNUSED: '未使用', USED: '已使用', EXPIRED: '已过期' } as any)[s] || s
}
function couponStatusType(s: string) {
  return ({ UNUSED: 'success', USED: 'info', EXPIRED: 'danger' } as any)[s] || 'info'
}

const route = useRoute()

onMounted(async () => {
  stores.value = await storesApi.list().catch(() => [])
  await loadList()
  // 从 Wallet 跳转过来时自动打开会员详情
  const mid = route.query.id
  if (mid) {
    const id = Number(mid)
    if (!isNaN(id)) {
      const row = list.value.find((m: any) => m.id === id)
      if (row) {
        openDetail(row)
      } else {
        // 列表里没找到，尝试直接查详情
        try {
          const d = await membersApi.detail(id)
          if (d) openDetail(d as Member)
        } catch { /* 会员可能不存在 */ }
      }
    }
  }
})

onBeforeUnmount(() => {
  trendChart?.dispose()
})
</script>

<style scoped>
.table-wrap { padding: 16px 18px; }

/* 批量操作栏 */
.batch-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 18px;
  margin-bottom: 12px;
  background: rgba(111, 148, 184, 0.06);
  border: 1px solid rgba(111, 148, 184, 0.20);
}
.batch-text { font-size: 13px; color: var(--ink); font-weight: 500; }
.batch-actions { display: flex; gap: 8px; }
.slide-down-enter-active, .slide-down-leave-active { transition: all 0.2s ease-out; }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; transform: translateY(-6px); }

.filter-spacer { flex: 1; }
.cell-member { display: flex; align-items: center; gap: 10px; }
.m-avatar {
  background: var(--primary-action);
  color: #fff;
  font-size: 13px;
  flex-shrink: 0;
}
.m-avatar.lg { font-size: 20px; }
.m-name { font-size: 13px; color: var(--ink); font-weight: 500; }
.m-phone { font-size: 12px; color: var(--muted); margin-top: 2px; }
.m-tag { margin-right: 4px; }
.muted { color: var(--muted); }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
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
.recharge-member {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; background: var(--surface-2); border-radius: 8px;
}
.form-tip { font-size: 12px; color: var(--muted); line-height: 1.4; }

.detail-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.detail-name {
  font-size: 16px; font-weight: 600; color: var(--ink);
  display: flex; align-items: center; gap: 8px;
}
.detail-stats {
  display: grid; grid-template-columns: repeat(2, 1fr);
  gap: 10px; margin-bottom: 16px;
}
.ds-item {
  background: var(--surface-2); border: 1px solid var(--card-border);
  border-radius: 8px; padding: 10px 12px; text-align: center;
}
.ds-val { font-size: 16px; font-weight: 600; color: var(--ink); }
.ds-label { font-size: 12px; color: var(--muted); margin-top: 2px; }
.detail-desc { margin-bottom: 16px; }
.detail-section { margin-top: 18px; }
.ds-title {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 13px; font-weight: 600; color: var(--ink); margin-bottom: 8px;
}
.tag-edit-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
.pos { color: var(--success); }
.neg { color: var(--danger); }

/* 会员画像 */
.profile-section { padding: 14px 16px; margin-bottom: 14px; }
.profile-title {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; font-weight: 600; color: var(--ink);
  margin-bottom: 12px;
}
.profile-title .el-icon { color: var(--primary-action); }
.profile-content { display: flex; flex-direction: column; gap: 14px; }
.score-ring-wrap { display: flex; align-items: center; gap: 18px; }
.ring { position: relative; width: 80px; height: 80px; flex-shrink: 0; }
.ring svg { width: 100%; height: 100%; }
.ring-num {
  position: absolute; inset: 0;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
}
.ring-val { font-size: 18px; font-weight: 600; color: var(--ink); line-height: 1.1; }
.ring-tip { font-size: 10px; color: var(--muted); margin-top: 2px; }
.profile-meta { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.meta-row {
  display: flex; justify-content: space-between;
  font-size: 12.5px;
}
.meta-label { color: var(--muted); }
.meta-val { color: var(--ink); font-weight: 500; }
.hint {
  display: flex; align-items: flex-start; gap: 4px;
  font-size: 12px; color: var(--primary-action);
  background: var(--primary-action-soft);
  padding: 6px 8px;
  border-radius: 6px;
  margin-top: 4px;
  line-height: 1.5;
}
.hint .el-icon { flex-shrink: 0; margin-top: 1px; }
.profile-trend { padding-top: 6px; border-top: 1px dashed var(--line); }
.trend-title { font-size: 12px; color: var(--muted); margin-bottom: 6px; }
.trend-host { width: 100%; height: 80px; }
</style>
