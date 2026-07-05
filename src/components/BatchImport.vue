<script setup>
import { ref, computed, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { downloadTemplate, parseImportFile, validateAllRows, validateDeviceRow, rowsToPayload } from '../utils/excelTemplate.js'
import { batchCreateDevices } from '../api/devices.js'
import { useAMap } from '../composables/useAMap.js'
import { parseLocation } from '../utils/coordinate.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  existingDeviceIds: { type: Array, default: () => [] },
  existingDevices: { type: Array, default: () => [] },
})

const emit = defineEmits(['update:visible', 'imported'])

const { AMap: AMapRef, loaded: mapLoaded, loading: mapLoading } = useAMap()

const step = ref('upload')
const file = ref(null)
const fileInputRef = ref(null)
const parsedRows = ref([])
const validationResults = ref([])
const importing = ref(false)
const importResult = ref({ success: 0, failed: 0, errors: [] })

// 行内编辑
const editingCell = ref(null) // { rowIndex, field }
const editValue = ref('')

// 地图预览
const mapPreviewVisible = ref(false)
const mapContainerRef = ref(null)
let previewMap = null
let previewMarkers = []

const validCount = computed(() => validationResults.value.filter(r => r.valid).length)
const errorCount = computed(() => validationResults.value.filter(r => !r.valid).length)

const COLORS = { 0: '#888', 1: '#4caf82', 2: '#9e9e9e', 3: '#ffa726' }

function onFileChange(e) {
  const f = e.target.files?.[0]
  if (!f) return
  const ext = f.name.split('.').pop().toLowerCase()
  if (!['xlsx', 'xls', 'csv'].includes(ext)) {
    ElMessage.error('仅支持 .xlsx / .xls / .csv 格式')
    return
  }
  file.value = f
  doParse(f)
}

async function doParse(f) {
  try {
    parsedRows.value = await parseImportFile(f)
    if (parsedRows.value.length === 0) {
      ElMessage.warning('文件中未解析到有效数据')
      return
    }
    revalidateAll()
    step.value = 'preview'
  } catch (e) {
    ElMessage.error(e.message || '文件解析失败')
  }
}

function revalidateAll() {
  validationResults.value = validateAllRows(parsedRows.value, new Set(props.existingDeviceIds))
}

function revalidateRow(rowIndex) {
  // 编辑后全量重校验：因为修改可能影响其他行的重复检测
  revalidateAll()
}

// ── 行内编辑 ──
function startEdit(rowIndex, field) {
  editingCell.value = { rowIndex, field }
  editValue.value = parsedRows.value[rowIndex][field] || ''
  nextTick(() => {
    const input = document.querySelector('.bi-edit-input')
    if (input) { input.focus(); input.select() }
  })
}

function finishEdit() {
  if (!editingCell.value) return
  const { rowIndex, field } = editingCell.value
  const val = editValue.value.trim()
  parsedRows.value[rowIndex][field] = val || ''
  editingCell.value = null
  revalidateRow(rowIndex)
}

function onEditKeydown(e) {
  if (e.key === 'Enter') finishEdit()
  if (e.key === 'Escape') { editingCell.value = null }
}

function cellDisplay(rowIndex, field) {
  if (editingCell.value?.rowIndex === rowIndex && editingCell.value?.field === field) {
    return null // render input instead
  }
  return parsedRows.value[rowIndex][field] || ''
}

// ── 导入 ──
async function doImport() {
  if (importing.value) return
  const validRows = parsedRows.value.filter((_, i) => validationResults.value[i]?.valid)
  if (validRows.length === 0) {
    ElMessage.warning('没有可导入的有效数据')
    return
  }
  importing.value = true
  const payload = rowsToPayload(validRows)
  try {
    const res = await batchCreateDevices(payload)
    const data = res?.data || res
    importResult.value = { success: data.success || 0, failed: data.failed || 0, errors: (data.failedDetails || []).map(e => ({ row: e.row, deviceId: e.deviceId, reason: e.reason })) }
  } catch (e) {
    importResult.value = { success: 0, failed: payload.length, errors: [{ row: 0, deviceId: '', reason: e?.message || '批量导入请求失败' }] }
  }
  step.value = 'result'
  importing.value = false
  emit('imported')
}

function reset() {
  step.value = 'upload'; file.value = null; parsedRows.value = []; validationResults.value = []; importResult.value = { success: 0, failed: 0, errors: [] }; editingCell.value = null
}

function handleClose() { reset(); emit('update:visible', false) }

// ── 地图预览 ──
const importCandidatesWithCoords = computed(() =>
  parsedRows.value.filter(r => r.longitude && r.latitude && !isNaN(parseFloat(r.longitude)) && !isNaN(parseFloat(r.latitude)))
)

