<template>
  <div>
    <div class="pagehead">
      <div><h1>借用维护单</h1><p>默认按「待本人处理优先、更新时间倒序」排列 · 共 {{ total }} 单</p></div>
      <div class="head-actions">
        <el-button type="primary" @click="$router.push('/borrow/create')"><el-icon><Plus /></el-icon>新建借用申请</el-button>
        <el-button @click="exportCsv">导出当前结果</el-button>
      </div>
    </div>

    <div class="card">
      <el-radio-group v-model="tab" size="small" style="margin-bottom: 14px" @change="search">
        <el-radio-button v-for="t in tabs" :key="t.key" :label="t.key">{{ t.label }} {{ t.count ?? '' }}</el-radio-button>
      </el-radio-group>
      <div class="filters">
        <el-input v-model="q.keyword" placeholder="业务编号 / 使用人 / 手机号" clearable @keyup.enter="search" />
        <el-select v-model="q.category" placeholder="器具类别" clearable>
          <el-option label="全部类别" value="" /><el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="提交开始" end-placeholder="提交结束" style="width: 260px" />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="appNo" label="业务编号" min-width="140" fixed />
        <el-table-column prop="applicantName" label="使用人" width="90" />
        <el-table-column prop="deviceCategory" label="器具类别" width="96" />
        <el-table-column prop="userCondition" label="身体条件" min-width="170" show-overflow-tooltip />
        <el-table-column label="借用期限" width="196">
          <template #default="{ row }">{{ formatDate(row.expectStart) }} ~ {{ formatDate(row.expectEnd) }}</template>
        </el-table-column>
        <el-table-column prop="currentStage" label="当前阶段" width="110" />
        <el-table-column prop="ownerName" label="责任人" width="90" />
        <el-table-column label="状态" width="112">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" effect="light">{{ statusMeta(row.status).text }}</el-tag>
            <el-tag v-if="row.overdue === 1" type="danger" size="small" style="margin-left: 4px">逾期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160"><template #default="{ row }">{{ formatTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/borrow/detail/' + row.id)">查看</el-button>
            <el-button v-if="row.status === 'PENDING_ASSESS'" link type="primary" @click="$router.push('/borrow/assessment')">评估</el-button>
            <el-button v-if="row.status === 'PENDING_APPROVE'" link type="primary" @click="$router.push('/borrow/approval')">审批</el-button>
            <el-button v-if="row.status === 'PENDING_HANDOVER'" link type="primary" @click="$router.push('/borrow/handover')">交接</el-button>
            <el-button v-if="row.status === 'IN_USE'" link type="primary" @click="openExtend(row)">延期</el-button>
            <el-button v-if="row.status === 'IN_USE'" link type="primary" @click="$router.push('/borrow/return')">归还</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination background layout="total, sizes, prev, pager, next" :total="total"
        v-model:current-page="q.pageNum" v-model:page-size="q.pageSize" :page-sizes="[10,20,50]" @current-change="load" @size-change="search" /></div>
    </div>

    <el-dialog v-model="extVisible" title="申请延期" width="480px">
      <el-form :model="ext" label-width="96px">
        <el-form-item label="业务编号"><b>{{ extApp?.appNo }}</b></el-form-item>
        <el-form-item label="当前到期">{{ formatDate(extApp?.expectEnd) }}</el-form-item>
        <el-form-item label="申请新到期">
          <el-date-picker v-model="ext.newEnd" type="date" value-format="YYYY-MM-DD" style="width: 200px" />
        </el-form-item>
        <el-form-item label="延期原因">
          <el-input v-model="ext.reason" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="不少于 5 个字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="extVisible = false">取消</el-button>
        <el-button type="primary" @click="submitExtend">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { formatDate, formatTime, statusMeta } from '../utils'

const loading = ref(false), rows = ref([]), total = ref(0), range = ref([]), categories = ref([])
const tab = ref('ALL')
const q = reactive({ pageNum: 1, pageSize: 10, keyword: '', category: '', status: 'ALL' })
const tabs = [
  { key: 'ALL', label: '全部' }, { key: 'PENDING_ASSESS', label: '待评估' },
  { key: 'PENDING_APPROVE', label: '待审批' }, { key: 'PENDING_HANDOVER', label: '待交接' },
  { key: 'IN_USE', label: '借用中' }, { key: 'COMPLETED', label: '已完成' }, { key: 'RETURNED', label: '异常' }
]
const extVisible = ref(false), extApp = ref(null), ext = reactive({ newEnd: '', reason: '' })

async function load() {
  loading.value = true
  try {
    const p = await api.borrowPage({ ...q, status: tab.value, dateFrom: range.value?.[0], dateTo: range.value?.[1] })
    rows.value = p.records
    total.value = p.total
  } finally { loading.value = false }
}
function search() { q.pageNum = 1; load() }
function reset() { Object.assign(q, { pageNum: 1, keyword: '', category: '', status: 'ALL' }); range.value = []; tab.value = 'ALL'; load() }

function openExtend(row) { extApp.value = row; ext.newEnd = ''; ext.reason = ''; extVisible.value = true }
async function submitExtend() {
  if (!ext.newEnd) { ElMessage.warning('请选择新的到期日期'); return }
  await api.borrowExtension(extApp.value.id, { ...ext })
  ElMessage.success('延期申请已提交，等待管理员审批')
  extVisible.value = false
  load()
}

function exportCsv() {
  const head = ['业务编号', '使用人', '器具类别', '当前阶段', '责任人', '状态', '借用开始', '借用到期', '提交时间']
  const body = rows.value.map(r => [r.appNo, r.applicantName, r.deviceCategory, r.currentStage, r.ownerName, statusMeta(r.status).text, formatDate(r.expectStart), formatDate(r.expectEnd), formatTime(r.createTime)])
  const csv = '\ufeff' + [head, ...body].map(l => l.join(',')).join('\n')
  const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  a.download = '借用维护单.csv'; a.click(); ElMessage.success('导出任务已生成（当前筛选条件）')
}

onMounted(async () => {
  await load()
  const opt = await api.deviceOptions()
  categories.value = opt.categories || []
})
</script>

<style scoped>.pager{display:flex;justify-content:flex-end;margin-top:14px}</style>
