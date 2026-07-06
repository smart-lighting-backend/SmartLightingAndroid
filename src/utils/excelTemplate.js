const TEMPLATE_HEADERS = ['设备编号', '设备名称', '所属区域', '经度', '纬度', '额定功率(W)']

// 导出表头
const EXPORT_HEADERS = ['设备编号', '设备名称', '所属区域', '安装位置', '状态', '健康分', '额定功率(W)', '是否启用', '最后心跳', '订阅前缀']
const STATUS_LABELS = { 0: '停用', 1: '在线', 2: '离线', 3: '异常' }

/**
 * 下载批量导入模板 (.csv)
 */
export function downloadTemplate() {
  const sampleRow = ['SL-007', '北门-03', 'A区', '106.5622', '29.5621', '60']
  downloadCsv([TEMPLATE_HEADERS, sampleRow], '设备批量导入模板.csv')
}

/**
 * 解析上传的 Excel/CSV 文件
 * @param {File} file
 * @returns {Promise<Array>} 解析后的设备数据数组
 */
export function parseImportFile(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const rows = parseCsv(e.target.result)

        if (rows.length < 2) {
          reject(new Error('文件为空或只有表头'))
          return
        }

        // 第一行是表头，从第二行开始解析
        const headerRow = rows[0]
        // 建立列索引映射
        const colMap = {}
        TEMPLATE_HEADERS.forEach((h, i) => {
          const idx = headerRow.findIndex(cell => String(cell).trim() === h)
          if (idx >= 0) colMap[i] = idx
        })

        const result = []
        for (let i = 1; i < rows.length; i++) {
          const row = rows[i]
          if (!row || row.every(cell => String(cell).trim() === '')) continue // 跳过空行

          const getVal = (fi) => {
            const ci = colMap[fi]
            if (ci === undefined) return ''
            return String(row[ci] ?? '').trim()
          }

          const device = {
            deviceId: getVal(0),
            name: getVal(1),
            area: getVal(2),
            longitude: getVal(3),
            latitude: getVal(4),
            ratedPower: getVal(5),
            _row: i + 1, // Excel 行号（1-based + 表头）
          }

          // 只有 deviceId 非空才算有效行
          if (device.deviceId) result.push(device)
        }

        resolve(result)
      } catch (err) {
        reject(new Error('文件解析失败：' + err.message))
      }
    }
    reader.onerror = () => reject(new Error('文件读取失败'))

    const ext = file.name.split('.').pop()?.toLowerCase()
    if (ext && ext !== 'csv') {
      reject(new Error('当前环境未安装 xlsx 依赖，暂只支持 CSV 文件。请将 Excel 另存为 CSV 后导入。'))
      return
    }
    reader.readAsText(file, 'utf-8')
  })
}

/**
 * 校验单条设备数据
 * @returns {{ valid: boolean, errors: string[] }}
 */
export function validateDeviceRow(row, existingDeviceIds) {
  const errors = []

  if (!row.deviceId) {
    errors.push('设备编号不能为空')
  } else if (row.deviceId.length > 50) {
    errors.push('设备编号不能超过50个字符')
  } else if (existingDeviceIds.has(row.deviceId)) {
    errors.push('设备编号重复')
  }

  if (row.longitude) {
    const lng = parseFloat(row.longitude)
    if (isNaN(lng) || lng < 73.5 || lng > 135) {
      errors.push('经度需在 73.5°~135° 之间（中国境内）')
    }
  }
  if (row.latitude) {
    const lat = parseFloat(row.latitude)
    if (isNaN(lat) || lat < 18 || lat > 54) {
      errors.push('纬度需在 18°~54° 之间（中国境内）')
    }
  }
  if (row.ratedPower) {
    const p = parseFloat(row.ratedPower)
    if (isNaN(p) || p <= 0) {
      errors.push('额定功率需为正数')
    }
  }

  return { valid: errors.length === 0, errors }
}