function openMapPreview() {
  mapPreviewVisible.value = true
  nextTick(initPreviewMap)
}

function closeMapPreview() {
  mapPreviewVisible.value = false
  clearPreviewMarkers()
  if (previewMap) { previewMap.destroy(); previewMap = null }
}

function initPreviewMap() {
  if (!mapContainerRef.value || !AMapRef.value) return
  if (previewMap) { previewMap.destroy(); previewMap = null }
  clearPreviewMarkers()

  previewMap = new AMapRef.value.Map(mapContainerRef.value, { mapStyle: 'amap://styles/dark', zoom: 5, center: [104, 35] })

  const allMarkers = []

  // 已有设备（彩色圆点）
  props.existingDevices.forEach(d => {
    const pos = parseLocation(d.location)
    if (!pos) return
    const c = COLORS[d.status] || '#888'
    const m = new AMapRef.value.Marker({
      position: [pos.lng, pos.lat],
      content: `<div style="width:10px;height:10px;background:${c};border:2px solid rgba(255,255,255,0.7);border-radius:50%;box-shadow:0 0 4px ${c};"></div>`,
      anchor: 'center', zIndex: 100,
    })
    m.setMap(previewMap)
    allMarkers.push(m)
    previewMarkers.push(m)
  })

  // 待导入设备（红色标记）
  importCandidatesWithCoords.value.forEach((r, i) => {
    const lng = parseFloat(r.longitude)
    const lat = parseFloat(r.latitude)
    const label = r.deviceId || `#${i + 1}`
    const m = new AMapRef.value.Marker({
      position: [lng, lat],
      content: `<div style="text-align:center;line-height:1.1"><div style="width:14px;height:14px;background:#ff4444;border:2.5px solid #fff;border-radius:50%;box-shadow:0 0 8px rgba(255,50,50,0.6);margin:0 auto;"></div><span style="font-size:9px;color:#ff8888;white-space:nowrap;">${label}</span></div>`,
      anchor: 'bottom-center', zIndex: 200,
    })
    m.setMap(previewMap)
    allMarkers.push(m)
    previewMarkers.push(m)
  })

  if (allMarkers.length > 0) previewMap.setFitView(allMarkers)
}

function clearPreviewMarkers() {
  previewMarkers.forEach(m => m.setMap(null))
  previewMarkers = []
}

function handleDragOver(e) { e.preventDefault() }
function handleDrop(e) {
  e.preventDefault()
  const f = e.dataTransfer?.files?.[0]
  if (!f) return
  file.value = f
  doParse(f)
}

onUnmounted(() => {
  clearPreviewMarkers()
  if (previewMap) { previewMap.destroy(); previewMap = null }
})
</script>

