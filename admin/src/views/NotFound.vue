<template>
  <div class="not-found">
    <!-- 背景装饰 -->
    <div class="bg-decor">
      <div class="decor decor-1"></div>
      <div class="decor decor-2"></div>
      <div class="decor decor-3"></div>
      <div class="orbit">
        <span class="orbit-star"></span>
      </div>
    </div>

    <div class="nf-inner">
      <div class="brand-mark">
        <span class="star"></span>
      </div>

      <div class="nf-code">
        <span class="d">4</span>
        <span class="d planet"><span class="p-ring"></span></span>
        <span class="d">4</span>
      </div>

      <div class="nf-text">这片星河里没有你要找的星</div>
      <div class="nf-sub">页面走失了，或已被时间的潮汐带走</div>

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
  background:
    radial-gradient(circle at 20% 28%, rgba(111, 148, 184, 0.10), transparent 45%),
    radial-gradient(circle at 80% 75%, rgba(138, 130, 120, 0.08), transparent 45%),
    linear-gradient(160deg, #fbfaf6 0%, #eef0f2 60%, #e6e8ec 100%);
  overflow: hidden;
}

/* 背景装饰 */
.bg-decor { position: absolute; inset: 0; pointer-events: none; }
.decor {
  position: absolute;
  border-radius: 50%;
  filter: blur(0.5px);
}
.decor-1 {
  width: 260px; height: 260px;
  top: 8%; right: 6%;
  background: radial-gradient(circle, rgba(111, 148, 184, 0.12), transparent 70%);
}
.decor-2 {
  width: 360px; height: 360px;
  bottom: -12%; left: -10%;
  background: radial-gradient(circle, rgba(138, 130, 120, 0.10), transparent 70%);
}
.decor-3 {
  width: 96px; height: 96px;
  top: 22%; left: 16%;
  border: 1px solid rgba(108, 120, 108, 0.18);
  background: rgba(255, 255, 255, 0.4);
}
.orbit {
  position: absolute;
  bottom: 14%; right: 12%;
  width: 180px; height: 180px;
  border: 1px dashed rgba(111, 148, 184, 0.25);
  border-radius: 50%;
  animation: spin 22s linear infinite;
}
.orbit-star {
  position: absolute;
  top: -6px; left: 50%;
  width: 12px; height: 12px;
  background: var(--primary-action);
  border-radius: 50%;
  box-shadow: 0 0 12px rgba(111, 148, 184, 0.55);
}
@keyframes spin { to { transform: rotate(360deg); } }

/* 内容 */
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
  border-radius: 12px;
  background: linear-gradient(135deg, #6f94b8 0%, #4a6a87 100%);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 6px 18px rgba(74, 106, 135, 0.28);
  margin-bottom: 26px;
}
.star {
  width: 18px; height: 18px;
  background: #fff;
  clip-path: polygon(50% 0, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%);
}

.nf-code {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 88px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1;
  letter-spacing: 2px;
}
.nf-code .d.planet {
  position: relative;
  width: 64px; height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6f94b8 0%, #4a6a87 100%);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 38px;
  box-shadow: 0 4px 18px rgba(74, 106, 135, 0.30);
}
.p-ring {
  position: absolute;
  inset: -8px -14px;
  border: 1px solid rgba(111, 148, 184, 0.45);
  border-radius: 50%;
  transform: rotate(-18deg);
}

.nf-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--ink);
  margin-top: 22px;
  letter-spacing: 0.5px;
}
.nf-sub {
  font-size: 13px;
  color: var(--muted);
  margin-top: 8px;
  letter-spacing: 0.3px;
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
  letter-spacing: 1px;
}
</style>
