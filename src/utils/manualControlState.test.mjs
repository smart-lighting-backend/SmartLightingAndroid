import assert from 'node:assert/strict'
import {
  isManualModeActive,
  resolveManualControlState,
  stateFromLatestData,
} from './manualControlState.js'

const now = new Date('2026-07-06T10:00:00')

assert.equal(
  isManualModeActive({ manualMode: true, manualExpireAt: '2026-07-06T10:30:00' }, now),
  true,
)

assert.deepEqual(
  stateFromLatestData(JSON.stringify({ action: 'ON', brightness: 90 })),
  { power: true, brightness: 90 },
)

assert.deepEqual(
  resolveManualControlState(
    {
      latestData: JSON.stringify({ action: 'ON', brightness: 100 }),
      manualMode: true,
      manualExpireAt: '2026-07-06T10:30:00',
    },
    { command: 'turn_off', params: {}, created_at: '2026-07-06 10:01:00' },
    75,
    now,
  ),
  { power: false, brightness: 0 },
)

assert.deepEqual(
  resolveManualControlState(
    {
      latestData: JSON.stringify({ action: 'ON', brightness: 100 }),
      manualMode: false,
      manualExpireAt: null,
    },
    { command: 'turn_off', params: {}, created_at: '2026-07-06 10:01:00' },
    75,
    now,
  ),
  { power: true, brightness: 100 },
)

console.log('manualControlState tests passed')
