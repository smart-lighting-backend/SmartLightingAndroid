/**
 * 通用自动刷新 composable
 *
 * 用法:
 *   const { pause, resume, stop } = useAutoRefresh(() => loadData(), { interval: 30000 })
 *
 * 特性:
 *   - 页面隐藏时自动暂停，恢复可见时立刻拉取一次
 *   - 传入 isSensitive 回调可在敏感操作（如下拉框展开）时跳过当次刷新
 *   - onUnmounted 自动清理定时器
 */
import { onUnmounted, ref } from 'vue'

export function useAutoRefresh(fn, { interval = 30000, immediateFirst = false, isSensitive } = {}) {
  let timer = null
  let lastRun = 0
  const paused = ref(false)

  function run() {
    if (paused.value) return
    if (typeof isSensitive === 'function' && isSensitive()) return
    lastRun = Date.now()
    fn()
  }

  function schedule() {
    if (timer) clearInterval(timer)
    const delay = typeof interval === 'function' ? interval() : interval
    timer = setInterval(run, delay)
  }

  function start() {
    stop()
    if (immediateFirst) run()
    schedule()
  }

  function pause() {
    paused.value = true
    stop()
  }

  function resume() {
    paused.value = false
    // 恢复时立刻拉取一次，然后重新开始定时
    run()
    schedule()
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  // 页面可见性变化
  function onVisibilityChange() {
    if (document.visibilityState === 'visible') {
      // 恢复时若距离上次拉取已超 interval，立刻刷新
      if (!lastRun || Date.now() - lastRun > (typeof interval === 'function' ? interval() : interval)) {
        run()
      }
      schedule()
    } else {
      stop()
    }
  }

  document.addEventListener('visibilitychange', onVisibilityChange)

  onUnmounted(() => {
    stop()
    document.removeEventListener('visibilitychange', onVisibilityChange)
  })

  // 启动
  start()

  return { pause, resume, stop }
}