/**
 * 全量校验（含文件内重复检测）
 * @returns {Array} 每行的校验结果
 */
export function validateAllRows(rows, existingDeviceIds) {
  const seenIds = new Set(existingDeviceIds)
  return rows.map((row, i) => {
    // 检查当前批次内的重复
    if (row.deviceId && seenIds.has(row.deviceId)) {
      return { valid: false, errors: [`设备编号 "${row.deviceId}" 重复（文件内第 ${i + 1} 行与前面行重复）`] }
    }
    if (row.deviceId) seenIds.add(row.deviceId)
    const result = validateDeviceRow(row, new Set()) // 不传 existingDeviceIds 因为上面已经处理了
    if (row.deviceId && existingDeviceIds.has(row.deviceId)) {
      result.valid = false
      result.errors.unshift(`设备编号 "${row.deviceId}" 已存在`)
    }
    // 合并文件内重复检测
    if (!result.valid) return result
    // 重新做完整校验
    return validateDeviceRow(row, existingDeviceIds)
  })
}

/**
 * 将设备数据转为上传 payload
 */
export function rowsToPayload(rows) {
  return rows.map(r => {
    const lng = r.longitude || ''
    const lat = r.latitude || ''
    return {
      deviceId: r.deviceId,
      name: r.name || undefined,
      area: r.area || undefined,
      location: (lng && lat) ? `${lng},${lat}` : undefined,
      ratedPower: r.ratedPower ? parseFloat(r.ratedPower) : undefined,
      topicPrefix: 'streetlight',
    }
  })
}

/**
 * 导出设备数据为 CSV 文件
 * @param {Array} devices 设备列表
 * @param {string} area 区域筛选（空=全部）
 */
export function exportDevices(devices, area = '') {
  let list = devices
  if (area) list = devices.filter(d => d.area === area)

  const data = list.map(d => [
    d.deviceId || '',
    d.name || '',
    d.area || '',
    d.location || '',
    STATUS_LABELS[d.status] || '未知',
    d.healthScore != null ? d.healthScore : '',
    d.ratedPower != null ? d.ratedPower : '',
    d.enabled !== false ? '是' : '否',
    d.lastHeartbeatAt ? formatExportTime(d.lastHeartbeatAt) : '',
    d.topicPrefix || 'streetlight',
  ])

  const filename = area ? `设备清单_${area}.csv` : '设备清单_全部.csv'
  downloadCsv([EXPORT_HEADERS, ...data], filename)
}

function formatExportTime(val) {
  if (!val) return ''
  if (Array.isArray(val)) {
    const [y, m, d, h, mi] = val
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(mi).padStart(2, '0')}`
  }
  return String(val).replace('T', ' ').slice(0, 16)
}

function downloadCsv(rows, filename) {
  const csv = rows.map(row => row.map(escapeCsvCell).join(',')).join('\r\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function escapeCsvCell(value) {
  const text = String(value ?? '')
  if (/[",\r\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

function parseCsv(text) {
  const rows = []
  let row = []
  let cell = ''
  let inQuotes = false

  const source = String(text || '').replace(/^\ufeff/, '')

  for (let i = 0; i < source.length; i++) {
    const ch = source[i]
    const next = source[i + 1]

    if (ch === '"') {
      if (inQuotes && next === '"') {
        cell += '"'
        i++
      } else {
        inQuotes = !inQuotes
      }
      continue
    }

    if (ch === ',' && !inQuotes) {
      row.push(cell)
      cell = ''
      continue
    }

    if ((ch === '\n' || ch === '\r') && !inQuotes) {
      if (ch === '\r' && next === '\n') i++
      row.push(cell)
      rows.push(row)
      row = []
      cell = ''
      continue
    }

    cell += ch
  }

  if (cell || row.length) {
    row.push(cell)
    rows.push(row)
  }

  return rows
}
