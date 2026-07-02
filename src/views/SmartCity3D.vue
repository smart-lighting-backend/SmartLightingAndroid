<script setup>import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElButton, ElTag, ElTooltip } from 'element-plus';
import { ArrowLeft, Camera } from '@element-plus/icons-vue';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { fetchDeviceList } from '../api/device';
const router = useRouter();
const containerRef = ref(null);
const deviceList = ref([]);
const loading = ref(true);
let scene, camera, renderer, controls;
let lights = [];
let raycaster, mouse;
let animationId;
let resizeTimer;
const getHealthColor = (score) => {
 if (score >= 80)
 return new THREE.Color(0x67c23a);
 if (score >= 60)
 return new THREE.Color(0xe6a23c);
 return new THREE.Color(0xf56c6c);
};
const createStreetLight = (device) => {
 const group = new THREE.Group();
 group.userData = { deviceId: device.id, deviceName: device.name };
 const poleHeight = 6 + Math.random() * 2;
 const poleGeometry = new THREE.CylinderGeometry(0.08, 0.12, poleHeight, 16);
 const poleMaterial = new THREE.MeshStandardMaterial({
 color: 0x4a4a4a,
 roughness: 0.7,
 metalness: 0.3
 });
 const pole = new THREE.Mesh(poleGeometry, poleMaterial);
 pole.position.y = poleHeight / 2;
 pole.castShadow = true;
 pole.receiveShadow = true;
 group.add(pole);
 const lampGeometry = new THREE.BoxGeometry(0.8, 0.3, 0.2);
 const lampMaterial = new THREE.MeshStandardMaterial({
 color: 0x333333,
 roughness: 0.5,
 metalness: 0.8
 });
 const lamp = new THREE.Mesh(lampGeometry, lampMaterial);
 lamp.position.set(0, poleHeight + 0.15, 0);
 lamp.castShadow = true;
 group.add(lamp);
 const glowGeometry = new THREE.SphereGeometry(0.4, 32, 32);
 const isOnline = device.status === 'online';
 const healthColor = getHealthColor(device.healthScore);
 const glowMaterial = new THREE.MeshBasicMaterial({
 color: healthColor,
 transparent: true,
 opacity: isOnline ? 0.8 : 0.2,
 side: THREE.FrontSide
 });
 const glow = new THREE.Mesh(glowGeometry, glowMaterial);
 glow.position.set(0, poleHeight + 0.15, 0);
 group.add(glow);
 const pointLight = new THREE.PointLight(healthColor, isOnline ? 2 : 0.3, 20);
 pointLight.position.set(0, poleHeight + 0.15, 0);
 pointLight.castShadow = true;
 pointLight.shadow.mapSize.width = 256;
 pointLight.shadow.mapSize.height = 256;
 group.add(pointLight);
 group.userData.glow = glow;
 group.userData.pointLight = pointLight;
 group.userData.healthColor = healthColor;
 group.userData.isOnline = isOnline;
 return group;
};
const createCityScene = () => {
 scene = new THREE.Scene();
 scene.background = new THREE.Color(0x0a0a1a);
 scene.fog = new THREE.Fog(0x0a0a1a, 30, 100);
 const cameraWidth = containerRef.value.clientWidth;
 const cameraHeight = containerRef.value.clientHeight;
 camera = new THREE.PerspectiveCamera(60, cameraWidth / cameraHeight, 0.1, 1000);
 camera.position.set(20, 15, 20);
 camera.lookAt(0, 0, 0);
 renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
 renderer.setSize(cameraWidth, cameraHeight);
 renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
 renderer.shadowMap.enabled = true;
 renderer.shadowMap.type = THREE.PCFShadowMap;
 containerRef.value.appendChild(renderer.domElement);
 controls = new OrbitControls(camera, renderer.domElement);
 controls.enableDamping = true;
 controls.dampingFactor = 0.05;
 controls.maxPolarAngle = Math.PI / 2.2;
 controls.minDistance = 10;
 controls.maxDistance = 60;
 controls.enableZoom = true;
 controls.zoomSpeed = 1.0;
 const ambientLight = new THREE.AmbientLight(0x404050, 0.5);
 scene.add(ambientLight);
 const moonLight = new THREE.DirectionalLight(0x88ccff, 0.3);
 moonLight.position.set(10, 20, 10);
 moonLight.castShadow = true;
 moonLight.shadow.mapSize.width = 1024;
 moonLight.shadow.mapSize.height = 1024;
 moonLight.shadow.camera.near = 0.5;
 moonLight.shadow.camera.far = 500;
 moonLight.shadow.camera.left = -50;
 moonLight.shadow.camera.right = 50;
 moonLight.shadow.camera.top = 50;
 moonLight.shadow.camera.bottom = -50;
 scene.add(moonLight);
 const groundGeometry = new THREE.PlaneGeometry(80, 80);
 const groundMaterial = new THREE.MeshStandardMaterial({
 color: 0x1a1a2e,
 roughness: 0.8,
 metalness: 0.2
 });
 const ground = new THREE.Mesh(groundGeometry, groundMaterial);
 ground.rotation.x = -Math.PI / 2;
 ground.receiveShadow = true;
 scene.add(ground);
 const gridHelper = new THREE.GridHelper(80, 80, 0x2a2a4e, 0x1a1a2e);
 gridHelper.position.y = 0.01;
 scene.add(gridHelper);
 for (let i = -15; i <= 15; i += 5) {
 for (let j = -15; j <= 15; j += 5) {
 if (i === 0 && j === 0)
 continue;
 const buildingHeight = 3 + Math.random() * 8;
 const buildingGeometry = new THREE.BoxGeometry(3, buildingHeight, 3);
 const buildingMaterial = new THREE.MeshStandardMaterial({
 color: new THREE.Color().setHSL(0.6 + Math.random() * 0.1, 0.1, 0.15 + Math.random() * 0.1),
 roughness: 0.7,
 metalness: 0.1
 });
 const building = new THREE.Mesh(buildingGeometry, buildingMaterial);
 building.position.set(i * 4, buildingHeight / 2, j * 4);
 building.castShadow = true;
 building.receiveShadow = true;
 scene.add(building);
 const windowGeometry = new THREE.BoxGeometry(0.4, 0.6, 0.1);
 const windowMaterial = new THREE.MeshBasicMaterial({
 color: Math.random() > 0.3 ? 0xffdd88 : 0x111111,
 transparent: true,
 opacity: Math.random() > 0.3 ? 0.8 : 0.1
 });
 for (let wy = 1; wy < buildingHeight; wy += 1.2) {
 for (let wx = -1; wx <= 1; wx += 1) {
 const windowMesh = new THREE.Mesh(windowGeometry, windowMaterial.clone());
 windowMesh.position.set(i * 4 + wx * 1.2, wy, j * 4 + 1.6);
 scene.add(windowMesh);
 }
 }
 }
 }
 raycaster = new THREE.Raycaster();
 mouse = new THREE.Vector2();
 renderer.domElement.addEventListener('click', onMouseClick);
};
const onMouseClick = (event) => {
 const rect = renderer.domElement.getBoundingClientRect();
 mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
 mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;
 raycaster.setFromCamera(mouse, camera);
 if (lights.length === 0)
 return;
 const intersects = raycaster.intersectObjects(lights.map(l => l.children), true);
 if (intersects.length > 0) {
 let parent = intersects[0].object;
 while (parent && !parent.userData.deviceId) {
 parent = parent.parent;
 }
 if (parent && parent.userData.deviceId) {
 router.push(`/device/detail/${parent.userData.deviceId}`);
 }
 }
};
const updateLights = () => {
 lights.forEach(light => {
 scene.remove(light);
 light.traverse((child) => {
 if (child.geometry)
 child.geometry.dispose();
 if (child.material) {
 if (Array.isArray(child.material)) {
 child.material.forEach(m => m.dispose());
 }
 else {
 child.material.dispose();
 }
 }
 });
 });
 lights = [];
 const positions = [
 { x: -10, z: -10 }, { x: -10, z: 0 }, { x: -10, z: 10 },
 { x: 0, z: -10 }, { x: 0, z: 0 }, { x: 0, z: 10 },
 { x: 10, z: -10 }, { x: 10, z: 0 }, { x: 10, z: 10 },
 { x: -5, z: -5 }, { x: -5, z: 5 }, { x: 5, z: -5 }, { x: 5, z: 5 },
 { x: -15, z: -15 }, { x: -15, z: 15 }, { x: 15, z: -15 }, { x: 15, z: 15 }
 ];
 deviceList.value.forEach((device, index) => {
 const pos = positions[index % positions.length];
 const lightGroup = createStreetLight(device);
 lightGroup.position.set(pos.x, 0, pos.z);
 scene.add(lightGroup);
 lights.push(lightGroup);
 });
};
const animate = () => {
 animationId = requestAnimationFrame(animate);
 controls.update();
 const time = Date.now() * 0.001;
 lights.forEach(light => {
 if (light.userData.glow && light.userData.isOnline) {
 const scale = 1 + Math.sin(time * 2) * 0.1;
 light.userData.glow.scale.set(scale, scale, scale);
 }
 });
 renderer.render(scene, camera);
};
const handleResize = () => {
 if (resizeTimer) {
 clearTimeout(resizeTimer);
 }
 resizeTimer = setTimeout(() => {
 if (!containerRef.value)
 return;
 const w = containerRef.value.clientWidth;
 const h = containerRef.value.clientHeight;
 camera.aspect = w / h;
 camera.updateProjectionMatrix();
 renderer.setSize(w, h);
 }, 300);
};
const goBack = () => {
 router.push('/device/list');
};
const loadDevices = async () => {
 loading.value = true;
 const res = await fetchDeviceList({});
 if (res.code === 200) {
 deviceList.value = res.data.list;
 updateLights();
 }
 loading.value = false;
};
onMounted(() => {
 createCityScene();
 loadDevices();
 animate();
 window.addEventListener('resize', handleResize);
});
onBeforeUnmount(() => {
 window.removeEventListener('resize', handleResize);
 if (resizeTimer)
 clearTimeout(resizeTimer);
 if (animationId)
 cancelAnimationFrame(animationId);
 renderer.domElement.removeEventListener('click', onMouseClick);
 controls.dispose();
 lights.forEach(light => {
 light.traverse((child) => {
 if (child.geometry)
 child.geometry.dispose();
 if (child.material) {
 if (Array.isArray(child.material)) {
 child.material.forEach(m => m.dispose());
 }
 else {
 child.material.dispose();
 }
 }
 });
 });
 renderer.dispose();
 if (containerRef.value && renderer.domElement) {
 containerRef.value.removeChild(renderer.domElement);
 }
});
</script>