<template>
  <div v-if="visible" class="bi-overlay" @click.self="handleClose">
    <div class="bi-dialog">
      <div class="bi-header">
        <span>批量导入设备</span>
        <button class="bi-close" @click="handleClose">&times;</button>
      </div>

      <!-- Step 1: 上传 -->
      <div v-if="step === 'upload'" class="bi-body">
        <div class="bi-upload-zone" @dragover="handleDragOver" @drop="handleDrop" @click="fileInputRef?.click()">
          <UploadFilled style="font-size:36px;color:rgba(77,208,225,0.5)" />
          <p>点击或拖拽上传 Excel / CSV 文件</p>
          <span class="bi-hint">支持 .xlsx .xls .csv</span>
          <input ref="fileInputRef" type="file" accept=".xlsx,.xls,.csv" style="display:none" @change="onFileChange" />
        </div>
        <div class="bi-template">
          <span>还没有模板？</span>
          <button class="bi-btn-outline" @click="downloadTemplate">下载模板</button>
        </div>
      </div>

      <!-- Step 2: 预览 -->
      <div v-if="step === 'preview'" class="bi-body">
        <div class="bi-summary">
          共解析 <strong>{{ parsedRows.length }}</strong> 条，
          <span class="bi-ok">有效 {{ validCount }} 条</span>
          <span v-if="errorCount > 0" class="bi-err">，错误 {{ errorCount }} 条</span>
          <span class="bi-hint-edit" v-if="errorCount > 0">（点击单元格可修改）</span>
        </div>
        <div class="bi-table-wrap">
          <table class="bi-table">
            <thead>
              <tr>
                <th>行</th><th>设备编号</th><th>名称</th><th>区域</th><th>经度</th><th>纬度</th><th>功率(W)</th><th>问题</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in parsedRows" :key="i" :class="{ 'bi-row-err': !validationResults[i]?.valid }">
                <td>{{ row._row }}</td>
                <td v-for="field in ['deviceId','name','area','longitude','latitude','ratedPower']" :key="field"
                  class="bi-cell-editable" @click="startEdit(i, field)">
                  <template v-if="editingCell?.rowIndex === i && editingCell?.field === field">
                    <input v-model="editValue" class="bi-edit-input"
                      @blur="finishEdit" @keydown="onEditKeydown"
                      @click.stop />
                  </template>
                  <template v-else>{{ cellDisplay(i, field) }}</template>
                </td>
                <td class="bi-err-cell">{{ validationResults[i]?.errors?.join('；') || '' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Step 3: 结果 -->
      <div v-if="step === 'result'" class="bi-body">
        <div class="bi-result">
          <div class="bi-result-icon">{{ importResult.failed === 0 ? '✅' : '⚠️' }}</div>
          <p class="bi-result-text">
            导入完成：成功 <strong>{{ importResult.success }}</strong> 条
            <template v-if="importResult.failed > 0">，失败 <strong class="bi-err">{{ importResult.failed }}</strong> 条</template>
          </p>
          <div v-if="importResult.errors.length" class="bi-err-list">
            <div v-for="(e, i) in importResult.errors" :key="i" class="bi-err-item">
              第 {{ e.row }} 行 [{{ e.deviceId }}]：{{ e.reason }}
            </div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="bi-footer">
        <button class="bi-btn-cancel" @click="handleClose">关闭</button>
        <template v-if="step === 'preview'">
          <button class="bi-btn-outline" @click="openMapPreview">🗺️ 地图预览</button>
          <div style="flex:1"></div>
          <button class="bi-btn-cancel" @click="reset">重新上传</button>
          <button class="bi-btn-confirm" :disabled="validCount === 0 || importing" @click="doImport">
            {{ importing ? '导入中...' : `确认导入 ${validCount} 条` }}
          </button>
        </template>
        <template v-if="step === 'result'">
          <button class="bi-btn-confirm" @click="reset">继续导入</button>
        </template>
      </div>
    </div>

    <!-- 地图预览弹窗 -->
    <Teleport to="body">
      <div v-if="mapPreviewVisible" class="bi-map-overlay" @click.self="closeMapPreview">
        <div class="bi-map-dialog">
          <div class="bi-map-header">
            <span>导入地图预览 — 红色为待导入设备</span>
            <button class="bi-close" @click="closeMapPreview">&times;</button>
          </div>
          <div class="bi-map-body">
            <div v-if="!mapLoaded" class="bi-map-loading">地图加载中...</div>
            <div ref="mapContainerRef" class="bi-map-container"></div>
          </div>
          <div class="bi-map-footer">
            <span class="bi-legend">
              <i style="background:#ff4444;width:10px;height:10px;border-radius:50%;display:inline-block;border:2px solid #fff;"></i> 待导入 ({{ importCandidatesWithCoords.length }})
              <i style="background:#4caf82;width:10px;height:10px;border-radius:50%;display:inline-block;border:2px solid #fff;margin-left:12px;"></i> 已有在线
              <i style="background:#9e9e9e;width:10px;height:10px;border-radius:50%;display:inline-block;border:2px solid #fff;margin-left:8px;"></i> 离线
            </span>
            <button class="bi-btn-confirm" @click="closeMapPreview">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.bi-overlay {
  position: fixed; inset: 0; z-index: 3100;
  background: rgba(0,0,0,0.6);
  display: flex; align-items: center; justify-content: center;
}
.bi-dialog {
  width: 820px; max-height: 85vh;
  background: #0d1b33; border: 1px solid rgba(0,120,200,0.3);
  border-radius: 12px; overflow: hidden;
  display: flex; flex-direction: column;
}
.bi-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px; border-bottom: 1px solid rgba(0,120,200,0.15);
  font-size: 16px; font-weight: 600; color: #d0eaf8;
}
.bi-close { background: none; border: none; color: rgba(140,190,220,0.6); font-size: 22px; cursor: pointer; }
.bi-close:hover { color: #fff; }
.bi-body { flex: 1; overflow-y: auto; padding: 20px; }

.bi-upload-zone {
  border: 2px dashed rgba(0,120,200,0.3); border-radius: 10px;
  padding: 48px 20px; text-align: center; cursor: pointer;
  transition: all 0.2s;
}
.bi-upload-zone:hover { border-color: rgba(77,208,225,0.5); background: rgba(0,80,160,0.08); }
.bi-upload-zone p { color: rgba(180,210,240,0.7); font-size: 14px; margin: 12px 0 6px; }
.bi-hint { color: rgba(140,190,220,0.4); font-size: 12px; }
.bi-template { display: flex; align-items: center; justify-content: center; gap: 10px; margin-top: 16px; color: rgba(140,190,220,0.5); font-size: 13px; }

/* Table */
.bi-summary { margin-bottom: 12px; font-size: 13px; color: rgba(180,210,240,0.7); }
.bi-summary strong { color: #d0eaf8; }
.bi-ok { color: #4caf82; }
.bi-err { color: #ef5350; }
.bi-hint-edit { color: rgba(140,190,220,0.4); font-size: 11px; margin-left: 8px; }
.bi-table-wrap { max-height: 340px; overflow: auto; border: 1px solid rgba(0,120,200,0.12); border-radius: 8px; }
.bi-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.bi-table th {
  position: sticky; top: 0; z-index: 1;
  background: rgba(0,30,70,0.9); color: rgba(140,190,220,0.7);
  padding: 8px 10px; text-align: left; font-weight: 600; white-space: nowrap;
}
.bi-table td {
  padding: 7px 10px; color: rgba(200,220,240,0.8);
  border-top: 1px solid rgba(0,80,140,0.1); white-space: nowrap;
}
.bi-row-err td { background: rgba(239,83,80,0.08); }
.bi-cell-editable { cursor: pointer; transition: background 0.15s; }
.bi-cell-editable:hover { background: rgba(77,208,225,0.08); outline: 1px dashed rgba(77,208,225,0.3); outline-offset: -1px; }
.bi-edit-input {
  width: 100%; padding: 2px 4px; font-size: 12px;
  background: rgba(0,30,80,0.8); border: 1px solid #4dd0e1;
  border-radius: 3px; color: #d0eaf8; outline: none;
}
.bi-err-cell { color: #ef5350 !important; min-width: 80px; white-space: normal !important; }

/* Result */
.bi-result { text-align: center; padding: 30px 0; }
.bi-result-icon { font-size: 40px; margin-bottom: 12px; }
.bi-result-text { font-size: 16px; color: rgba(200,220,240,0.8); }
.bi-result-text strong { font-size: 18px; }
.bi-err-list { text-align: left; margin-top: 16px; max-height: 200px; overflow: auto; }
.bi-err-item { padding: 6px 10px; font-size: 12px; color: #ef5350; background: rgba(239,83,80,0.05); border-radius: 4px; margin-bottom: 4px; }

/* Footer */
.bi-footer { display: flex; align-items: center; justify-content: flex-end; gap: 10px; padding: 14px 20px; border-top: 1px solid rgba(0,120,200,0.15); flex-wrap: wrap; }
.bi-btn-outline {
  padding: 6px 14px; border-radius: 6px; font-size: 12px;
  background: transparent; border: 1px solid rgba(77,208,225,0.3);
  color: #4dd0e1; cursor: pointer;
}
.bi-btn-outline:hover { background: rgba(77,208,225,0.1); }
.bi-btn-cancel {
  padding: 8px 18px; border-radius: 6px; font-size: 13px;
  background: rgba(0,50,100,0.3); border: 1px solid rgba(0,100,160,0.3);
  color: rgba(180,210,230,0.7); cursor: pointer;
}
.bi-btn-cancel:hover { background: rgba(0,80,140,0.3); }
.bi-btn-confirm {
  padding: 8px 18px; border-radius: 6px; font-size: 13px;
  background: linear-gradient(135deg, #0077cc, #0099e6);
  border: none; color: #fff; cursor: pointer; font-weight: 500;
}
.bi-btn-confirm:hover { box-shadow: 0 2px 12px rgba(0,150,230,0.3); }
.bi-btn-confirm:disabled { opacity: 0.5; cursor: not-allowed; }

/* Map preview */
.bi-map-overlay {
  position: fixed; inset: 0; z-index: 3200;
  background: rgba(0,0,0,0.75);
  display: flex; align-items: center; justify-content: center;
}
.bi-map-dialog {
  width: 90vw; height: 85vh;
  background: #0d1b33; border: 1px solid rgba(0,120,200,0.3);
  border-radius: 12px; overflow: hidden;
  display: flex; flex-direction: column;
}
.bi-map-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 18px; border-bottom: 1px solid rgba(0,120,200,0.15);
  font-size: 14px; font-weight: 600; color: #d0eaf8;
}
.bi-map-body { flex: 1; position: relative; }
.bi-map-container { width: 100%; height: 100%; }
.bi-map-loading {
  position: absolute; inset: 0; z-index: 2;
  display: flex; align-items: center; justify-content: center;
  background: rgba(8,20,45,0.6); color: rgba(140,190,220,0.6); font-size: 14px;
}
.bi-map-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 18px; border-top: 1px solid rgba(0,120,200,0.15);
}
.bi-legend { font-size: 12px; color: rgba(140,190,220,0.6); display: flex; align-items: center; gap: 4px; }
</style>
