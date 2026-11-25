<template>
  <div>
    <div class="pagehead">
      <div><h1>借用审批台</h1><p>审批通过后系统锁定一台检测合格器具并预留 48 小时；退回必须填写原因</p></div>
      <div class="head-actions">
        <el-button :disabled="!selected.length" @click="batchApprove">批量通过（{{ selected.length }}）</el-button>
      </div>
    </div>
    <div class="card">
      <el-table :data="rows" v-loading="loading" border stripe @selection-change="(v) => (selected = v)">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="appNo" label="业务编号" min-width="140" />
        <el-table-column prop="applicantName" label="使用人" width="90" />
        <el-table-column prop="deviceCategory" label="类别" width="90" />
        <el-table-column prop="assessorName" label="评估员" width="90" />
        <el-table-column prop="borrowDays" label="借期" width="76">
          <template #default="{ row }">{{ row.borrowDays }} 天</template>
        </el-table-column>
        <el-table-column label="评分" width="96">
          <template #default="{ row }">
            <span v-if="scoreOf(row.id)">{{ scoreOf(row.id).totalScore }} · {{ scoreOf(row.id).level }}</span>
            <span v-else class="hint">未计算</span>
          </template>
        </el-table-column>
        <el-table-column label="可用库存" width="110">
          <template #default="{ row }">
            <el-tag :type="stock(row.deviceCategory) > 0 ? 'success' : 'danger'" size="small">{{ stock(row.deviceCategory) }} 台</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160"><template #default="{ row }">{{ formatTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="190" fixed="right"><template #default="{ row }">
          <el-button link type="primary" @click="open(row)">审批</el-button>
          <el-button link type="danger" @click="reject(row)">驳回</el-button>
        </template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="q.pageNum" v-model:page-size="q.pageSize" @current-change="load" /></div>
    </div>

    <el-drawer v-model="visible" title="审批详情" size="640px">
      <div v-if="cur">
        <div style="display: flex; justify-content: space-between; align-items: flex-start">
          <div>
            <div style="font-size: 17px; font-weight: 600">{{ cur.appNo }} · {{ cur.applicantName }}</div>
            <div class="hint" style="margin-top: 6px">{{ cur.deviceCategory }} · {{ cur.borrowDays }} 天 · 责任人 {{ cur.ownerName }}</div>
          </div>
          <el-tag :type="statusMeta(cur.status).type">{{ statusMeta(cur.status).text }}</el-tag>
        </div>
        <el-descriptions :column="2" border size="small" style="margin-top: 14px">
          <el-descriptions-item label="身体条件" :span="2">{{ cur.userCondition }}</el-descriptions-item>
          <el-descriptions-item label="借用期限" :span="2">{{ formatDate(cur.expectStart) }} 至 {{ formatDate(cur.expectEnd) }}</el-descriptions-item>
          <el-descriptions-item label="配送方式">{{ cur.deliveryType === 'DELIVER' ? '上门配送' : '自提' }}</el-descriptions-item>
          <el-descriptions-item label="评估员">{{ cur.assessorName }}</el-descriptions-item>
          <el-descriptions-item label="用途" :span="2">{{ cur.purpose }}</el-descriptions-item>
        </el-descriptions>
        <div class="card-title" style="margin-top: 16px">适配与库存校验</div>
        <el-alert v-if="sc" :title="`适配评分 ${sc.totalScore}（${sc.level} 级）· ${sc.suggestText}`" type="info" :closable="false" />
        <el-alert v-else title="尚未执行适配计算，建议先计算再审批" type="warning" :closable="false" />
        <el-alert style="margin-top: 8px" :type="stock(cur.deviceCategory) > 0 ? 'success' : 'error'" :closable="false"
                  :title="stock(cur.deviceCategory) > 0 ? `该类别有 ${stock(cur.deviceCategory)} 台可借器具，通过后将自动预留` : '该类别暂无可预留器具，请先入库或完成检测消毒'" />
        <div class="card-title" style="margin-top: 16px">时间轴</div>
        <el-timeline>
          <el-timeline-item v-for="(t, i) in tl" :key="i" :timestamp="formatTime(t.time)">
            <div class="timeline-title">{{ t.text }}</div><div class="timeline-meta">{{ t.who }}</div>
          </el-timeline-item>
        </el-timeline>
        <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 10px">
          <el-button @click="visible = false">关闭</el-button>
          <el-button type="danger" plain @click="reject(cur)">驳回</el-button>
          <el-button type="primary" @click="approve(cur)">审批通过</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { formatDate, formatTime, statusMeta } from '../utils'
const loading = ref(false), rows = ref([]), total = ref(0), selected = ref([])
const q = reactive({ pageNum: 1, pageSize: 10 })
const visible = ref(false), cur = ref(null), tl = ref([]), sc = ref(null)
const scores = ref({}), stockMap = ref({})

function scoreOf(id) { return scores.value[id] }
function stock(cat) { return stockMap.value[cat] || 0 }

async function load() {
  loading.value = true
  try {
    const p = await api.borrowPage({ ...q, status: 'PENDING_APPROVE' })
    rows.value = p.records; total.value = p.total
  } finally { loading.value = false }
  const dev = await api.devicePage({ pageNum: 1, pageSize: 100, status: 'AVAILABLE' })
  const m = {}
  dev.records.forEach(d => { m[d.name] = (m[d.name] || 0) + 1 })
  stockMap.value = m
  const sp = await api.scorePage({ pageNum: 1, pageSize: 100 })
  const sm = {}
  sp.records.forEach(s => { if (!sm[s.appId]) sm[s.appId] = s })
  scores.value = sm
}
async function open(row) {
  cur.value = row
  const d = await api.borrowDetail(row.id)
  tl.value = d.timeline
  sc.value = d.scores?.length ? d.scores[0] : null
  visible.value = true
}
async function approve(row) {
  await ElMessageBox.confirm(`确认审批通过 ${row.appNo}？系统将锁定一台检测合格器具`, '审批通过', { type: 'warning' })
  await api.borrowTransition(row.id, { action: 'approve', reason: '库存与检测均满足要求' })
  ElMessage.success('审批通过，器具已预留')
  visible.value = false
  load()
}
async function reject(row) {
  const { value } = await ElMessageBox.prompt('请填写驳回原因（不少于 5 个字）', '驳回申请',
    { inputValidator: (v) => (v && v.trim().length >= 5) || '原因不少于 5 个字' })
  await api.borrowTransition(row.id, { action: 'reject', reason: value })
  ElMessage.success('已驳回并释放预留')
  visible.value = false
  load()
}
async function batchApprove() {
  await ElMessageBox.confirm(`将逐条审批 ${selected.value.length} 条单据，失败项会返回具体原因`, '批量审批', { type: 'warning' })
  let ok = 0; const fail = []
  for (const r of selected.value) {
    try { await api.borrowTransition(r.id, { action: 'approve', reason: '批量审批通过' }); ok++ }
    catch (e) { fail.push(`${r.appNo}：${e.message}`) }
  }
  ElMessage[fail.length ? 'warning' : 'success'](`成功 ${ok} 条${fail.length ? '，失败 ' + fail.length + ' 条：' + fail.slice(0, 2).join('；') : ''}`)
  load()
}
onMounted(load)
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;margin-top:14px}</style>
