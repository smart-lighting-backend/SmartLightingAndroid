/**
 * api/dashboard.js — 首页/数字孪生统计数据
 */
import request from './request.js'
import { reportMock } from '../utils/mockStore.js'

function rand(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min }

const MOCK_STATS = {
  totalDevices: 1248,
  onlineDevices: 1186,
  onlineRate: 95.0,
  energySavingRate: 32.5,
  alertCount: 3,
  todayEnergy: 4286.5,
}

function genEnergyTrend() {
  const hours = Array.from({ length: 24 }, (_, i) => `${String(i).padStart(2,'0')}:00`)
  return {
    labels: hours,
    current:  hours.map(() => rand(180, 420)),
    lastWeek: hours.map(() => rand(200, 450)),
  }
}

function genDistrictData() {
  return [
    { name: '高新区', online: 386, offline: 12, warning: 2 },
    { name: '创业园区', online: 201, offline: 8, warning: 1 },
    { name: '市中心', online: 312, offline: 5, warning: 0 },
    { name: '工业园', online: 187, offline: 9, warning: 0 },
    { name: '学院路段', online: 100, offline: 2, warning: 0 },
  ]
}

async function safeCall(apiFn, mockData, endpoint) {
  try { return await apiFn() }
  catch {
    if (endpoint) reportMock(endpoint)
    return { code: 200, msg: 'mock', data: mockData }
  }
}

export function fetchDashboardStats() {
  return safeCall(() => request.get('/api/dashboard/stats'), MOCK_STATS, 'GET /api/dashboard/stats')
}

export function fetchEnergyTrend() {
  return safeCall(() => request.get('/api/dashboard/energy-trend'), genEnergyTrend(), 'GET /api/dashboard/energy-trend')
}

export function fetchDistrictData() {
  return safeCall(() => request.get('/api/dashboard/districts'), genDistrictData(), 'GET /api/dashboard/districts')
}
