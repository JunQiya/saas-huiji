<template>
  <div class="page recharge">
    <NavBar title="余额充值" back />

    <div class="page-padding">
      <!-- 余额卡 -->
      <div class="balance-card">
        <div class="bc-label">当前储值余额</div>
        <div class="bc-value">
          <span class="unit">¥</span><span class="val">{{ yuan(balance) }}</span>
        </div>
        <div class="bc-tip">充值到账，余额长期有效</div>
      </div>

      <!-- 快捷金额 -->
      <div class="section-title"><span>选择金额</span></div>
      <div class="amount-grid">
        <div
          v-for="opt in amountOptions"
          :key="opt.amount"
          class="amount-cell"
          :class="{ active: selectedAmount === opt.amount }"
          @click="selectedAmount = opt.amount"
        >
          <div class="ac-val"><span class="ac-unit">¥</span>{{ opt.amount }}</div>
          <div v-if="opt.gift > 0" class="ac-gift">送 ¥{{ opt.gift }}</div>
          <div v-else class="ac-gift empty">实充实到</div>
        </div>
        <div class="amount-cell custom" :class="{ active: customAmount > 0 && selectedAmount === customAmount }" @click="focusCustom">
          <div class="ac-val custom-val">
            <span class="ac-unit">¥</span>
            <input
              ref="customInput"
              v-model.number="customAmount"
              type="number"
              min="1"
              max="50000"
              placeholder="自定义"
              class="custom-input"
              @focus="selectedAmount = customAmount"
              @input="onCustomInput"
            />
          </div>
          <div class="ac-gift empty">输入金额</div>
        </div>
      </div>

      <!-- 已选赠礼 -->
      <div v-if="giftForSelected > 0" class="gift-banner">
        <van-icon name="gift-o" size="16" />
        <span>充值 ¥{{ currentAmount }} 立赠 ¥{{ giftForSelected }}</span>
      </div>

      <!-- 支付方式 -->
      <div class="section-title"><span>支付方式</span></div>
      <div class="pay-card ui-card">
        <div class="pay-row">
          <div class="pay-icon"><span class="pay-wechat">微</span></div>
          <div class="pay-info">
            <div class="pay-name">微信支付</div>
            <div class="pay-sub">安全快捷{{ isDemo ? ' · 演示环境确认后直接到账' : '' }}</div>
          </div>
          <van-icon name="success" class="pay-check" />
        </div>
      </div>

      <!-- 充值规则 -->
      <div v-if="rules.length" class="section-title">
        <span>充值活动</span>
        <span class="st-tip">多充多送</span>
      </div>
      <div v-if="rules.length" class="rules-card ui-card">
        <div v-for="(r, i) in rules" :key="i" class="rule-row">
          <span class="rr-amt">充 ¥{{ yuan(r.recharge) }}</span>
          <van-icon name="arrow" size="12" color="var(--muted)" />
          <span class="rr-gift">送 ¥{{ yuan(r.gift) }}</span>
        </div>
      </div>

      <!-- 提交 -->
      <button class="submit-btn" :disabled="!canSubmit || submitting" @click="onSubmit">
        <template v-if="submitting">
          <van-loading size="16" color="#fff" class="btn-loading" />
          <span>充值中…</span>
        </template>
        <template v-else>
          <span>确认充值 ¥{{ currentAmount || 0 }}</span>
          <van-icon name="arrow" size="14" />
        </template>
      </button>
      <div class="agree-tip">充值即同意《储值协议》· 余额不可提现不可转让</div>
    </div>

    <!-- 充值成功弹窗 -->
    <van-popup v-model:show="successVisible" position="center" round :close-on-click-overlay="false">
      <div class="success-pop">
        <div class="sp-icon">
          <van-icon name="success" size="30" />
        </div>
        <div class="sp-title">充值成功</div>
        <div class="sp-amount">
          <span class="unit">¥</span><span class="val">{{ successAmount }}</span>
        </div>
        <div v-if="successGift > 0" class="sp-gift">已到账，其中含赠送 ¥{{ successGift }}</div>
        <div v-else class="sp-gift">已到账，可立即消费</div>
        <button class="sp-btn" @click="onSuccessDone">完成</button>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { h5Api } from '@/api/h5'
import { useMemberStore } from '@/stores/member'
import NavBar from '@/components/NavBar.vue'
import { fenToYuan } from '@/utils/format'

const router = useRouter()
const memberStore = useMemberStore()
const balance = computed(() => memberStore.memberInfo?.balance || 0)

const isDemo = !import.meta.env.PROD
const MIN_AMOUNT = 1
const MAX_AMOUNT = 50000

const rules = ref<{ recharge: number; gift: number }[]>([])
const selectedAmount = ref(0)
const customAmount = ref(0)
const submitting = ref(false)
const customInput = ref<HTMLInputElement>()
const successVisible = ref(false)
const successAmount = ref(0)
const successGift = ref(0)

