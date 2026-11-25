<template>
  <div>
    <div class="pagehead"><div><h1>回访评价</h1><p>归还后评价器具适用性、使用便利、服务质量与遗留问题</p></div></div>
    <div class="card">
      <div class="filters">
        <el-input v-model="q.keyword" placeholder="使用人 / 业务编号" clearable style="width: 220px" @keyup.enter="search" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>
    <div class="card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="feedbackNo" label="评价编号" min-width="140" />
        <el-table-column prop="appNo" label="业务编号" min-width="140" />
        <el-table-column prop="applicantName" label="使用人" width="90" />
        <el-table-column prop="deviceCategory" label="器具" width="96" />
        <el-table-column label="适用性" width="130"><template #default="{ row }"><el-rate :model-value="row.fitScore" disabled size="small" /></template></el-table-column>
        <el-table-column label="便利性" width="130"><template #default="{ row }"><el-rate :model-value="row.easyScore" disabled size="small" /></template></el-table-column>
        <el-table-column label="服务" width="130"><template #default="{ row }"><el-rate :model-value="row.serviceScore" disabled size="small" /></template></el-table-column>
        <el-table-column prop="content" label="建议与问题" min-width="240" show-overflow-tooltip />
        <el-table-column label="评价时间" width="160"><template #default="{ row }">{{ formatTime(row.feedbackTime) }}</template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="q.pageNum" v-model:page-size="q.pageSize" @current-change="load" /></div>
    </div>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import { formatTime } from '../utils'
const loading = ref(false), rows = ref([]), total = ref(0)
const q = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
async function load() {
  loading.value = true
  try { const p = await api.feedbackPage(q); rows.value = p.records; total.value = p.total } finally { loading.value = false }
}
function search() { q.pageNum = 1; load() }
onMounted(load)
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;margin-top:14px}</style>
