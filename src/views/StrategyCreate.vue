<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchStrategyGroups, createStrategy, fetchStrategyDetail, updateStrategy } from '../api/strategy.js'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const groups = ref([])
const saving = ref(false)
const saveSuccess = ref(false)
const isEdit = ref(false)
const showAddMenu = ref(false)

// 可添加的条件类型（key 必须与 DecisionEngine evaluateSingle 匹配）
const AVAILABLE_CONDITIONS = [
  { key: 'lux_gt',     label: '光照强度高于',   desc: '当光照传感器读数超过阈值时触发',     value: 200, unit: 'Lux' },
  { key: 'temp_lt',    label: '温度低于',       desc: '当温度读数低于阈值时触发',           value: 5,   unit: '°C' },
  { key: 'temp_gt',    label: '温度高于',       desc: '当温度读数超过阈值时触发',           value: 35,  unit: '°C' },
  { key: 'humidity_lt',label: '湿度低于',       desc: '当湿度读数低于阈值时触发',           value: 30,  unit: '%' },
  { key: 'humidity_gt',label: '湿度高于',       desc: '当湿度读数超过阈值时触发',           value: 80,  unit: '%' },
  { key: 'traffic_gt', label: '车流量高于',     desc: '当车流量读数超过阈值时触发',         value: 30,  unit: '辆/分钟' },
  { key: 'pir',        label: '人体红外检测',   desc: '检测到人体活动时触发（1=有人）',    value: 1,   unit: '',   isBoolean: true },
]

// 旧格式条件映射 → 新格式 key
function mapOldCondition(key, val) {
  switch (key) {
    case 'illuminance': return { key: 'lux_lt', label: '环境光照度低于阈值', desc: '当光传感器读数跌至阈值以下时触发。', value: val.threshold || 30, unit: 'Lux', enabled: true }
    case 'traffic': return { key: 'traffic_lt', label: '人车流量阈值（雷达感知）', desc: '区域内5分钟平均流量低于设定值。', value: val.threshold || 10, unit: '次/5min', enabled: true }
    default: return null
  }
}

const form = reactive({
  name: '',
  group: '',
  startTime: '23:00',
  endTime: '05:00',
  actions: {
    brightness: 30,
    voiceAlert: false,
    nightVision: true,
    generateAlert: false,
  },
})

let nextId = 1
const conditionItems = ref([
  { id: nextId++, key: 'lux_lt',     label: '环境光照度低于阈值',       desc: '当光传感器读数跌至阈值以下时触发。',   value: 30, unit: 'Lux',        enabled: true },
  { id: nextId++, key: 'traffic_lt', label: '人车流量阈值（雷达感知）', desc: '区域内5分钟平均流量低于设定值。',       value: 10, unit: '次/5min',    enabled: false },
])

function addCondition(typeKey) {
  const def = AVAILABLE_CONDITIONS.find(c => c.key === typeKey)
  if (!def) return
  if (conditionItems.value.some(c => c.key === typeKey)) {
    ElMessage.warning('该条件已存在')
    return
  }
  conditionItems.value.push({
    id: nextId++,
    key: def.key,
    label: def.label,
    desc: def.desc,
    value: def.value,
    unit: def.unit,
    enabled: true,
    isBoolean: def.isBoolean || false,
  })
}

function removeCondition(id) {
  conditionItems.value = conditionItems.value.filter(c => c.id !== id)
}

function unusedConditions() {
  return AVAILABLE_CONDITIONS.filter(o => !conditionItems.value.some(c => c.key === o.key))
}

