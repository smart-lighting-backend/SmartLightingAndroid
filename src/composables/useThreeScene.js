import { ref, onMounted, onUnmounted } from 'vue'
import { createScene } from '../three/core/index.js'

export function useThreeScene(containerRef) {
  const sceneRef = ref(null)

  onMounted(() => {
    if (!containerRef.value) return
    const { scene, camera, renderer, controls, dispose } = createScene(containerRef.value)
    sceneRef.value = { scene, camera, renderer, controls }

    onUnmounted(() => {
      dispose()
      sceneRef.value = null
    })
  })

  return { sceneRef }
}
