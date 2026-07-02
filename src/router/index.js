import { createRouter, createWebHistory } from 'vue-router'
import DeviceList from '../views/DeviceList.vue'
import DeviceDetail from '../views/DeviceDetail.vue'
import SmartCity3D from '../views/SmartCity3D.vue'
import AlarmList from '../views/AlarmList.vue'
import AlarmDetail from '../views/AlarmDetail.vue'

const routes = [
  {
    path: '/',
    redirect: '/device/list'
  },
  {
    path: '/device/list',
    name: 'DeviceList',
    component: DeviceList
  },
  {
    path: '/device/detail/:id',
    name: 'DeviceDetail',
    component: DeviceDetail
  },
  {
    path: '/city/3d',
    name: 'SmartCity3D',
    component: SmartCity3D
  },
  {
    path: '/alarm/list',
    name: 'AlarmList',
    component: AlarmList
  },
  {
    path: '/alarm/detail/:id',
    name: 'AlarmDetail',
    component: AlarmDetail
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router