onMounted(async () => {
  const res = await fetchStrategyGroups()
  groups.value = res.data || []
  if (groups.value.length) form.group = groups.value[0]

  if (route.params.id) {
    isEdit.value = true
    const detailRes = await fetchStrategyDetail(route.params.id)
    if (detailRes && detailRes.data) {
      const data = detailRes.data
      form.name = data.name || ''
      if (data.conditions && typeof data.conditions === 'string') {
        try {
          const cond = JSON.parse(data.conditions)
          if (cond.group) form.group = cond.group
          if (cond.startTime) form.startTime = cond.startTime
          if (cond.endTime) form.endTime = cond.endTime
          if (cond.extraActions) {
            form.actions.voiceAlert = !!cond.extraActions.voiceAlert
            form.actions.nightVision = !!cond.extraActions.nightVision
            form.actions.generateAlert = !!cond.extraActions.generateAlert
          }

          // 解析条件：兼容新格式 (lux_lt: 30) 和旧嵌套格式 (illuminance: {enabled, threshold})
          const items = []
          const metaKeys = ['group', 'startTime', 'endTime', 'extraActions']
          for (const [key, val] of Object.entries(cond)) {
            if (metaKeys.includes(key)) continue
            if (typeof val === 'number' || typeof val === 'string') {
              const def = AVAILABLE_CONDITIONS.find(c => c.key === key)
              items.push({
                id: nextId++,
                key,
                label: def ? def.label : key,
                desc: def ? def.desc : '',
                value: Number(val),
                unit: def ? def.unit : '',
                enabled: true,
                isBoolean: def ? def.isBoolean || false : false,
              })
            } else if (val && typeof val === 'object' && val.enabled) {
              const mapped = mapOldCondition(key, val)
              if (mapped) items.push({ id: nextId++, ...mapped })
            }
          }
          if (items.length > 0) conditionItems.value = items
        } catch(e) {}
      }

      if (data.action === 'ON') {
        form.actions.brightness = 100
      } else if (data.action === 'OFF') {
        form.actions.brightness = 0
      } else if (data.action && data.action.startsWith('DIMMING(')) {
        const val = parseInt(data.action.replace('DIMMING(', '').replace(')', ''))
        if (!isNaN(val)) form.actions.brightness = val
      }
    }
  }
})

