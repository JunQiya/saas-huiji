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
          <div class="ac-val">¥{{ opt.amount }}</div>
          <div v-if="opt.gift" class="ac-gift">送 ¥{{ opt.gift }}</div>
          <div v-else class="ac-gift empty">—</div>
        </div>
        <div class="amount-cell custom" :class="{ active: customAmount > 0 && selectedAmount === customAmount }" @click="focusCustom">
          <div class="ac-val custom-val">
            <input
              ref="customInput"
              v-model.number="customAmount"
              type="number"
              min="0"
              placeholder="自定义"
              class="custom-input"
              @focus="selectedAmount = customAmount"
            />
          </div>
          <div class="ac-gift empty">输入金额</div>
        </div>
      </div>

      <!-- 已选赠礼 -->
      <div v-if="giftForSelected > 0" class="gift-banner">
        <van-icon name="gift-o" size="16" />
        <span>充值 ¥{{ selectedAmount }} 立赠 ¥{{ giftForSelected }}</span>
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
        <div class="rule-tip">演示环境：确认后直接到账</div>
      </div>

      <!-- 提交 -->
      <button class="submit-btn" :disabled="!canSubmit || submitting" @click="onSubmit">
        {{ submitting ? '充值中…' : `确认充值 ¥${selectedAmount || 0}` }}
      </button>
      <div class="agree-tip">充值即同意《储值协议》· 余额不可提现不可转让</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast, showConfirmDialog } from 'vant'
import { h5Api } from '@/api/h5'
import { useMemberStore } from '@/stores/member'
import NavBar from '@/components/NavBar.vue'
import { fenToYuan } from '@/utils/format'

const router = useRouter()
const memberStore = useMemberStore()
const balance = computed(() => memberStore.memberInfo?.balance || 0)

const rules = ref<{ recharge: number; gift: number }[]>([])
const selectedAmount = ref(0)
const customAmount = ref(0)
const submitting = ref(false)
const customInput = ref<HTMLInputElement>()

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

const canSubmit = computed(() => currentAmount.value > 0 && !submitting.value)

function focusCustom() {
  if (selectedAmount.value !== customAmount.value) {
    selectedAmount.value = customAmount.value || 0
  }
  nextTick(() => customInput.value?.focus())
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
  try {
    await showConfirmDialog({
      title: '确认充值',
      message: `确认充值 ¥${amt}${giftForSelected.value > 0 ? `（另赠 ¥${giftForSelected.value}）` : ''}？`,
      confirmButtonText: '确认充值'
    })
  } catch { return }

  submitting.value = true
  try {
    const res = await h5Api.recharge({ amount: Math.round(amt * 100), payMethod: 'WECHAT' })
    // 刷新会员资料（余额同步到全局状态）
    const p = await h5Api.profile()
    memberStore.setMember(p)
    const totalGift = res?.gift ? Math.round(res.gift / 100) : giftForSelected.value
    showSuccessToast(`充值成功${totalGift > 0 ? `，到账赠送 ¥${totalGift}` : ''}`)
    setTimeout(() => router.back(), 900)
  } catch (e: any) {
    showToast(e?.message || '充值失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}

const yuan = fenToYuan

onMounted(loadRules)
onActivated(loadRules)
</script>

<style scoped>
.recharge { padding-bottom: 40px; }

.balance-card {
  margin: 4px 0 6px;
  padding: 20px 20px;
  background: var(--brand);
  border-radius: var(--r-lg);
  color: #fff;
}
.bc-label {
  font-family: var(--font-serif);
  font-size: 12px; opacity: 0.8;
  letter-spacing: 0.12em;
}
.bc-value {
  margin-top: 8px;
  font-family: var(--font-num);
  font-size: 34px; font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.bc-value .unit { font-size: 16px; margin-right: 3px; opacity: 0.85; }
.bc-tip {
  font-size: 11px; opacity: 0.72;
  margin-top: 6px; letter-spacing: 0.06em;
}

.amount-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.amount-cell {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r);
  padding: 12px 8px;
  text-align: center;
  cursor: pointer;
  transition: all var(--dur) var(--ease-out);
}
.amount-cell.active {
  background: var(--brand-soft);
  border-color: var(--brand);
}
.ac-val {
  font-family: var(--font-num);
  font-size: 17px; font-weight: 600;
  color: var(--ink);
}
.amount-cell.active .ac-val { color: var(--brand-deep); }
.ac-gift {
  font-size: 10.5px;
  color: var(--success-deep);
  margin-top: 4px;
  letter-spacing: 0.02em;
}
.ac-gift.empty { color: var(--muted); }
.custom-input {
  width: 100%;
  border: none;
  background: transparent;
  outline: none;
  font-family: var(--font-num);
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  text-align: center;
  -moz-appearance: textfield;
}
.custom-input::placeholder { color: var(--muted-2); font-weight: 400; font-size: 12.5px; }

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
.rule-tip {
  font-size: 11px; color: var(--muted);
  text-align: center; padding: 4px 0 6px;
  letter-spacing: 0.04em;
}

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
  letter-spacing: 0.16em;
  cursor: pointer;
  transition: transform var(--dur) var(--ease-out), opacity var(--dur) var(--ease-out);
}
.submit-btn:active:not(:disabled) { transform: scale(0.98); }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.agree-tip {
  text-align: center;
  font-size: 11px; color: var(--muted);
  margin-top: 10px;
  letter-spacing: 0.04em;
}
</style>
