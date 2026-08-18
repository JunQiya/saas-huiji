<template>
  <div class="page about">
    <NavBar title="关于星河" back />

    <div class="page-padding">
      <!-- 品牌区 -->
      <div class="brand-zone">
        <div class="brand-mark">
          <svg viewBox="0 0 64 64" width="64" height="64">
            <circle cx="14" cy="16" r="0.9" fill="var(--brand-ink)" opacity="0.4" />
            <circle cx="50" cy="10" r="0.7" fill="var(--brand-ink)" opacity="0.32" />
            <circle cx="56" cy="40" r="0.8" fill="var(--brand-ink)" opacity="0.36" />
            <circle cx="12" cy="48" r="0.7" fill="var(--brand-ink)" opacity="0.28" />
            <g stroke="var(--brand-ink)" stroke-width="0.7" opacity="0.5" fill="none">
              <line x1="22" y1="24" x2="32" y2="36" />
              <line x1="32" y1="36" x2="44" y2="24" />
              <line x1="32" y1="36" x2="32" y2="50" />
            </g>
            <circle cx="22" cy="24" r="2" fill="var(--brand-deep)" />
            <circle cx="44" cy="24" r="2" fill="var(--brand-deep)" />
            <circle cx="32" cy="36" r="3" fill="var(--brand-deep)" />
            <circle cx="32" cy="50" r="1.6" fill="var(--brand-deep)" opacity="0.7" />
          </svg>
        </div>
        <div class="brand-name">星河·会记</div>
        <div class="brand-en">XINGHE HUIJI</div>
        <div class="brand-version">v 1.0.0 · 手机端</div>
        <div class="brand-slogan">在细碎的会员日常里，做一支温热的笔。</div>
      </div>

      <!-- 品牌故事 -->
      <div class="block">
        <div class="block-title">
          <span class="dot"></span>品牌故事
        </div>
        <div class="ui-card block-card">
          <p class="story">
            星河·会记诞生于一个简单的念头——<br>
            会员不该是表格里的一行数字，<br>
            而应是门店悉心珍藏的，一位具体的人。
          </p>
          <div class="divider-poem">— · —</div>
          <p class="story">
            我们想把每一次到店、每一份心意、每一通回访电话，<br>
            都妥帖地安放在星河里，<br>
            让细水长流，成为最好的生意。
          </p>
        </div>
      </div>

      <!-- 联系方式 -->
      <div class="block">
        <div class="block-title">
          <span class="dot"></span>联系星河
        </div>
        <div class="ui-card contact-card">
          <div class="contact-row">
            <div class="contact-ic"><van-icon name="phone-o" /></div>
            <div class="contact-text">
              <div class="contact-key">客服电话</div>
              <div class="contact-val">{{ CONTACT.phone }}</div>
            </div>
            <a :href="CONTACT.phoneHref" class="contact-action">拨号</a>
          </div>
          <div class="contact-row">
            <div class="contact-ic"><van-icon name="envelop-o" /></div>
            <div class="contact-text">
              <div class="contact-key">服务邮箱</div>
              <div class="contact-val">{{ CONTACT.email }}</div>
            </div>
            <a :href="CONTACT.emailHref" class="contact-action">写信</a>
          </div>
          <div class="contact-row">
            <div class="contact-ic"><van-icon name="location-o" /></div>
            <div class="contact-text">
              <div class="contact-key">星河坐标</div>
              <div class="contact-val">上海市黄浦区南京东路 100 号</div>
            </div>
          </div>
          <div class="contact-row">
            <div class="contact-ic"><van-icon name="clock-o" /></div>
            <div class="contact-text">
              <div class="contact-key">服务时间</div>
              <div class="contact-val">9:00 - 21:00 · 7x12h</div>
            </div>
          </div>
        </div>
      </div>

      <div class="copyright">
        © 2026 星河·会记 · 版权所有
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import NavBar from '@/components/NavBar.vue'
import { CONTACT } from '@/constants/config'
</script>

<style scoped>
.about { padding-bottom: 40px; }

.brand-zone {
  text-align: center;
  padding: 18px 16px 26px;
  position: relative;
}
.brand-mark {
  width: 64px; height: 64px;
  margin: 0 auto 14px;
  position: relative;
  display: flex; align-items: center; justify-content: center;
  background: var(--brand-soft);
  border-radius: 16px;
  border: 1px dashed var(--line-2);
  animation: x-fade-in 0.5s var(--ease-out) both;
}
.brand-name {
  font-size: 20px; font-weight: 600; color: var(--ink);
  letter-spacing: 0.08em;
  font-family: var(--font-serif);
}
.brand-en {
  font-size: 10.5px; color: var(--muted);
  letter-spacing: 0.36em; margin-top: 4px;
}
.brand-version {
  font-size: 11px; color: var(--muted);
  margin-top: 8px; letter-spacing: 0.12em;
}
.brand-slogan {
  margin-top: 14px;
  font-size: 12.5px;
  color: var(--ink-2);
  letter-spacing: 0.12em;
  font-family: var(--font-serif);
  opacity: 0.85;
  line-height: 1.7;
}

.block { margin-top: 18px; }
.block-title {
  font-size: 13px; font-weight: 600; color: var(--ink-2);
  margin-bottom: 8px; padding-left: 2px;
  display: flex; align-items: center; gap: 8px;
  letter-spacing: 0.06em;
}
.block-title .dot {
  width: 5px; height: 5px;
  background: var(--brand); border-radius: 50%;
}
.block-card { padding: 18px 18px; }
.story {
  font-size: 13px; color: var(--ink-2);
  line-height: 1.95;
  margin: 0;
  font-family: var(--font-serif);
  letter-spacing: 0.04em;
  text-align: center;
}

.contact-card { padding: 6px 16px; }
.contact-row {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--line);
}
.contact-row:last-child { border-bottom: none; }
.contact-ic {
  width: 30px; height: 30px;
  border-radius: 8px;
  background: var(--brand-soft); color: var(--brand-deep);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.contact-text { flex: 1; min-width: 0; }
.contact-key { font-size: 11px; color: var(--muted); letter-spacing: 0.04em; }
.contact-val { font-size: 13px; color: var(--ink); margin-top: 2px; font-family: 'SF Mono', monospace; letter-spacing: 0.04em; }
.contact-action {
  font-size: 12px;
  color: var(--brand-deep);
  padding: 3px 10px;
  background: var(--brand-soft);
  border-radius: 999px;
  letter-spacing: 0.04em;
}

.copyright {
  text-align: center;
  font-size: 11px;
  color: var(--muted);
  margin: 30px 0 8px;
  letter-spacing: 0.16em;
  opacity: 0.7;
}
</style>
