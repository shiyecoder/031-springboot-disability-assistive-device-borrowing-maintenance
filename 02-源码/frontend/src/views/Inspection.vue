<template>
  <div>
    <div class="pagehead">
      <div><h1>检测消毒记录</h1><p>按器具类别配置检查项、消毒方式与周期；检测过期或未消毒的器具不可预留</p></div>
      <div class="head-actions"><el-button @click="exportCsv">导出当前结果</el-button></div>
    </div>
    <div class="card">
      <div class="filters">
        <el-input v-model="q.keyword" placeholder="检测单号 / 器具编号" clearable @keyup.enter="search" />
        <el-select v-model="q.status" placeholder="检查结果" clearable>
          <el-option label="全部结果" value="ALL" />
          <el-option label="合格" value="PASS" /><el-option label="不合格" value="FAIL" />
        </el-select>
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>
    <div class="card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="inspectionNo" label="检测单号" min-width="140" />
        <el-table-column prop="deviceNo" label="器具编号" min-width="130" />
        <el-table-column prop="category" label="类别" width="96" />
        <el-table-column prop="checkItems" label="检查项" min-width="190" />
        <el-table-column prop="disinfectMethod" label="消毒方式" min-width="150" />
        <el-table-column label="结论" width="90">
          <template #default="{ row }">
            <el-tag :type="row.result === 'PASS' ? 'success' : 'danger'" effect="light">{{ row.result === 'PASS' ? '合格' : '不合格' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="inspector" label="执行人" width="90" />
        <el-table-column label="检测时间" width="160"><template #default="{ row }">{{ formatTime(row.inspectTime) }}</template></el-table-column>
        <el-table-column label="下次到期" width="112"><template #default="{ row }">{{ formatDate(row.nextDueDate) }}</template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination background layout="total, sizes, prev, pager, next" :total="total"
        v-model:current-page="q.pageNum" v-model:page-size="q.pageSize" :page-sizes="[10,20,50]" @current-change="load" @size-change="search" /></div>
    </div>
    <div class="card">
      <div class="card-title">检测周期配置（按器具类别）</div>
      <el-table :data="cycles" size="small" border>
        <el-table-column prop="category" label="器具类别" width="120" />
        <el-table-column prop="items" label="检查项" min-width="200" />
        <el-table-column prop="method" label="消毒方式" min-width="160" />
        <el-table-column prop="days" label="周期（天）" width="110" />
      </el-table>
      <div class="hint mt8">超过周期未检测的器具在审批环节会被拦截，不会静默放行。</div>
    </div>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { formatDate, formatTime } from '../utils'
const loading = ref(false), rows = ref([]), total = ref(0), range = ref([])
const q = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: 'ALL' })
const cycles = [
  { category: '轮椅', items: '刹车 / 轮组 / 坐垫 / 车架', method: '含氯消毒液擦拭', days: 30 },
  { category: '护理床', items: '结构紧固 / 护栏 / 电气安全', method: '紫外线照射 30 分钟', days: 60 },
  { category: '助行器', items: '脚垫 / 折叠机构 / 螺丝', method: '酒精喷洒', days: 45 },
  { category: '拐杖', items: '胶垫磨损 / 高度调节 / 握把', method: '酒精喷洒', days: 45 },
  { category: '坐便椅', items: '座圈 / 扶手 / 防滑脚垫', method: '含氯消毒液浸泡', days: 30 },
  { category: '移位机', items: '液压 / 吊带 / 承重测试', method: '含氯消毒液擦拭', days: 30 }
]
async function load() {
  loading.value = true
  try { const p = await api.inspectionPage({ ...q, dateFrom: range.value?.[0], dateTo: range.value?.[1] }); rows.value = p.records; total.value = p.total }
  finally { loading.value = false }
}
function search() { q.pageNum = 1; load() }
function reset() { Object.assign(q, { pageNum: 1, keyword: '', status: 'ALL' }); range.value = []; load() }
function exportCsv() {
  const csv = '\ufeff' + [['检测单号','器具编号','检查项','消毒方式','结论','执行人','检测时间'], ...rows.value.map(r => [r.inspectionNo, r.deviceNo, r.checkItems, r.disinfectMethod, r.result === 'PASS' ? '合格' : '不合格', r.inspector, formatTime(r.inspectTime)])].map(l => l.join(',')).join('\n')
  const a = document.createElement('a'); a.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  a.download = '检测消毒记录.csv'; a.click(); ElMessage.success('已导出')
}
onMounted(load)
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;margin-top:14px}</style>
