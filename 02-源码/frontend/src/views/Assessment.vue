<template>
  <div>
    <div class="pagehead">
      <div><h1>适配评估表</h1><p>核验需求、适配型号、借用期限与使用注意事项；完成评估后进入审批</p></div>
    </div>
    <div class="card">
      <div class="filters">
        <el-radio-group v-model="tab" size="small" @change="search">
          <el-radio-button label="PENDING_ASSESS">待评估</el-radio-button>
          <el-radio-button label="ALL">全部记录</el-radio-button>
        </el-radio-group>
        <el-input v-model="q.keyword" placeholder="业务编号 / 使用人" clearable style="width: 200px" @keyup.enter="search" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>
    <div class="card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="appNo" label="业务编号" min-width="140" />
        <el-table-column prop="applicantName" label="使用人" width="90" />
        <el-table-column prop="userCondition" label="身体条件" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deviceCategory" label="申请类别" width="96" />
        <el-table-column label="借用期限" width="190">
          <template #default="{ row }">{{ formatDate(row.expectStart) }} ~ {{ formatDate(row.expectEnd) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }">
          <el-tag :type="statusMeta(row.status).type">{{ statusMeta(row.status).text }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right"><template #default="{ row }">
          <el-button link type="primary" @click="$router.push('/borrow/detail/' + row.id)">查看</el-button>
          <el-button v-if="row.status === 'PENDING_ASSESS'" link type="primary" @click="open(row)">执行评估</el-button>
        </template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="q.pageNum" v-model:page-size="q.pageSize" @current-change="load" /></div>
    </div>

    <el-drawer v-model="visible" title="适配评估" size="600px">
      <div v-if="cur">
        <div class="notice" style="margin-bottom: 14px">
          {{ cur.appNo }} · {{ cur.applicantName }} · {{ cur.deviceCategory }} · {{ cur.userCondition }}
        </div>
        <el-form :model="f" label-width="104px">
          <el-form-item label="建议型号" required>
            <el-select v-model="f.suggestModel" style="width: 300px" filterable allow-create placeholder="选择同类别可用型号">
              <el-option v-for="d in devices" :key="d.id" :label="d.spec" :value="d.name + ' ' + d.spec" />
            </el-select>
          </el-form-item>
          <el-form-item label="建议附件"><el-input v-model="f.suggestParts" placeholder="如 防滑坐垫、安全带" /></el-form-item>
          <el-form-item label="借用时长（天）"><el-input-number v-model="f.borrowDays" :min="7" :max="365" /></el-form-item>
          <el-form-item label="需保证金">
            <el-switch v-model="needDeposit" /><span v-if="needDeposit" style="margin-left: 12px">¥<el-input-number v-model="f.depositAmount" :min="0" :step="50" size="small" /></span>
          </el-form-item>
          <el-form-item label="使用指导"><el-input v-model="f.guidance" type="textarea" :rows="3" /></el-form-item>
          <el-form-item label="评估结论"><el-radio-group v-model="f.conclusion">
            <el-radio label="适配通过，建议出借">适配通过</el-radio><el-radio label="需补充证明材料">需补充材料</el-radio>
          </el-radio-group></el-form-item>
          <el-form-item label="备注"><el-input v-model="f.remark" type="textarea" :rows="2" /></el-form-item>
        </el-form>
        <div style="display: flex; gap: 10px; justify-content: flex-end">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" @click="submit">提交评估</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { formatDate, statusMeta } from '../utils'
const loading = ref(false), rows = ref([]), total = ref(0), tab = ref('PENDING_ASSESS')
const q = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const visible = ref(false), cur = ref(null), devices = ref([]), needDeposit = ref(false)
const f = reactive({ suggestModel: '', suggestParts: '', borrowDays: 60, depositAmount: 0, guidance: '', conclusion: '适配通过，建议出借', remark: '' })
async function load() {
  loading.value = true
  try {
    if (tab.value === 'ALL') { const p = await api.borrowPage(q); rows.value = p.records; total.value = p.total }
    else { const p = await api.borrowPage({ ...q, status: 'PENDING_ASSESS' }); rows.value = p.records; total.value = p.total }
  } finally { loading.value = false }
}
function search() { q.pageNum = 1; load() }
async function open(row) {
  cur.value = row
  const opt = await api.deviceOptions()
  devices.value = (opt.available || []).filter(d => d.name === row.deviceCategory)
  Object.assign(f, { suggestModel: row.deviceCategory + ' ' + (devices.value[0]?.spec || ''), suggestParts: '说明书、防滑垫',
    borrowDays: row.borrowDays || 60, depositAmount: 0, guidance: '每日检查刹车与轮胎；坡道需陪护；异常立即停用并报修', conclusion: '适配通过，建议出借', remark: '' })
  needDeposit.value = false
  visible.value = true
}
async function submit() {
  f.needDeposit = needDeposit.value ? 1 : 0
  await api.borrowAssessment(cur.value.id, { ...f })
  ElMessage.success('评估已提交，单据进入审批环节')
  visible.value = false
  load()
}
onMounted(load)
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;margin-top:14px}</style>
