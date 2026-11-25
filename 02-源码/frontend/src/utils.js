// 全局格式化与状态映射：全系统统一调用，禁止页面各自实现
export function formatTime(v) {
  if (!v) return '-'
  const s = String(v).replace('T', ' ')
  return s.length >= 19 ? s.slice(0, 19) : s
}

export function formatDate(v) {
  if (!v) return '-'
  return formatTime(v).slice(0, 10)
}

export function money(v) {
  if (v === null || v === undefined || v === '') return '0.00'
  return Number(v).toFixed(2)
}

// 借用维护单状态
export const STATUS_MAP = {
  PENDING_ASSESS: { text: '待评估', type: 'warning' },
  PENDING_APPROVE: { text: '待审批', type: 'warning' },
  PENDING_HANDOVER: { text: '待交接', type: 'primary' },
  IN_USE: { text: '借用中', type: 'primary' },
  PENDING_RETURN: { text: '待归还检查', type: 'warning' },
  COMPLETED: { text: '已完成', type: 'success' },
  REJECTED: { text: '已驳回', type: 'danger' },
  RETURNED: { text: '已退回', type: 'danger' },
  CANCELLED: { text: '已取消', type: 'info' }
}

// 器具状态
export const DEVICE_STATUS = {
  AVAILABLE: { text: '可借', type: 'success' },
  RESERVED: { text: '已预留', type: 'primary' },
  LENT: { text: '借出', type: 'warning' },
  TO_DISINFECT: { text: '待消毒', type: 'warning' },
  TO_REPAIR: { text: '待维修', type: 'danger' },
  SCRAPPED: { text: '已报废', type: 'info' }
}

export const LEVEL_MAP = {
  A: { text: 'A 级 · 推荐', type: 'success' },
  B: { text: 'B 级 · 适配', type: 'primary' },
  C: { text: 'C 级 · 风险', type: 'warning' },
  D: { text: 'D 级 · 不推荐', type: 'danger' }
}

export const REPAIR_STATUS = {
  PENDING: { text: '待接单', type: 'warning' },
  REPAIRING: { text: '维修中', type: 'primary' },
  CHECKING: { text: '待检测', type: 'warning' },
  DONE: { text: '已完成', type: 'success' },
  SCRAPPED: { text: '已报废', type: 'info' }
}

export const RESERVE_STATUS = {
  LOCKED: { text: '预留中', type: 'warning' },
  HANDOVER: { text: '已交接', type: 'success' },
  RELEASED: { text: '已释放', type: 'info' }
}

export const ROLE_TEXT = {
  ADMIN: '机构管理员',
  ASSESSOR: '评估员',
  WAREHOUSE: '仓管员',
  REPAIRER: '维修员',
  BORROWER: '借用人/代办人'
}

export function statusMeta(s) {
  return STATUS_MAP[s] || { text: s || '-', type: 'info' }
}

export function deviceMeta(s) {
  return DEVICE_STATUS[s] || { text: s || '-', type: 'info' }
}

export function levelMeta(l) {
  return LEVEL_MAP[l] || { text: l || '-', type: 'info' }
}

export function repairMeta(s) {
  return REPAIR_STATUS[s] || { text: s || '-', type: 'info' }
}

export function roleText(code) {
  return ROLE_TEXT[code] || code || '-'
}

// 借用天数（用于表单即时提示）
export function calcDays(start, end) {
  if (!start || !end) return 0
  const a = new Date(String(start).replace(/-/g, '/'))
  const b = new Date(String(end).replace(/-/g, '/'))
  const d = Math.round((b - a) / 86400000)
  return d > 0 ? d : 0
}

// 图表统一配色：与 design-system 保持一致，同义同色
export const CHART_COLORS = ['#173B57', '#287271', '#2F6B8A', '#7E93A2', '#287D4F', '#B42318', '#A9761F']
