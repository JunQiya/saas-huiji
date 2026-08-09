import { onBeforeUnmount, onMounted } from 'vue'

interface UsePollingOptions {
  /** 轮询间隔(ms) */
  interval: number
  /** 每次轮询要执行的动作 */
  tick: () => void
  /** 页面切回前台时立即执行的动作（默认与 tick 相同） */
  onVisible?: () => void
}

// 统一的页面轮询：自动管理 setInterval + 页面可见性监听 + 卸载清理，
// 避免每个页面复制粘贴同一套逻辑。
export function usePolling(options: UsePollingOptions) {
  const { interval, tick, onVisible } = options

  let timer: number | null = null

  function start() {
    stop()
    timer = window.setInterval(tick, interval)
  }

  function stop() {
    if (timer !== null) {
      clearInterval(timer)
      timer = null
    }
  }

  function onVisibilityChange() {
    if (document.hidden) {
      stop()
    } else {
      if (onVisible) onVisible()
      else tick()
      start()
    }
  }

  onMounted(() => {
    document.addEventListener('visibilitychange', onVisibilityChange)
    start()
  })

  onBeforeUnmount(() => {
    stop()
    document.removeEventListener('visibilitychange', onVisibilityChange)
  })

  return { start, stop }
}
