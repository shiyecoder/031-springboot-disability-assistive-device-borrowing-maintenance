<template>
  <div>
    <div class="pagehead">
      <div><h1>库存预留</h1><p>审批通过后锁定一台「检测合格且已消毒」器具，48 小时未领用自动释放</p></div>
      <div class="head-actions"><el-button type="primary" @click="$router.push('/device/list')">去器具目录</el-button></div>
    </div>
    <div class="card">
      <div class="filters">
        <el-input v-model="q.keyword" placeholder="预留单号 / 业务编号" clearable @keyup.enter="search" />
        <el-select v-model="q.status" placeholder="预留状态" clearable>
          <el-option label="全部" value="ALL" /><el-option label="预留中" value="LOCKED" />
          <el-option label="已交接" value="HANDOVER" /><el-option label="已释放" value="RELEASED" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>
    <div class="card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="reserveNo" label="预留单号" min-width="140" />
        <el-table-column prop="appNo" label="业务编号" min-width="140" />
        <el-table-column prop="deviceNo" label="器具编号" min-width="130" />
        <el-table-column prop="deviceName" label="器具名称" width="100" />
        <el-table-column label="锁定时间" width="160"><template #default="{ row }">{{ formatTime(row.lockTime) }}</template></el-table-column>
        <el-table-column label="到期时间" width="160"><template #default="{ row }">{{ formatTime(row.expireTime) }}</template></el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }"><el-tag :type="RESERVE_STATUS[row.status]?.type">{{ RESERVE_STATUS[row.status]?.text || row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">查看</el-button>
            <el-button v-if="row.status === 'LOCKED'" link type="danger" @click="release(row)">释放</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination background layout="total, sizes, prev, pager, next" :total="total"
        v-model:current-page="q.pageNum" v-model:page-size="q.pageSize" :page-sizes="[10,20,50]" @current-change="load" @size-change="search" /></div>
    </div>
    <div class="card">
      <div class="card-title">可预留库存（按类别）</div>
      <el-table :data="stock" size="small" border>
        <el-table-column prop="category" label="类别" width="120" />
        <el-table-column prop="total" label="在册" width="90" />
        <el-table-column prop="available" label="可借" width="90" />
        <el-table-column prop="reserved" label="预留中" width="90" />
        <el-table-column prop="lent" label="借出" width="90" />
        <el-table-column label="缺口提示" min-width="180"><template #default="{ row }">
          <el-tag v-if="row.available === 0" type="danger">暂无可借，建议补货或转介</el-tag>
          <span v-else class="hint">可正常分配</span>
        </template></el-table-column>
      </el-table>
    </div>
    <el-drawer v-model="visible" title="预留详情" size="480px">
      <el-descriptions v-if="cur" :column="1" border>
        <el-descriptions-item label="预留单号">{{ cur.reserveNo }}</el-descriptions-item>
        <el-descriptions-item label="业务编号">{{ cur.appNo }}</el-descriptions-item>
        <el-descriptions-item label="器具">{{ cur.deviceNo }} · {{ cur.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="锁定时间">{{ formatTime(cur.lockTime) }}</el-descriptions-item>
        <el-descriptions-item label="到期时间">{{ formatTime(cur.expireTime) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ RESERVE_STATUS[cur.status]?.text }}</el-descriptions-item>
        <el-descriptions-item label="释放原因">{{ cur.releaseReason || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { formatTime, RESERVE_STATUS } from '../utils'
const loading = ref(false), rows = ref([]), total = ref(0), visible = ref(false), cur = ref(null), stock = ref([])
const q = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: 'ALL' })
async function load() {
  loading.value = true
  try { const p = await api.reservationPage(q); rows.value = p.records; total.value = p.total } finally { loading.value = false }
  const all = await api.deviceOptions()
  const map = {}
  all.available.concat([]).forEach(() => {})
  const dict = {}
  ;(all.dicts || []).filter(d => d.dictType === 'device_category').forEach(d => dict[d.dictCode] = d.dictLabel)
  const dev = await api.devicePage({ pageNum: 1, pageSize: 100 })
  dev.records.forEach(d => {
    const k = d.name
    map[k] = map[k] || { category: k, total: 0, available: 0, reserved: 0, lent: 0 }
    map[k].total++
    if (d.status === 'AVAILABLE') map[k].available++
    if (d.status === 'RESERVED') map[k].reserved++
    if (d.status === 'LENT') map[k].lent++
  })
  stock.value = Object.values(map)
}
function search() { q.pageNum = 1; load() }
function reset() { Object.assign(q, { pageNum: 1, keyword: '', status: 'ALL' }); load() }
function open(row) { cur.value = row; visible.value = true }
async function release(row) {
  const { value } = await ElMessageBox.prompt('请填写释放原因', '释放预留', { inputValue: '超过 48 小时未领用，自动释放' })
  await api.reservationRelease(row.id, value)
  ElMessage.success('已释放，器具回到可借状态')
  load()
}
onMounted(load)
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;margin-top:14px}</style>
