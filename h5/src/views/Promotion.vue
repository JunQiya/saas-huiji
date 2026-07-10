<template>
  <div class="page promotion">
    <NavBar title="活动详情" back />

    <div class="page-padding">
      <div class="hero" :class="`hero-${id}`">
        <div class="hero-decor decor-1"></div>
        <div class="hero-decor decor-2"></div>
        <div class="hero-content">
          <div class="hero-tag">{{ tagText }}</div>
          <h1 class="hero-title">{{ name }}</h1>
          <div class="hero-sub">{{ subText }}</div>
        </div>
      </div>

      <div class="ui-card block">
        <div class="block-title">
          <span class="dot"></span>活动规则
        </div>
        <div class="rule-text">{{ ruleText }}</div>
      </div>

      <div class="ui-card block">
        <div class="block-title">
          <span class="dot"></span>活动时间
        </div>
        <div class="rule-text">{{ timeText }}</div>
      </div>

      <div class="ui-card block">
        <div class="block-title">
          <span class="dot"></span>温馨提示
        </div>
        <ul class="tip-list">
          <li>活动期间领取的券可在「我的券」中查看</li>
          <li>请到店出示券码使用</li>
          <li>最终解释权归星河·会记所有</li>
        </ul>
      </div>

      <button class="join-btn" @click="onJoin">立即参与</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import NavBar from '@/components/NavBar.vue'

const route = useRoute()
const id = computed(() => String(route.params.id || '1'))
const name = computed(() => {
  const map: Record<string, string> = {
    '1': '生日月专属礼',
    '2': '新人见面礼 30 元',
    '3': '沉睡唤醒礼 50 元'
  }
  return map[id.value] || '限时活动'
})
const tagText = computed(() => {
  const map: Record<string, string> = {
    '1': '生日月',
    '2': '新人专享',
    '3': '沉睡唤醒'
  }
  return map[id.value] || '活动'
})
const subText = computed(() => '星河·会记祝你消费愉快')
const ruleText = '活动期间，满足条件的会员可在「我的券」中查看并使用。活动不与其他优惠同享，特殊商品除外。'
const timeText = '2026.01.01 ~ 2026.12.31'

function onJoin() { showToast('请前往「我的券」查看') }
</script>

<style scoped>
.promotion { padding-bottom: 60px; }

.hero {
  position: relative;
  height: 180px;
  border-radius: var(--r-lg);
  overflow: hidden;
  margin-bottom: 16px;
  color: #fff;
  background: linear-gradient(135deg, #5a7d9f 0%, #4a6a87 100%);
  box-shadow: 0 6px 20px rgba(74, 106, 135, 0.20);
}
.hero-1 { background: linear-gradient(135deg, #b88780 0%, #9c6a5a 100%); box-shadow: 0 6px 20px rgba(184, 135, 128, 0.22); }
.hero-2 { background: linear-gradient(135deg, #7e9a8a 0%, #5b7868 100%); box-shadow: 0 6px 20px rgba(126, 154, 138, 0.22); }
.hero-3 { background: linear-gradient(135deg, #b8825a 0%, #9c6a45 100%); box-shadow: 0 6px 20px rgba(184, 130, 90, 0.22); }
.hero-decor { position: absolute; border-radius: 50%; pointer-events: none; }
.decor-1 { width: 220px; height: 220px; top: -80px; right: -50px; background: radial-gradient(circle, rgba(255, 255, 255, 0.18), transparent 60%); }
.decor-2 { width: 140px; height: 140px; bottom: -60px; left: -30px; background: radial-gradient(circle, rgba(255, 255, 255, 0.08), transparent 60%); }
.hero-content { position: relative; padding: 24px 22px; z-index: 1; }
.hero-tag {
  display: inline-block;
  font-size: 11px;
  padding: 2px 10px;
  background: rgba(255, 255, 255, 0.20);
  border-radius: 999px;
  margin-bottom: 10px;
  letter-spacing: 0.12em;
  font-weight: 500;
}
.hero-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 6px;
  letter-spacing: 0.04em;
  font-family: 'Songti SC', 'STSong', serif;
}
.hero-sub { font-size: 13px; opacity: 0.85; letter-spacing: 0.04em; }

.block { margin-bottom: 12px; padding: 16px 18px; }
.block-title {
  font-size: 13px; font-weight: 600; color: var(--ink);
  margin-bottom: 8px;
  display: flex; align-items: center; gap: 8px;
  letter-spacing: 0.04em;
}
.block-title .dot { width: 5px; height: 5px; background: var(--brand); border-radius: 50%; }
.rule-text { font-size: 12.5px; color: var(--ink-2); line-height: 1.85; letter-spacing: 0.02em; }
.tip-list { margin: 0; padding-left: 18px; color: var(--ink-2); font-size: 12.5px; line-height: 1.85; }
.tip-list li { margin: 2px 0; }

.join-btn {
  position: fixed; left: 0; right: 0; bottom: 0;
  max-width: 480px; margin: 0 auto;
  width: calc(100% - 32px);
  height: 48px;
  margin-bottom: 16px;
  background: var(--brand-deep);
  color: #fff;
  border: none;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.32em;
  cursor: pointer;
  font-family: inherit;
  box-shadow: 0 6px 18px rgba(74, 106, 135, 0.30);
  transition: all var(--dur) var(--ease);
  z-index: 5;
}
.join-btn:active { transform: scale(0.99); }
</style>
