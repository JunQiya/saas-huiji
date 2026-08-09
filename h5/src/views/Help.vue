<template>
  <div class="page help">
    <NavBar title="帮助中心" back />

    <div class="page-padding">
      <div class="search-wrap">
        <div class="search-box">
          <van-icon name="search" class="search-ic" />
          <input
            v-model="kw"
            type="text"
            placeholder="搜索常见问题"
            class="search-input"
          />
        </div>
      </div>

      <div class="page-tip">有问必答，让每一次到访都安心。</div>

      <div class="block">
        <div class="block-title">
          <span class="dot"></span>常见问题
        </div>
        <div class="ui-card faq-card">
          <div v-if="!filtered.length" class="no-result">
            <div class="nr-icon">
              <van-icon name="search" size="32" />
            </div>
            <div class="nr-text">未找到「{{ kw }}」相关问题</div>
            <div class="nr-sub">试试别的关键词，或联系客服</div>
          </div>
          <div
            v-for="(q, i) in filtered"
            :key="i"
            class="faq-item"
            :class="{ open: active === i }"
          >
            <div class="faq-q" @click="toggle(i)">
              <span class="faq-num">Q{{ pad(i + 1) }}</span>
              <span class="faq-text">{{ q.q }}</span>
              <van-icon :name="active === i ? 'minus' : 'plus'" class="faq-toggle" />
            </div>
            <div v-show="active === i" class="faq-a">
              <span class="faq-num ans">A</span>
              <span class="faq-text ans">{{ q.a }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="contact-zone">
        <div class="cz-title">没找到答案？</div>
        <div class="cz-sub">工作时间 9:00-21:00 · 7x12h 在线</div>
        <div class="cz-btns">
          <button class="cz-btn primary" @click="callPhone">
            <van-icon name="phone-o" /> 电话客服
          </button>
          <button class="cz-btn" @click="onlineChat">
            <van-icon name="chat-o" /> 在线咨询
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { showToast } from 'vant'
import NavBar from '@/components/NavBar.vue'
import { CONTACT } from '@/constants/config'

const kw = ref('')
const active = ref<number | null>(null)

const faqs = [
  { q: '如何领取优惠券？', a: '在「领券中心」中可一键领取，券将自动存入「我的券」中，到期前均可使用。' },
  { q: '储值余额如何使用？', a: '在门店消费时出示会员卡，由收银员核销并扣减储值余额，支付完成后会同步生成消费记录。' },
  { q: '我的券可以送人吗？', a: '券默认绑定本人账户，不支持转赠。会员等级权益、生日券等均为本人专享。' },
  { q: '如何查看消费记录？', a: '进入「我的」-「消费记录」可查看全部充值、消费、退款流水，可按类型筛选。' },
  { q: '如何升级会员等级？', a: '累计消费达到对应等级门槛后，系统将自动升级。等级越高，权益越丰富。' },
  { q: '会员卡上的二维码有何用？', a: '会员卡上的二维码是核销身份凭证，到店出示即可完成会员识别、券核销、储值扣减等操作。' },
  { q: '储值后能退吗？', a: '储值余额支持原路退回，请联系门店或客服，提交申请后 1-3 个工作日内处理。' },
  { q: '收不到短信验证码？', a: '请检查手机信号与短信拦截设置。如仍无法收到，可稍后重试或联系客服。' }
]

const filtered = computed(() => {
  if (!kw.value) return faqs
  return faqs.filter(f => f.q.includes(kw.value) || f.a.includes(kw.value))
})

function pad(n: number) { return n.toString().padStart(2, '0') }
function toggle(i: number) { active.value = active.value === i ? null : i }
function callPhone() { window.location.href = CONTACT.phoneHref }
function onlineChat() { window.location.href = CONTACT.emailHref }
</script>

<style scoped>
.help { padding-bottom: 40px; }

.search-wrap { margin-bottom: 8px; }
.search-box {
  display: flex; align-items: center; gap: 8px;
  height: 38px;
  padding: 0 14px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 999px;
  transition: border-color var(--dur) var(--ease);
}
.search-box:focus-within { border-color: var(--brand); }
.search-ic { color: var(--muted); font-size: 14px; }
.search-input {
  flex: 1;
  border: none; background: transparent; outline: none;
  font-size: 13.5px; color: var(--ink);
  font-family: inherit;
  letter-spacing: 0.02em;
}
.search-input::placeholder { color: var(--muted-2); }

.page-tip {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 0.04em;
  margin-bottom: 14px;
  font-family: 'Songti SC', serif;
  opacity: 0.85;
  padding-left: 2px;
}

.block { margin-top: 18px; }
.block-title {
  font-size: 13px; font-weight: 600; color: var(--ink-2);
  margin-bottom: 8px; padding-left: 2px;
  display: flex; align-items: center; gap: 8px;
  letter-spacing: 0.06em;
}
.block-title .dot { width: 5px; height: 5px; background: var(--brand); border-radius: 50%; }

.faq-card { padding: 0 16px; }
.faq-item { border-bottom: 1px dashed var(--line); }
.faq-item:last-child { border-bottom: none; }
.faq-item.open .faq-q { color: var(--ink); }
.faq-item.open .faq-toggle { color: var(--brand-deep); }

.faq-q {
  display: flex; align-items: center; gap: 10px;
  padding: 14px 0;
  font-size: 13.5px;
  color: var(--ink-2);
  cursor: pointer;
  letter-spacing: 0.02em;
  transition: color var(--dur) var(--ease);
}
.faq-num {
  display: inline-flex; align-items: center; justify-content: center;
  width: 24px; height: 18px;
  background: var(--brand-soft);
  color: var(--brand-deep);
  border-radius: 4px;
  font-size: 10.5px;
  letter-spacing: 0.04em;
  flex-shrink: 0;
  font-weight: 600;
}
.faq-num.ans { background: var(--accent-rose-soft); color: #8a5a52; }
.faq-text { flex: 1; line-height: 1.5; }
.faq-text.ans { color: var(--ink-2); }
.faq-toggle { color: var(--muted-2); font-size: 14px; flex-shrink: 0; }

.faq-a {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 0 0 14px;
  font-size: 12.5px;
  color: var(--ink-2);
  line-height: 1.7;
  animation: fade-in 0.2s var(--ease);
}
@keyframes fade-in { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: none; } }

.no-result {
  padding: 40px 16px;
  text-align: center;
  color: var(--muted);
}
.nr-icon { color: var(--muted-2); }
.nr-text { margin-top: 10px; font-size: 13px; color: var(--ink-2); }
.nr-sub { margin-top: 4px; font-size: 11.5px; color: var(--muted); }

.contact-zone {
  margin: 24px 0 0;
  padding: 22px 16px;
  text-align: center;
  background: var(--surface);
  border: 1px dashed var(--line-2);
  border-radius: var(--r-md);
}
.cz-title { font-size: 14.5px; font-weight: 600; color: var(--ink); letter-spacing: 0.04em; }
.cz-sub { font-size: 11.5px; color: var(--muted); margin-top: 6px; letter-spacing: 0.04em; }
.cz-btns { display: flex; justify-content: center; gap: 10px; margin-top: 14px; }
.cz-btn {
  display: inline-flex; align-items: center; gap: 5px;
  height: 36px;
  padding: 0 16px;
  border: 1px solid var(--line);
  background: var(--surface-2);
  color: var(--ink-2);
  border-radius: 999px;
  font-size: 13px;
  font-family: inherit;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: all var(--dur) var(--ease);
}
.cz-btn.primary {
  background: var(--brand-deep);
  color: #fff;
  border-color: var(--brand-deep);
}
.cz-btn:active { transform: scale(0.98); }
.cz-btn .van-icon { font-size: 14px; }
</style>