<template>
  <div class="smart-city-container">
    <div class="header-bar">
      <ElButton @click="goBack" type="primary" plain>
        <ArrowLeft />
        返回列表
      </ElButton>
      <div class="header-title">
        <Camera />
        <span>智慧路灯3D可视化</span>
      </div>
      <div class="status-summary">
        <span class="summary-item">
          在线: 
          <ElTag type="success">{{ deviceList.filter(d => d.status === 'online').length }}</ElTag>
        </span>
        <span class="summary-item">
          离线: 
          <ElTag type="danger">{{ deviceList.filter(d => d.status === 'offline').length }}</ElTag>
        </span>
      </div>
    </div>

    <div class="scene-container" ref="containerRef" v-loading="loading">
      <div class="info-panel" v-if="!loading">
        <div class="panel-header">操作提示</div>
        <ul class="panel-content">
          <li>🖱️ 左键拖拽旋转视角</li>
          <li>🖱️ 右键拖拽平移场景</li>
          <li>🖱️ 滚轮缩放视图</li>
          <li>👆 点击路灯查看详情</li>
        </ul>
        <div class="legend">
          <div class="legend-title">图例</div>
          <div class="legend-items">
            <div class="legend-item">
              <div class="legend-dot online"></div>
              <span>在线 (健康)</span>
            </div>
            <div class="legend-item">
              <div class="legend-dot warning"></div>
              <span>在线 (一般)</span>
            </div>
            <div class="legend-item">
              <div class="legend-dot danger"></div>
              <span>在线 (异常)</span>
            </div>
            <div class="legend-item">
              <div class="legend-dot offline"></div>
              <span>离线</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.smart-city-container {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: #0a0a1a;
  position: relative;
}

.header-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: rgba(10, 10, 26, 0.9);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 100;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
}

.status-summary {
  display: flex;
  gap: 20px;
}

.summary-item {
  font-size: 14px;
  color: #909399;
}

.scene-container {
  width: 100%;
  height: 100%;
  touch-action: none;
}

.info-panel {
  position: absolute;
  bottom: 24px;
  left: 24px;
  background: rgba(10, 10, 26, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 16px;
  color: #fff;
  font-size: 13px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  z-index: 100;
  min-width: 200px;
}

.panel-header {
  font-weight: 600;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.panel-content {
  list-style: none;
  padding: 0;
  margin: 0;
  line-height: 2;
}

.panel-content li {
  color: #b0b3b8;
}

.legend {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.legend-title {
  font-weight: 600;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.legend-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #b0b3b8;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.legend-dot.online {
  background-color: #67c23a;
  box-shadow: 0 0 8px #67c23a;
}

.legend-dot.warning {
  background-color: #e6a23c;
  box-shadow: 0 0 8px #e6a23c;
}

.legend-dot.danger {
  background-color: #f56c6c;
  box-shadow: 0 0 8px #f56c6c;
}

.legend-dot.offline {
  background-color: #606266;
}
</style>