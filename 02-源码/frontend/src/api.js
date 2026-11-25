import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = 'Bearer ' + token
  return cfg
})

http.interceptors.response.use(
  (res) => {
    if (res.config.responseType === 'blob') return res
    const body = res.data
    if (body && body.code === 200) return body.data
    if (body && body.code === 401) {
      localStorage.removeItem('token')
      location.href = '/login'
      return Promise.reject(new Error(body.msg))
    }
    ElMessage.error(body?.msg || '请求失败')
    return Promise.reject(new Error(body?.msg || '请求失败'))
  },
  (err) => {
    const status = err.response?.status
    if (status === 401) {
      localStorage.removeItem('token')
      location.href = '/login'
    } else {
      ElMessage.error(err.response?.data?.msg || err.message || '网络异常')
    }
    return Promise.reject(err)
  }
)

export default http

// 统一接口封装：分页、上传、状态标签、时间格式都走公共层，页面不重复实现
export const api = {
  login: (d) => http.post('/auth/login', d),
  me: () => http.get('/auth/me'),
  updateProfile: (d) => http.put('/auth/profile', d),
  changePassword: (d) => http.post('/auth/password', d),

  dashboard: (range) => http.get('/dashboard', { params: { range } }),

  devicePage: (p) => http.get('/device/page', { params: p }),
  deviceDetail: (id) => http.get('/device/' + id),
  deviceSave: (d) => (d.id ? http.put('/device/' + d.id, d) : http.post('/device', d)),
  deviceInspect: (id, d) => http.post(`/device/${id}/inspect`, d),
  deviceOptions: () => http.get('/device/options'),
  inspectionPage: (p) => http.get('/inspection/page', { params: p }),
  reservationPage: (p) => http.get('/reservation/page', { params: p }),
  reservationRelease: (id, reason) => http.post(`/reservation/${id}/release`, { reason }),

  borrowPage: (p) => http.get('/borrow/page', { params: p }),
  borrowDetail: (id) => http.get('/borrow/' + id),
  borrowCreate: (d) => http.post('/borrow', d),
  borrowTransition: (id, d) => http.post(`/borrow/${id}/transition`, d),
  borrowAssessment: (id, d) => http.post(`/borrow/${id}/assessment`, d),
  borrowHandover: (id, d) => http.post(`/borrow/${id}/handover`, d),
  borrowReturn: (id, d) => http.post(`/borrow/${id}/return`, d),
  borrowExtension: (id, d) => http.post(`/borrow/${id}/extension`, d),
  extensionPage: (p) => http.get('/borrow/extension/page', { params: p }),
  extensionApprove: (id, d) => http.post(`/borrow/extension/${id}/approve`, d),
  borrowFeedback: (id, d) => http.post(`/borrow/${id}/feedback`, d),
  feedbackPage: (p) => http.get('/borrow/feedback/page', { params: p }),
  assessmentPage: (p) => http.get('/borrow/assessment/page', { params: p }),

  repairPage: (p) => http.get('/repair/page', { params: p }),
  repairCreate: (d) => http.post('/repair', d),
  repairTransition: (id, d) => http.post(`/repair/${id}/transition`, d),

  ruleList: () => http.get('/rule/list'),
  ruleCurrent: () => http.get('/rule/current'),
  rulePublish: (d) => http.post('/rule', d),
  scorePage: (p) => http.get('/score/page', { params: p }),
  scoreDetail: (id) => http.get('/score/' + id),
  scoreByApp: (id) => http.get('/score/by-app/' + id),
  evaluate: (id) => http.post(`/borrow/${id}/evaluate`),
  scoreAdjust: (id, d) => http.post(`/score/${id}/adjust`, d),
  scoreEffect: () => http.get('/score/effect'),

  userPage: (p) => http.get('/user/page', { params: p }),
  userSave: (d) => (d.id ? http.put('/user/' + d.id, d) : http.post('/user', d)),
  userReset: (id) => http.post(`/user/${id}/reset`),
  userDelete: (id) => http.delete('/user/' + id),
  roleList: () => http.get('/role/list'),
  roleMenus: (id) => http.get(`/role/${id}/menus`),
  roleAssign: (id, ids) => http.post(`/role/${id}/menus`, ids),
  menuTree: () => http.get('/menu/tree'),
  menuSave: (d) => http.post('/menu', d),
  dict: (t) => http.get('/dict/' + t),
  messages: (p) => http.get('/message/page', { params: p }),
  messageRead: (id) => http.post(`/message/${id}/read`),
  uploadUrl: '/api/upload'
}
