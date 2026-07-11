<template>
  <div class="not-found">
    <div class="nf-inner">
      <div class="brand-mark">星</div>

      <div class="nf-code">
        <span class="d">4</span>
        <span class="d middle">0</span>
        <span class="d">4</span>
      </div>

      <div class="nf-text">页面没有找到</div>
      <div class="nf-sub">可能已被移动或不存在</div>

      <div class="nf-actions">
        <el-button type="primary" class="btn-scale" @click="$router.push('/dashboard')">回到看板</el-button>
        <el-button class="btn-scale" @click="$router.back()">返回上一页</el-button>
      </div>

      <div class="nf-footer">© 星河·会记</div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 404 页
</script>

<style scoped>
.not-found {
  position: relative;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--page-bg);
  overflow: hidden;
}

.nf-inner {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 0 24px;
}

.brand-mark {
  width: 44px; height: 44px;
  border-radius: var(--r-md);
  background: var(--brand);
  color: #fff;
  font-family: var(--font-serif);
  font-size: 20px;
  display: flex; align-items: center; justify-content: center;
  letter-spacing: 0.04em;
  margin-bottom: 26px;
}

.nf-code {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 88px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1;
  letter-spacing: 4px;
  font-family: var(--font-serif);
}
.nf-code .d.middle {
  background: var(--brand);
  color: #fff;
  width: 84px; height: 84px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
}

.nf-text {
  font-size: 17px;
  font-weight: 500;
  color: var(--ink);
  margin-top: 24px;
  letter-spacing: 0.06em;
  font-family: var(--font-serif);
}
.nf-sub {
  font-size: 13px;
  color: var(--muted);
  margin-top: 8px;
  letter-spacing: 0.04em;
}

.nf-actions {
  display: flex;
  gap: 10px;
  margin-top: 28px;
}
.nf-footer {
  margin-top: 64px;
  font-size: 11.5px;
  color: var(--muted-2);
  letter-spacing: 0.16em;
}
</style>
