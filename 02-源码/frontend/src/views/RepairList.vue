<template>
  <div>
    <div class="pagehead">
      <div><h1>故障维修工单</h1><p>接收工单 → 维修 → 完工检测 → 完成或报废建议；费用与零件全部留痕</p></div>
      <div class="head-actions"><el-button type="primary" @click="openNew">新建工单</el-button></div>
    </div>
    <div class="card">
      <div class="filters">
        <el-input v-model="q.keyword" placeholder="工单号 / 器具编号 / 故障" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="q.status" placeholder="工单状态" clearable style="width: 160px">
          <el-option label="全部" value="ALL" /><el-option v-for="(v, k) in REPAIR_STATUS" :key="k" :label="v.text" :value="k" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>
    <div class="card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="orderNo" label="工单号" min-width="140" />
        <el-table-column prop="deviceNo" label="器具编号" min-width="130" />
        <el-table-column prop="deviceName" label="器具" width="96" />
        <el-table-column prop="faultDesc" label="故障描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="repairMeasure" label="维修措施" min-width="180" show-overflow-tooltip />
        <el-table-column label="费用" width="96"><template #default="{ row }">¥{{ money(row.cost) }}</template></el-table-column>
        <el-table-column prop="repairerName" label="维修员" width="90" />
        <el-table-column label="状态" width="96"><template #default="{ row }">
          <el-tag :type="repairMeta(row.status).type">{{ repairMeta(row.status).text }}</el-tag></template></el-table-column>
        <el-table-column label="报修时间" width="160"><template #default="{ row }">{{ formatTime(row.reportTime) }}</template></el-table-column>
        <el-table-column label="操作" width="150" fixed="right"><template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="primary" @click="act(row, 'accept')">接单</el-button>
          <el-button v-if="row.status === 'REPAIRING'" link type="primary" @click="finish(row)">完工</el-button>
          <el-button v-if="row.status === 'CHECKING'" link type="primary" @click="act(row, 'done')">检测通过</el-button>
          <el-button v-if="row.status === 'CHECKING'" link type="danger" @click="scrap(row)">报废</el-button>
        </template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="q.pageNum" v-model:page-size="q.pageSize" @current-change="load" /></div>
    </div>

    <el-dialog v-model="finishVisible" title="提交完工信息" width="480px">
      <el-form label-width="96px">
        <el-form-item label="维修措施"><el-input v-model="fin.repairMeasure" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="更换零件"><el-input v-model="fin.partsReplaced" /></el-form-item>
        <el-form-item label="维修费用"><el-input-number v-model="fin.cost" :min="0" :precision="2" :step="10" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="finishVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFinish">提交并转检测</el-button></template>
    </el-dialog>

    <el-dialog v-model="newVisible" title="新建维修工单" width="500px">
      <el-form label-width="96px">
        <el-form-item label="器具">
          <el-select v-model="nf.deviceId" filterable style="width: 280px" placeholder="按编号搜索">
            <el-option v-for="d in devices" :key="d.id" :label="`${d.deviceNo} · ${d.name} ${d.spec}`" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="故障类型"><el-select v-model="nf.faultType" style="width: 200px">
          <el-option label="结构损坏" value="STRUCTURE" /><el-option label="液压故障" value="HYDRAULIC" />
          <el-option label="轮组故障" value="WHEEL" /><el-option label="电气故障" value="ELECTRIC" />
          <el-option label="表面破损" value="SURFACE" /></el-select></el-form-item>
        <el-form-item label="故障描述"><el-input v-model="nf.faultDesc" type="textarea" :rows="3" placeholder="不少于 5 个字" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="newVisible = false">取消</el-button>
        <el-button type="primary" @click="submitNew">提交</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { formatTime, money, repairMeta, REPAIR_STATUS } from '../utils'
const loading = ref(false), rows = ref([]), total = ref(0), devices = ref([])
const q = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: 'ALL' })
const finishVisible = ref(false), newVisible = ref(false), cur = ref(null)
const fin = reactive({ repairMeasure: '', partsReplaced: '', cost: 0 })
const nf = reactive({ deviceId: null, faultType: 'STRUCTURE', faultDesc: '' })
async function load() {
  loading.value = true
  try { const p = await api.repairPage(q); rows.value = p.records; total.value = p.total } finally { loading.value = false }
}
function search() { q.pageNum = 1; load() }
async function act(row, action) {
  await api.repairTransition(row.id, { action, remark: action === 'done' ? '承重与功能复检通过' : '' })
  ElMessage.success(action === 'accept' ? '已接单' : '工单已完成，器具转待消毒')
  load()
}
function finish(row) { cur.value = row; Object.assign(fin, { repairMeasure: row.repairMeasure || '更换损坏零件并调试', partsReplaced: row.partsReplaced || '', cost: Number(row.cost || 0) }); finishVisible.value = true }
async function submitFinish() {
  await api.repairTransition(cur.value.id, { action: 'finish', ...fin, cost: String(fin.cost) })
  ElMessage.success('已提交完工信息，等待检测')
  finishVisible.value = false
  load()
}
async function scrap(row) {
  const { value } = await ElMessageBox.prompt('请填写报废原因（不少于 5 个字）', '建议报废', { inputValidator: (v) => (v && v.trim().length >= 5) || '原因不少于 5 个字' })
  await api.repairTransition(row.id, { action: 'scrap', remark: value })
  ElMessage.success('已标记报废，器具状态同步更新')
  load()
}
async function openNew() {
  nf.deviceId = null; nf.faultType = 'STRUCTURE'; nf.faultDesc = ''
  if (!devices.value.length) { const p = await api.devicePage({ pageNum: 1, pageSize: 100 }); devices.value = p.records }
  newVisible.value = true
}
async function submitNew() {
  if (!nf.deviceId || nf.faultDesc.trim().length < 5) { ElMessage.warning('请选择器具并填写不少于 5 个字的故障描述'); return }
  await api.repairCreate({ ...nf })
  ElMessage.success('工单已创建')
  newVisible.value = false
  load()
}
onMounted(load)
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;margin-top:14px}</style>