async function saveStrategy() {
  if (!form.name.trim()) return alert('请输入策略名称')
  saving.value = true

  let actionStr = 'DIMMING(' + form.actions.brightness + ')'
  if (form.actions.brightness === 0) actionStr = 'OFF'
  else if (form.actions.brightness === 100) actionStr = 'ON'

  // 构建引擎兼容的扁平条件 JSON
  const conditionsObj = {
    group: form.group,
    startTime: form.startTime,
    endTime: form.endTime,
  }
  conditionItems.value.filter(c => c.enabled).forEach(c => {
    conditionsObj[c.key] = c.isBoolean ? 1 : c.value
  })
  conditionsObj.extraActions = {
    voiceAlert: form.actions.voiceAlert,
    nightVision: form.actions.nightVision,
    generateAlert: form.actions.generateAlert,
  }

  const payload = {
    name: form.name,
    policyType: 'SCENE',
    conditions: JSON.stringify(conditionsObj),
    action: actionStr,
    effectiveTime: `${form.startTime}-${form.endTime}`,
  }

  try {
    if (isEdit.value) {
      await updateStrategy(route.params.id, payload)
    } else {
      await createStrategy(payload)
    }
    saveSuccess.value = true
    setTimeout(() => {
      router.push('/strategy')
    }, 1200)
  } catch (e) {
    alert('保存失败，请重试')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="strategy-create-page">
    <div class="page-header">
      <button class="back-btn" @click="router.push('/strategy')">
        <svg viewBox="0 0 24 24" fill="none"><path d="M19 12H5M12 5l-7 7 7 7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
      </button>
      <div>
        <h1 class="page-title">{{ isEdit ? '编辑策略配置' : '新建策略配置' }}</h1>
        <p class="page-sub">配置基于环境感知与时间调度的路灯联动规则。</p>
      </div>
    </div>

    <div class="form-content">
      <!-- ① 基础信息 -->
      <div class="form-section">
        <div class="section-title">
          <span class="section-icon info">ℹ</span>
          基础信息
        </div>
        <div class="field-grid">
          <div class="field-group">
            <label>策略名称</label>
            <input v-model="form.name" class="field-input" placeholder="例：深夜节能模式" />
          </div>
          <div class="field-group">
            <label>策略组</label>
            <select v-model="form.group" class="field-select">
              <option v-for="g in groups" :key="g">{{ g }}</option>
            </select>
          </div>
          <div class="field-group">
            <label>生效开始时间</label>
            <div class="time-input-wrap">
              <input v-model="form.startTime" class="field-input" type="time" />
            </div>
          </div>
          <div class="field-group">
            <label>生效结束时间</label>
            <div class="time-input-wrap">
              <input v-model="form.endTime" class="field-input" type="time" />
            </div>
          </div>
        </div>
      </div>

      <!-- ② 触发条件 -->
      <div class="form-section">
        <div class="section-title">
          <span class="section-icon radio">((·))</span>
          触发条件 <span class="logic-tag">AND 逻辑</span>
        </div>

        <div v-for="item in conditionItems" :key="item.id"
             class="condition-card" :class="{ active: item.enabled }">
          <label class="condition-check">
            <input type="checkbox" v-model="item.enabled" class="real-checkbox" />
            <span class="checkbox-custom"></span>
          </label>
          <div class="condition-body">
            <div class="condition-name">{{ item.label }}</div>
            <div class="condition-desc">{{ item.desc }}</div>
          </div>
          <div class="condition-params" v-if="item.enabled && !item.isBoolean">
            <div class="param-field">
              <input v-model.number="item.value" class="param-input" type="number" min="0" />
              <span class="param-unit">{{ item.unit }}</span>
            </div>
          </div>
          <div class="condition-params" v-if="item.enabled && item.isBoolean">
            <span class="param-bool">{{ item.value === 1 ? '已启用（检测到人体时触发）' : '已启用' }}</span>
          </div>
          <button class="cond-remove-btn" @click="removeCondition(item.id)" title="移除此条件">
            <svg viewBox="0 0 24 24" fill="none"><path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          </button>
        </div>

        <div class="add-condition-wrap">
          <button class="add-condition-btn" @click="showAddMenu = !showAddMenu">
            <svg viewBox="0 0 24 24" fill="none"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
            添加更多条件
          </button>
          <div class="add-condition-dropdown" v-if="showAddMenu">
            <div v-for="opt in unusedConditions()" :key="opt.key"
                 class="add-option" @click="addCondition(opt.key); showAddMenu = false">
              <div>
                <div class="add-option-label">{{ opt.label }}</div>
                <div class="add-option-desc">{{ opt.desc }}</div>
              </div>
              <span class="add-option-unit">{{ opt.unit || '布尔' }}</span>
            </div>
            <div v-if="unusedConditions().length === 0" class="add-option-empty">
              所有条件类型已添加
            </div>
          </div>
        </div>
      </div>

      <!-- ③ 动作执行 -->
      <div class="form-section">
        <div class="section-title">
          <span class="section-icon action">⚙</span>
          动作执行
        </div>
        <div class="action-body">
          <div class="action-left">
            <div class="action-label-row">
              <span>目标亮度调节</span>
              <span class="brightness-pct">{{ form.actions.brightness }}%</span>
            </div>
            <input
              type="range" min="0" max="100" step="5"
              v-model="form.actions.brightness"
              class="brightness-slider"
              :style="{ '--val': form.actions.brightness + '%' }"
            />
            <div class="slider-marks">
              <span>0%（关闭）</span>
              <span>50%</span>
              <span>100%（全亮）</span>
            </div>
          </div>

          <div class="action-right">
            <div class="action-sub-title">附加联动动作</div>
            <div class="action-checks">
              <label class="action-check-item">
                <input type="checkbox" v-model="form.actions.voiceAlert" class="real-checkbox" />
                <span class="checkbox-custom"></span>
                <svg class="ac-icon" viewBox="0 0 24 24" fill="none"><path d="M12 1a3 3 0 00-3 3v8a3 3 0 006 0V4a3 3 0 00-3-3z" stroke="currentColor" stroke-width="1.5"/><path d="M19 10v2a7 7 0 01-14 0v-2M12 19v4M8 23h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                开启语音播报提示（针对违规停车）
              </label>
              <label class="action-check-item">
                <input type="checkbox" v-model="form.actions.nightVision" class="real-checkbox" />
                <span class="checkbox-custom"></span>
                <svg class="ac-icon" viewBox="0 0 24 24" fill="none"><rect x="2" y="4" width="20" height="16" rx="2" stroke="currentColor" stroke-width="1.5"/><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.5"/></svg>
                切换监控摄像头至夜视红外模式
              </label>
              <label class="action-check-item">
                <input type="checkbox" v-model="form.actions.generateAlert" class="real-checkbox" />
                <span class="checkbox-custom"></span>
                <svg class="ac-icon" viewBox="0 0 24 24" fill="none"><path d="M12 2L2 20h20L12 2z" stroke="currentColor" stroke-width="1.5"/><path d="M12 9v5M12 17v.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
                产生异常告警记录
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 保存按钮 -->
    <div class="footer-save">
      <button class="save-btn" :class="{ success: saveSuccess }" @click="saveStrategy" :disabled="saving">
        <svg v-if="!saveSuccess" viewBox="0 0 24 24" fill="none"><path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.5"/><polyline points="17 21 17 13 7 13 7 21" stroke="currentColor" stroke-width="1.5"/><polyline points="7 3 7 8 15 8" stroke="currentColor" stroke-width="1.5"/></svg>
        <svg v-else viewBox="0 0 24 24" fill="none"><path d="M20 6L9 17l-5-5" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
        {{ saveSuccess ? '保存成功！' : saving ? '保存中...' : '保存策略配置' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.strategy-create-page { padding: 24px 28px 40px; max-width: 900px; }

.page-header { display: flex; align-items: flex-start; gap: 12px; margin-bottom: 24px; }
.back-btn {
  width: 34px; height: 34px; margin-top: 4px;
  background: rgba(0,80,140,0.2); border: 1px solid rgba(0,120,200,0.25);
  border-radius: 7px; color: rgba(140,190,220,0.8);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s; flex-shrink: 0;
}
.back-btn:hover { color: #4dd0e1; border-color: rgba(77,208,225,0.4); }
.back-btn svg { width: 16px; height: 16px; }
.page-title { font-size: 22px; font-weight: 700; color: #e0f4ff; margin-bottom: 4px; }
.page-sub { font-size: 13px; color: rgba(140,190,220,0.6); }

/* Sections */
.form-section {
  background: rgba(8,20,45,0.8);
  border: 1px solid rgba(0,120,200,0.15);
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 14px;
}
.section-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 15px; font-weight: 600; color: #4dd0e1;
  margin-bottom: 18px;
}
.section-icon { font-size: 14px; }
.logic-tag {
  font-size: 11px; font-weight: 400;
  background: rgba(77,208,225,0.12);
  border: 1px solid rgba(77,208,225,0.25);
  padding: 2px 8px; border-radius: 10px;
  color: rgba(77,208,225,0.8);
}

/* Field grid */
.field-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.field-group { display: flex; flex-direction: column; gap: 6px; }
.field-group label { font-size: 12px; color: rgba(140,190,220,0.7); }
.field-input {
  height: 40px; padding: 0 12px;
  background: rgba(0,20,50,0.7);
  border: 1px solid rgba(0,100,160,0.3);
  border-radius: 7px; color: #d0eaf8;
  font-size: 13px; outline: none;
  transition: border-color 0.2s;
}
.field-input:focus { border-color: rgba(77,208,225,0.5); }
.field-input::placeholder { color: rgba(100,160,200,0.4); }
.field-select {
  height: 40px; padding: 0 12px;
  background: rgba(0,20,50,0.7);
  border: 1px solid rgba(0,100,160,0.3);
  border-radius: 7px; color: #d0eaf8;
  font-size: 13px; outline: none; cursor: pointer;
  appearance: auto;
}
.time-input-wrap { position: relative; }

/* Conditions */
.condition-card {
  display: flex; align-items: flex-start; gap: 12px;
  background: rgba(0,20,50,0.5);
  border: 1px solid rgba(0,80,140,0.2);
  border-radius: 8px; padding: 14px 16px;
  margin-bottom: 10px;
  transition: border-color 0.2s;
}
.condition-card.active { border-color: rgba(77,208,225,0.3); }
.condition-check { flex-shrink: 0; margin-top: 2px; }
.real-checkbox { display: none; }
.checkbox-custom {
  width: 16px; height: 16px;
  border: 1.5px solid rgba(0,120,180,0.6);
  border-radius: 3px;
  background: rgba(0,30,60,0.6);
  display: block; position: relative;
  transition: all 0.2s; cursor: pointer;
}
.real-checkbox:checked + .checkbox-custom {
  background: rgba(0,150,220,0.4);
  border-color: #4dd0e1;
}
.real-checkbox:checked + .checkbox-custom::after {
  content: '';
  position: absolute;
  left: 3px; top: 1px;
  width: 5px; height: 9px;
  border: 1.5px solid #4dd0e1;
  border-left: none; border-top: none;
  transform: rotate(45deg);
}
.condition-body { flex: 1; }
.condition-name { font-size: 13px; font-weight: 500; color: #d0eaf8; margin-bottom: 3px; }
.condition-desc { font-size: 11px; color: rgba(140,190,220,0.55); }
.condition-params { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.param-field { display: flex; align-items: center; gap: 4px; }
.param-input {
  width: 60px; height: 32px;
  background: rgba(0,30,70,0.8);
  border: 1px solid rgba(0,120,200,0.3);
  border-radius: 5px; color: #d0eaf8;
  font-size: 13px; text-align: center; outline: none;
  padding: 0 6px;
}
.param-unit { font-size: 11px; color: rgba(140,190,220,0.6); white-space: nowrap; }
.param-bool { font-size: 12px; color: rgba(77,208,225,0.7); }

.cond-remove-btn {
  width: 24px; height: 24px;
  background: none; border: 1px solid rgba(200,60,60,0.2);
  border-radius: 4px;
  color: rgba(200,100,100,0.6);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s; flex-shrink: 0; align-self: center;
}
.cond-remove-btn svg { width: 12px; height: 12px; }
.cond-remove-btn:hover { border-color: rgba(200,60,60,0.5); color: #ff7070; background: rgba(180,30,30,0.15); }

/* Add condition */
.add-condition-wrap { position: relative; }
.add-condition-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px;
  background: none;
  border: 1px dashed rgba(0,120,200,0.3);
  border-radius: 7px;
  color: rgba(77,208,225,0.7); font-size: 13px;
  cursor: pointer; transition: all 0.2s;
}
.add-condition-btn svg { width: 14px; height: 14px; }
.add-condition-btn:hover { border-color: rgba(77,208,225,0.5); color: #4dd0e1; background: rgba(0,120,200,0.06); }

.add-condition-dropdown {
  position: absolute; top: 100%; left: 0;
  margin-top: 4px;
  min-width: 280px;
  background: rgba(10,24,55,0.98);
  border: 1px solid rgba(0,120,200,0.3);
  border-radius: 8px;
  padding: 6px 0;
  z-index: 10;
  box-shadow: 0 8px 24px rgba(0,0,0,0.5);
}
.add-option {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 14px;
  cursor: pointer;
  transition: background 0.15s;
}
.add-option:hover { background: rgba(0,120,200,0.12); }
.add-option-label { font-size: 13px; color: #d0eaf8; }
.add-option-desc { font-size: 11px; color: rgba(140,190,220,0.5); }
.add-option-unit { font-size: 11px; color: rgba(77,208,225,0.6); flex-shrink: 0; margin-left: 12px; }
.add-option-empty { padding: 10px 14px; font-size: 12px; color: rgba(140,190,220,0.4); text-align: center; }

/* Actions */
.action-body { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
.action-label-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; font-size: 13px; color: rgba(160,210,235,0.8); }
.brightness-pct { font-size: 14px; font-weight: 700; color: #4dd0e1; }
.brightness-slider {
  width: 100%; height: 4px;
  -webkit-appearance: none; border-radius: 2px;
  background: linear-gradient(to right, #4dd0e1 0%, #4dd0e1 var(--val, 30%), rgba(0,80,140,0.4) var(--val, 30%));
  outline: none; cursor: pointer; accent-color: #4dd0e1;
  margin-bottom: 8px;
}
.brightness-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 16px; height: 16px; border-radius: 50%;
  background: #4dd0e1; box-shadow: 0 0 8px rgba(77,208,225,0.6); cursor: pointer;
}
.slider-marks { display: flex; justify-content: space-between; font-size: 11px; color: rgba(140,190,220,0.5); }

.action-sub-title { font-size: 12px; color: rgba(140,190,220,0.7); margin-bottom: 12px; }
.action-checks { display: flex; flex-direction: column; gap: 10px; }
.action-check-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px;
  background: rgba(0,20,50,0.5);
  border: 1px solid rgba(0,80,140,0.2);
  border-radius: 7px;
  cursor: pointer;
  transition: border-color 0.2s;
  font-size: 12px; color: rgba(160,210,235,0.8);
}
.action-check-item:has(.real-checkbox:checked) { border-color: rgba(77,208,225,0.3); }
.ac-icon { width: 15px; height: 15px; flex-shrink: 0; color: rgba(140,190,220,0.6); }

/* Save */
.footer-save { display: flex; justify-content: center; margin-top: 24px; }
.save-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 14px 40px;
  background: linear-gradient(135deg, #0077cc, #0099e6);
  border: none; border-radius: 10px;
  color: #fff; font-size: 15px; font-weight: 600;
  cursor: pointer; transition: all 0.3s;
  box-shadow: 0 4px 20px rgba(0,150,230,0.4);
  letter-spacing: 1px;
}
.save-btn svg { width: 18px; height: 18px; }
.save-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 28px rgba(0,150,230,0.6); }
.save-btn.success { background: linear-gradient(135deg, #2e7d32, #43a047); box-shadow: 0 4px 20px rgba(76,175,80,0.4); }
.save-btn:disabled { opacity: 0.7; cursor: not-allowed; }
</style>
