import axios from 'axios'

const mockDevices = [
  {
    id: 'DEV001',
    name: '智慧路灯-A001',
    region: '示范区A-1号路',
    status: 'online',
    healthScore: 95,
    lastHeartbeat: '2024-01-15 14:30:25',
    installTime: '2023-06-15 10:00:00',
    firmwareVersion: 'v2.1.0',
    ipAddress: '192.168.1.101',
    latitude: 30.5728,
    longitude: 104.0668
  },
  {
    id: 'DEV002',
    name: '智慧路灯-A002',
    region: '示范区A-1号路',
    status: 'online',
    healthScore: 88,
    lastHeartbeat: '2024-01-15 14:28:10',
    installTime: '2023-06-15 10:15:00',
    firmwareVersion: 'v2.1.0',
    ipAddress: '192.168.1.102',
    latitude: 30.5729,
    longitude: 104.0670
  },
  {
    id: 'DEV003',
    name: '智慧路灯-B001',
    region: '示范区B-2号路',
    status: 'offline',
    healthScore: 62,
    lastHeartbeat: '2024-01-15 10:15:33',
    installTime: '2023-07-20 14:30:00',
    firmwareVersion: 'v2.0.5',
    ipAddress: '192.168.1.201',
    latitude: 30.5735,
    longitude: 104.0685
  },
  {
    id: 'DEV004',
    name: '智慧路灯-B002',
    region: '示范区B-2号路',
    status: 'online',
    healthScore: 92,
    lastHeartbeat: '2024-01-15 14:29:45',
    installTime: '2023-07-20 14:45:00',
    firmwareVersion: 'v2.1.0',
    ipAddress: '192.168.1.202',
    latitude: 30.5736,
    longitude: 104.0687
  },
  {
    id: 'DEV005',
    name: '智慧路灯-C001',
    region: '示范区C-3号路',
    status: 'online',
    healthScore: 78,
    lastHeartbeat: '2024-01-15 14:27:55',
    installTime: '2023-08-10 09:00:00',
    firmwareVersion: 'v2.0.8',
    ipAddress: '192.168.1.301',
    latitude: 30.5742,
    longitude: 104.0692
  },
  {
    id: 'DEV006',
    name: '智慧路灯-C002',
    region: '示范区C-3号路',
    status: 'offline',
    healthScore: 45,
    lastHeartbeat: '2024-01-14 22:08:12',
    installTime: '2023-08-10 09:20:00',
    firmwareVersion: 'v2.0.5',
    ipAddress: '192.168.1.302',
    latitude: 30.5743,
    longitude: 104.0694
  },
  {
    id: 'DEV007',
    name: '智慧路灯-D001',
    region: '示范区D-4号路',
    status: 'online',
    healthScore: 98,
    lastHeartbeat: '2024-01-15 14:30:18',
    installTime: '2023-09-05 11:00:00',
    firmwareVersion: 'v2.1.0',
    ipAddress: '192.168.1.401',
    latitude: 30.5750,
    longitude: 104.0700
  },
  {
    id: 'DEV008',
    name: '智慧路灯-D002',
    region: '示范区D-4号路',
    status: 'online',
    healthScore: 85,
    lastHeartbeat: '2024-01-15 14:26:30',
    installTime: '2023-09-05 11:30:00',
    firmwareVersion: 'v2.1.0',
    ipAddress: '192.168.1.402',
    latitude: 30.5751,
    longitude: 104.0702
  }
]

export const fetchDeviceList = (params = {}) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let result = [...mockDevices]

      if (params.keyword) {
        const keyword = params.keyword.toLowerCase()
        result = result.filter(
          (device) =>
            device.id.toLowerCase().includes(keyword) ||
            device.region.toLowerCase().includes(keyword) ||
            device.name.toLowerCase().includes(keyword)
        )
      }

      if (params.id) {
        result = result.filter((device) => device.id === params.id)
      }

      if (params.region) {
        result = result.filter((device) => device.region === params.region)
      }

      resolve({
        code: 200,
        message: 'success',
        data: {
          list: result,
          total: result.length
        }
      })
    }, 500)
  })
}

export const fetchDeviceDetail = (id) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const device = mockDevices.find((d) => d.id === id)
      resolve({
        code: 200,
        message: 'success',
        data: device
      })
    }, 500)
  })
}