// 快捷档位：充值规则档位 + 常用金额去重
const amountOptions = computed(() => {
  const set = new Map<number, number>()
  for (const r of rules.value) set.set(Math.round(r.recharge / 100), Math.round(r.gift / 100))
  for (const a of [100, 200, 500]) if (!set.has(a)) set.set(a, 0)
  return [...set.entries()].map(([amount, gift]) => ({ amount, gift }))
})

// 当前选择金额（快捷档位或自定义）
const currentAmount = computed(() => {
  if (selectedAmount.value === customAmount.value && customAmount.value) return customAmount.value
  return selectedAmount.value
})

// 按规则计算当前选择的赠送金额
const giftForSelected = computed(() => {
  const amt = currentAmount.value
  if (!amt || amt <= 0) return 0
  const fen = Math.round(amt * 100)
  let gift = 0
  for (const r of rules.value) {
    if (fen >= r.recharge) gift = Math.max(gift, r.gift)
  }
  return Math.round(gift / 100)
})

const canSubmit = computed(
  () => currentAmount.value > 0 && currentAmount.value <= MAX_AMOUNT && !submitting.value
)

function focusCustom() {
  if (selectedAmount.value !== customAmount.value) {
    selectedAmount.value = customAmount.value || 0
  }
  nextTick(() => customInput.value?.focus())
}

// 自定义金额限位
function onCustomInput() {
  if (customAmount.value > MAX_AMOUNT) customAmount.value = MAX_AMOUNT
  if (customAmount.value < 0) customAmount.value = 0
}

async function loadRules() {
  try {
    const data = await h5Api.walletRules()
    rules.value = (data || []).sort((a, b) => a.recharge - b.recharge)
  } catch {/* 无规则也可充值 */}
  // 直接进入本页时刷新余额(避免显示旧值)
  try {
    const p = await h5Api.profile()
    memberStore.setMember(p)
  } catch {/* 静默 */}
}

