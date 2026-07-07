import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'

export function createScene(container) {
  // Scene
  const scene = new THREE.Scene()
  scene.background = new THREE.Color(0x1a1a2e)

  // Camera
  const camera = new THREE.PerspectiveCamera(
    45,
    container.clientWidth / container.clientHeight,
    0.1,
    1000
  )
  camera.position.set(5, 5, 10)
  camera.lookAt(0, 0, 0)

  // Renderer
  const renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(container.clientWidth, container.clientHeight)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  container.appendChild(renderer.domElement)

  // Controls
  const controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05

  // Resize
  const onResize = () => {
    const w = container.clientWidth
    const h = container.clientHeight
    camera.aspect = w / h
    camera.updateProjectionMatrix()
    renderer.setSize(w, h)

    const ratio = window.innerWidth / 1920
    if (ratio > 1.1) {
      camera.position.set(5 * ratio, 5, 10 * ratio)
    } else {
      camera.position.set(5, 5, 10)
    }
    camera.lookAt(0, 0, 0)
  }
  window.addEventListener('resize', onResize)

  // Animation loop
  const animate = () => {
    requestAnimationFrame(animate)
    controls.update()
    renderer.render(scene, camera)
  }
  animate()

  // Cleanup
  const dispose = () => {
    window.removeEventListener('resize', onResize)
    controls.dispose()
    renderer.dispose()
    container.removeChild(renderer.domElement)
  }

  return { scene, camera, renderer, controls, dispose }
}
