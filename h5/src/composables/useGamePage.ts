import { computed, onActivated, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { gameApi } from '@/api/h5'
import { formatDateTime } from '@/utils/format'

// 游戏页公共逻辑：详情/奖品/剩余次数/记录加载 + 结果弹窗状态，
// 供大转盘/刮刮卡/砸金蛋/摇一摇四页复用，避免逐字复制。
export function useGamePage() {
  const route = useRoute()
  const gameId = route.params.id as string

  const loading = ref(false)
  const game = ref<any>(null)
  const prizes = ref<any[]>([])
  const records = ref<any[]>([])
  const showRecords = ref(false)
  const remaining = ref(0)
  const resultVisible = ref(false)
  const result = ref<any>(null)

  const bgStyle = computed(() => {
    if (game.value?.bgImage) {
      return { backgroundImage: `url(${game.value.bgImage})` }
    }
    return {}
  })

  async function loadDetail() {
    loading.value = true
    try {
      const d = await gameApi.detail(gameId)
      game.value = d?.game ?? d
      prizes.value = d?.prizes || []
      remaining.value = Number(d?.remaining ?? d?.game?.remaining ?? d?.game?.dailyLimit ?? 0)
    } catch (e: any) { console.warn('loadDetail failed', e) }
    finally { loading.value = false }
  }

  async function loadRecords() {
    try { records.value = (await gameApi.myPlays(gameId)) || [] } catch (e: any) { console.warn('loadRecords failed', e) }
  }

  function closeResult() {
    resultVisible.value = false
    result.value = null
  }

  // 继续/再玩一次：重置结果弹窗（由各页自行决定是否再次调用 play）
  function onContinue() {
    closeResult()
  }

  function formatTime(t?: string) {
    if (!t) return ''
    return formatDateTime(t)
  }

  onMounted(() => {
    loadDetail()
    loadRecords()
  })
  // 从其他页面返回时重新拉取剩余次数
  onActivated(() => {
    loadDetail()
  })

  return {
    gameId,
    loading,
    game,
    prizes,
    records,
    showRecords,
    remaining,
    resultVisible,
    result,
    bgStyle,
    loadDetail,
    loadRecords,
    onContinue,
    closeResult,
    formatTime
  }
}
