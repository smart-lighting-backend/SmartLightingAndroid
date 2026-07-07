import { ref } from 'vue'

let loadPromise = null
let cachedAMap = null

/**
 * 高德地图 JS API 2.0 动态加载器（单例模式）
 *
 * 用法：
 *   const { AMap, loaded, loading, error } = useAMap()
 *   watch(loaded, (ok) => { if (ok) initMap() })
 *
 * 返回的 AMap 是一个 ref，需通过 AMap.value 访问高德命名空间。
 */
export function useAMap() {
  const AMap = ref(cachedAMap)
  const loaded = ref(!!cachedAMap)
  const loading = ref(false)
  const error = ref(null)

  // 已加载过
  if (cachedAMap) {
    return { AMap, loaded, loading, error }
  }

  // 正在加载中
  if (loadPromise) {
    loading.value = true
    loadPromise.then((amap) => {
      AMap.value = amap
      loaded.value = true
      loading.value = false
    }).catch((e) => {
      error.value = e.message
      loading.value = false
    })
    return { AMap, loaded, loading, error }
  }

  // 首次加载
  loading.value = true
  loadPromise = new Promise((resolve, reject) => {
    const key = import.meta.env.VITE_AMAP_KEY
    if (!key) {
      const err = new Error('VITE_AMAP_KEY 未配置，请在 .env 文件中设置高德地图 API Key')
      error.value = err.message
      loading.value = false
      reject(err)
      return
    }

    const timeout = setTimeout(() => {
      const err = new Error('高德地图 SDK 加载超时（30s）')
      error.value = err.message
      loading.value = false
      reject(err)
    }, 30000)

    // 仅当 index.html 未预设时补充（避免覆盖）
    if (!window._AMapSecurityConfig?.securityJsCode) {
      window._AMapSecurityConfig = {
        securityJsCode: import.meta.env.VITE_AMAP_SECURITY_CODE || '',
      }
    }

    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${key}&plugin=AMap.MarkerCluster`
    script.onload = () => {
      clearTimeout(timeout)
      cachedAMap = window.AMap
      AMap.value = cachedAMap
      loaded.value = true
      loading.value = false
      resolve(cachedAMap)
    }
    script.onerror = () => {
      clearTimeout(timeout)
      const err = new Error('高德地图 SDK 加载失败，请检查网络或 API Key')
      error.value = err.message
      loading.value = false
      reject(err)
    }
    document.head.appendChild(script)
  })

  function retry() {
    // 清理缓存状态，移除旧 script 标签，重新触发加载
    loadPromise = null
    cachedAMap = null
    error.value = null

    const oldScript = document.querySelector('script[src*="webapi.amap.com/maps"]')
    if (oldScript) oldScript.remove()

    loading.value = true
    loadPromise = new Promise((resolve, reject) => {
      const key = import.meta.env.VITE_AMAP_KEY
      if (!key) {
        const err = new Error('VITE_AMAP_KEY 未配置')
        error.value = err.message
        loading.value = false
        reject(err)
        return
      }
      const timeout = setTimeout(() => {
        loading.value = false
        reject(new Error('高德地图 SDK 加载超时'))
      }, 30000)
      const script = document.createElement('script')
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${key}&plugin=AMap.MarkerCluster`
      script.onload = () => {
        clearTimeout(timeout)
        cachedAMap = window.AMap
        AMap.value = cachedAMap
        loaded.value = true
        loading.value = false
        resolve(cachedAMap)
      }
      script.onerror = () => {
        clearTimeout(timeout)
        error.value = '高德地图 SDK 加载失败'
        loading.value = false
        reject(new Error('加载失败'))
      }
      document.head.appendChild(script)
    })
  }

  return { AMap, loaded, loading, error, retry }
}