async function onSubmit() {
  const amt = currentAmount.value
  if (!amt || amt <= 0) { showToast('请输入充值金额'); return }
  if (amt > MAX_AMOUNT) { showToast(`单次充值不超过 ¥${MAX_AMOUNT}`); return }

  submitting.value = true
  try {
    // 1. 创建充值单
    const order = await h5Api.createRechargeOrder({ amount: Math.round(amt * 100), payMethod: 'WECHAT' })
    // 2. 支付（演示环境模拟成功；生产环境走微信支付后由回调入账）
    const res = await h5Api.payRecharge(order.rechargeOrderId)
    if (res?.status !== 'SUCCESS') {
      showToast('支付未完成，请稍后在订单中查看')
      return
    }
    // 3. 刷新会员资料（余额同步到全局状态）
    const p = await h5Api.profile()
    memberStore.setMember(p)
    const totalGift = res.gift ?? order.gift ?? 0
    const afterBalance = res.balanceAfter != null ? Math.round(res.balanceAfter / 100) : (balance.value + amt + totalGift) / 100
    successAmount.value = afterBalance
    successGift.value = totalGift
    successVisible.value = true
  } catch (e: any) {
    showToast(e?.message || '充值失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}

function onSuccessDone() {
  successVisible.value = false
  router.back()
}

const yuan = fenToYuan

onMounted(loadRules)
onActivated(loadRules)
</script>

<style scoped>
.recharge { padding-bottom: 40px; }

/* 余额卡：品牌渐变 + 星光点缀 */
.balance-card {
  position: relative;
  margin: 4px 0 6px;
  padding: 20px 20px;
  background:
    radial-gradient(circle at 88% 12%, rgba(255, 255, 255, 0.20), transparent 52%),
    radial-gradient(circle at 12% 100%, rgba(255, 255, 255, 0.10), transparent 46%),
    linear-gradient(135deg, var(--brand-deep), var(--brand));
  border-radius: var(--r-lg);
  color: #fff;
  overflow: hidden;
}
.balance-card::before,
.balance-card::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
}
.balance-card::before {
  width: 4px; height: 4px; top: 22px; right: 34%;
  box-shadow:
    14px 26px 0 -1px rgba(255, 255, 255, 0.16),
    44px 10px 0 -1px rgba(255, 255, 255, 0.13);
}
.balance-card::after {
  width: 3px; height: 3px; bottom: 16px; left: 26%;
  box-shadow:
    18px -18px 0 -1px rgba(255, 255, 255, 0.14),
    52px -6px 0 -1px rgba(255, 255, 255, 0.12);
}
.bc-label {
  position: relative; z-index: 1;
  font-family: var(--font-serif);
  font-size: 12px; opacity: 0.82;
  letter-spacing: 0.12em;
}
.bc-value {
  position: relative; z-index: 1;
  margin-top: 8px;
  font-family: var(--font-num);
  font-size: 34px; font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.bc-value .unit { font-size: 16px; margin-right: 3px; opacity: 0.85; }
.bc-tip {
  position: relative; z-index: 1;
  font-size: 11px; opacity: 0.72;
  margin-top: 6px; letter-spacing: 0.06em;
}

/* 金额档位 */
.amount-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.amount-cell {
  position: relative;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  padding: 13px 8px;
  text-align: center;
  cursor: pointer;
  transition: all var(--dur) var(--ease-out);
}
.amount-cell:active { transform: scale(0.97); }
.amount-cell.active {
  background: var(--brand-soft);
  border-color: var(--brand);
  box-shadow: 0 2px 10px var(--brand-glow);
}
.amount-cell.active::after {
  content: '✓';
  position: absolute; top: 5px; right: 5px;
  width: 15px; height: 15px;
  border-radius: 50%;
  background: var(--brand-deep);
  color: #fff;
  font-size: 9.5px; font-weight: 600;
  line-height: 15px;
  font-family: var(--font-num);
}
.ac-val {
  font-family: var(--font-num);
  font-size: 17px; font-weight: 600;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.ac-unit { font-size: 12px; margin-right: 1px; opacity: 0.75; }
.amount-cell.active .ac-val { color: var(--brand-deep); }
.ac-gift {
  font-size: 10.5px;
  color: var(--success-deep);
  margin-top: 4px;
  letter-spacing: 0.02em;
}
.ac-gift.empty { color: var(--muted-2); }
.custom-input {
  width: 62px;
  border: none;
  background: transparent;
  outline: none;
  font-family: var(--font-num);
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  text-align: left;
  -moz-appearance: textfield;
}
.custom-input::-webkit-outer-spin-button,
.custom-input::-webkit-inner-spin-button { -webkit-appearance: none; }
.custom-input::placeholder { color: var(--muted-2); font-weight: 400; font-size: 12.5px; }

/* 赠礼提示 */
.gift-banner {
  display: flex; align-items: center; gap: 6px;
  margin-top: 14px;
  padding: 10px 14px;
  background: var(--success-soft);
  border: 1px dashed var(--success);
  border-radius: var(--r);
  color: var(--success-deep);
  font-size: 13px;
  letter-spacing: 0.04em;
}

/* 支付方式 */
.pay-card { padding: 4px 16px; }
.pay-row {
  display: flex; align-items: center; gap: 12px;
  padding: 13px 0;
}
.pay-icon {
  width: 34px; height: 34px;
  border-radius: 10px;
  background: #07c160;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.pay-wechat {
  color: #fff;
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 500;
}
.pay-info { flex: 1; min-width: 0; }
.pay-name { font-size: 14px; color: var(--ink); font-weight: 500; letter-spacing: 0.02em; }
.pay-sub { font-size: 11px; color: var(--muted); margin-top: 2px; letter-spacing: 0.02em; }
.pay-check { color: var(--success); font-size: 18px; }

/* 充值规则 */
.rules-card { padding: 10px 16px; }
.rule-row {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 13px;
  color: var(--ink-2);
}
.rule-row:last-of-type { border-bottom: none; }
.rr-amt { font-family: var(--font-num); }
.rr-gift { color: var(--success-deep); font-family: var(--font-num); font-weight: 500; }

/* 提交按钮 */
.submit-btn {
  width: 100%;
  height: 46px;
  margin-top: 20px;
  border: none;
  border-radius: 999px;
  background: var(--brand-deep);
  color: #fff;
  font-size: 15px;
  font-family: var(--font-serif);
  letter-spacing: 0.12em;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  transition: transform var(--dur) var(--ease-out), opacity var(--dur) var(--ease-out), box-shadow var(--dur) var(--ease-out);
}
.submit-btn:active:not(:disabled) { transform: scale(0.98); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-loading { display: inline-flex; }
.agree-tip {
  text-align: center;
  font-size: 11px; color: var(--muted);
  margin-top: 10px;
  letter-spacing: 0.04em;
}

/* 充值成功弹窗 */
.success-pop {
  width: 280px;
  padding: 30px 24px 22px;
  text-align: center;
  background: var(--surface);
  border-radius: var(--r-lg);
}
.sp-icon {
  width: 60px; height: 60px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: var(--success-soft);
  color: var(--success-deep);
  display: flex; align-items: center; justify-content: center;
  animation: pop-in 0.45s var(--ease-out);
}
.sp-title {
  font-family: var(--font-serif);
  font-size: 17px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.14em;
}
.sp-amount {
  margin-top: 16px;
  font-family: var(--font-num);
  font-size: 36px; font-weight: 600;
  color: var(--brand-deep);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.01em;
  line-height: 1.1;
}
.sp-amount .unit { font-size: 18px; margin-right: 2px; opacity: 0.8; }
.sp-gift {
  font-family: var(--font-serif);
  font-size: 12px; color: var(--muted);
  margin-top: 10px; letter-spacing: 0.06em;
}
.sp-btn {
  margin-top: 24px;
  width: 100%;
  height: 42px;
  border: none;
  border-radius: 999px;
  background: var(--brand-deep);
  color: #fff;
  font-size: 14px;
  font-family: var(--font-serif);
  letter-spacing: 0.16em;
  cursor: pointer;
  transition: transform var(--dur) var(--ease-out), opacity var(--dur) var(--ease-out);
}
.sp-btn:active { transform: scale(0.98); opacity: 0.88; }

@keyframes pop-in {
  0% { transform: scale(0.3); opacity: 0; }
  60% { transform: scale(1.08); }
  100% { transform: scale(1); opacity: 1; }
}
</style>
