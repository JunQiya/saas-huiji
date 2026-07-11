<template>
  <div class="page">
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><ChatDotRound /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">微信公众号</h2>
          <div class="page-sub">{{ wxSlogan }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button :loading="testing" @click="onTest" class="btn-scale">
          <el-icon style="margin-right: 4px"><Connection /></el-icon>测试连通性
        </el-button>
        <el-button type="primary" :loading="saving" @click="onSave" class="btn-scale">保存配置</el-button>
      </div>
    </div>

    <div v-loading="loading" class="wx-grid">
      <!-- 基本信息 -->
      <div class="x-card section-card">
        <div class="section-title">基本信息</div>
        <el-form label-width="100px">
          <el-form-item label="AppId">
            <el-input v-model="form.appId" placeholder="公众号 AppId" />
          </el-form-item>
          <el-form-item label="AppSecret">
            <el-input v-model="form.appSecret" type="password" show-password placeholder="公众号 AppSecret" />
          </el-form-item>
          <el-form-item label="回调域名">
            <el-input v-model="form.domain" placeholder="如: huiji.example.com" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch
              v-model="form.status"
              active-value="ENABLED"
              inactive-value="DISABLED"
              active-text="启用"
              inactive-text="停用"
              inline-prompt
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 微信支付 -->
      <div class="x-card section-card">
        <div class="section-title">微信支付</div>
        <el-form label-width="100px">
          <el-form-item label="商户号">
            <el-input v-model="form.mchId" placeholder="微信支付商户号" />
          </el-form-item>
          <el-form-item label="API 密钥">
            <el-input v-model="form.mchKey" type="password" show-password placeholder="商户 API 密钥" />
          </el-form-item>
          <el-form-item label="API v3 密钥">
            <el-input v-model="form.apiV3Key" type="password" show-password placeholder="API v3 密钥" />
          </el-form-item>
          <el-form-item label="证书路径">
            <el-input v-model="form.certPath" placeholder="服务器证书文件路径" />
          </el-form-item>
        </el-form>
      </div>

      <!-- 模板消息 -->
      <div class="x-card section-card">
        <div class="section-title">
          <span>模板消息</span>
          <span class="st-tip">配置各类通知对应的模板 ID</span>
        </div>
        <el-collapse v-model="activeTpls">
          <el-collapse-item
            v-for="t in templateList"
            :key="t.key"
            :title="t.label"
            :name="t.key"
          >
            <el-form label-width="100px">
              <el-form-item label="模板 ID">
                <el-input v-model="templates[t.key]" placeholder="微信模板消息 ID" />
              </el-form-item>
            </el-form>
          </el-collapse-item>
        </el-collapse>
      </div>

      <!-- OAuth 授权链接预览 -->
      <div class="x-card section-card">
        <div class="section-title">
          <span>OAuth 授权链接</span>
          <span class="st-tip">根据 AppId 与回调域名生成</span>
        </div>
        <div class="oauth-preview">
          <div class="oauth-url val">{{ oauthUrl }}</div>
          <el-button link type="primary" @click="copyOauth">复制</el-button>
        </div>
        <div class="oauth-tip">用于网页授权获取用户 openid，请将回调域名填入公众号后台「网页授权域名」</div>
      </div>
    </div>

    <div class="footer-actions">
      <el-button @click="loadDetail">重置</el-button>
      <el-button type="primary" :loading="saving" @click="onSave" class="btn-scale">保存配置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Connection } from '@element-plus/icons-vue'
import { wxAccountApi } from '@/api'

// 随机副标题，避免每次都一样
const wxSlogan = [
  '把公众号变成你的私域入口',
  '一次授权，长期触达',
  '让消息发得出去，让到账看得清楚'
][Math.floor(Math.random() * 3)]

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)

// 模板消息列表
const templateList = [
  { key: 'loginNotify', label: '登录通知' },
  { key: 'rechargeArrived', label: '储值到账' },
  { key: 'orderPaid', label: '订单支付成功' },
  { key: 'couponArrived', label: '优惠券到账' }
]
const activeTpls = ref<string[]>(['loginNotify'])

const form = reactive<any>({
  appId: '',
  appSecret: '',
  domain: '',
  status: 'ENABLED',
  mchId: '',
  mchKey: '',
  apiV3Key: '',
  certPath: '',
  templateIds: '',
  agentId: null
})

// 模板消息本地对象（与 templateIds JSON 字符串互转）
const templates = reactive<Record<string, string>>({})

// 计算 OAuth 授权链接
const oauthUrl = computed(() => {
  if (!form.appId || !form.domain) return '请先填写 AppId 与回调域名'
  const redirect = encodeURIComponent(`https://${form.domain}/wx/callback`)
  return `https://open.weixin.qq.com/connect/oauth2/authorize?appid=${form.appId}&redirect_uri=${redirect}&response_type=code&scope=snsapi_base&state=huiji#wechat_redirect`
})

async function loadDetail() {
  loading.value = true
  try {
    const data: any = await wxAccountApi.get()
    if (data) {
      Object.assign(form, {
        appId: data.appId || '',
        appSecret: data.appSecret || '',
        domain: data.domain || '',
        status: data.status || 'ENABLED',
        mchId: data.mchId || '',
        mchKey: data.mchKey || '',
        apiV3Key: data.apiV3Key || '',
        certPath: data.certPath || '',
        templateIds: data.templateIds || '',
        agentId: data.agentId ?? null
      })
      // 解析 templateIds JSON 字符串到 templates 对象
      Object.keys(templates).forEach(k => delete templates[k])
      try {
        const parsed = form.templateIds ? JSON.parse(form.templateIds) : {}
        Object.assign(templates, parsed)
      } catch {/* 容错 */}
    }
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (!form.appId.trim()) { ElMessage.warning('请填写 AppId'); return }
  saving.value = true
  try {
    const payload = { ...form, templateIds: JSON.stringify(templates) }
    await wxAccountApi.save(payload)
    ElMessage.success('配置已保存')
    loadDetail()
  } finally {
    saving.value = false
  }
}

async function onTest() {
  testing.value = true
  try {
    const res: any = await wxAccountApi.test()
    ElMessage.success(res?.message || res?.ok ? '连通性测试通过' : '测试完成')
  } finally {
    testing.value = false
  }
}

function copyOauth() {
  if (!form.appId || !form.domain) {
    ElMessage.warning('请先填写 AppId 与回调域名')
    return
  }
  navigator.clipboard?.writeText(oauthUrl.value).then(() => {
    ElMessage.success('授权链接已复制')
  }).catch(() => {
    ElMessage.info(oauthUrl.value)
  })
}

onMounted(() => loadDetail())
</script>

<style scoped>
.wx-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.section-card {
  padding: 18px 20px;
}
.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 4px 0 12px;
}

/* OAuth 链接 */
.oauth-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
}
.oauth-url {
  flex: 1;
  font-size: 12px;
  color: var(--ink-2);
  word-break: break-all;
  line-height: 1.6;
}
.oauth-tip {
  font-size: 12px;
  color: var(--muted);
  margin-top: 8px;
  line-height: 1.6;
}
</style>
