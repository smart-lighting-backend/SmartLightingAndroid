import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const detailPath = resolve(__dirname, '../views/DeviceDetail.vue')
const source = readFileSync(detailPath, 'utf8')

assert.match(
  source,
  /resolveManualControlState/,
  'DeviceDetail.vue should use the shared manual-control state resolver.',
)

assert.doesNotMatch(
  source,
  /function\s+parseLatestData/,
  'DeviceDetail.vue should not keep a local latestData-only parser for control state.',
)

assert.doesNotMatch(
  source,
  /function\s+applyControlState/,
  'DeviceDetail.vue should not keep a local latestData-only control-state applier.',
)

const unawaitedReload = source
  .split(/\r?\n/)
  .find(line => line.includes('loadDeviceInfo();') && !line.trim().startsWith('await '))

assert.equal(
  unawaitedReload,
  undefined,
  'Control success should not reapply stale latestData without awaiting the unified resolver.',
)

console.log('deviceDetailControlStateUsage tests passed')
