<template>
  <div>
    <div class="pagehead"><div><h1>归还检查</h1><p>检查功能、外观、配件、清洁与损坏情况；正常器具进入待消毒，损坏自动生成维修工单</p></div></div>
    <div class="card">
      <div class="filters">
        <el-radio-group v-model="tab" size="small" @change="search">
          <el-radio-button label="IN_USE">借用中（待归还）</el-radio-button>
          <el-radio-button label="PENDING_RETURN">待归还检查</el-radio-button>
        </el-radio-group>
      </div>
    </div>
    <div class="card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="appNo" label="业务编号" min-width="140" />
        <el-table-column prop="applicantName" label="使用人" width="90" />
        <el-table-column prop="deviceNo" label="器具编号" min-width="130" />
        <el-table-column prop="deviceCategory" label="类别" width="90" />
        <el-table-column label="应还日期" width="120"><template #default="{ row }">{{ formatDate(row.expectEnd) }}</template></el-table-column>
        <el-table-column label="逾期" width="90"><template #default="{ row }">
          <el-tag :type="row.overdue ? 'danger' : 'success'" size="small">{{ row.overdue ? '已逾期' : '正常' }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }">
          <el-tag :type="statusMeta(row.status).type">{{ statusMeta(row.status).text }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="140"><template #default="{ row }">
          <el-button v-if="row.status === 'IN_USE'" link type="primary" @click="open(row)">归还检查</el-button>
          <el-button link @click="$router.push('/borrow/detail/' + row.id)">查看</el-button>
        </template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="q.pageNum" v-model:page-size="q.pageSize" @current-change="load" /></div>
    </div>

    <el-drawer v-model="visible" title="归还检查" size="620px">
      <div v-if="cur">
        <div class="notice">{{ cur.appNo }} · {{ cur.applicantName }} · {{ cur.deviceNo }} · 应还 {{ formatDate(cur.expectEnd) }}</div>
        <el-form label-width="104px" style="margin-top: 14px">
          <el-form-item label="功能检查"><el-select v-model="f.functionCheck" style="width: 200px">
            <el-option label="正常" value="正常" /><el-option label="轻微卡顿" value="轻微卡顿" /><el-option label="无法使用" value="无法使用" /></el-select></el-form-item>
          <el-form-item label="外观检查"><el-select v-model="f.appearanceCheck" style="width: 200px">
            <el-option label="完好" value="完好" /><el-option label="轻微磨损" value="轻微磨损" /><el-option label="明显损坏" value="明显损坏" /></el-select></el-form-item>
          <el-form-item label="配件检查"><el-select v-model="f.partsCheck" style="width: 200px">
            <el-option label="齐全" value="齐全" /><el-option label="缺件" value="缺件" /></el-select></el-form-item>
          <el-form-item label="清洁状态"><el-select v-model="f.cleanStatus" style="width: 200px">
            <el-option label="已清洁" value="已清洁" /><el-option label="需清洁" value="需清洁" /></el-select></el-form-item>
          <el-form-item label="损坏程度"><el-select v-model="f.damageLevel" style="width: 200px">
            <el-option label="无损坏" value="无损坏" /><el-option label="轻微损坏" value="轻微损坏" /><el-option label="严重损坏" value="严重损坏" /></el-select></el-form-item>
          <el-form-item label="损坏责任"><el-select v-model="f.damageDuty" style="width: 200px">
            <el-option label="无" value="无" /><el-option label="使用人责任" value="使用人责任" /><el-option label="自然损耗" value="自然损耗" /></el-select></el-form-item>
          <el-form-item label="归还照片">
            <el-upload :action="api.uploadUrl" :headers="headers" :show-file-list="false" :on-success="onUp" name="file" :data="{ type: 'return' }" accept="image/*">
              <el-image v-if="f.photoUrl" :src="f.photoUrl" fit="cover" style="width: 160px; height: 108px; border-radius: 6px" />
              <el-button v-else>上传照片</el-button>
            </el-upload>
          </el-form-item>
          <el-form-item label="备注"><el-input v-model="f.remark" type="textarea" :rows="2" /></el-form-item>
        </el-form>
        <el-alert :type="damaged ? 'warning' : 'success'" :closable="false"
                  :title="damaged ? '将生成维修工单，器具转待维修；损坏责任需管理员复核' : '器具将转待消毒，消毒合格后回到可借'" />
        <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 14px">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" @click="submit">提交检查结果</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { formatDate, statusMeta } from '../utils'
const loading = ref(false), rows = ref([]), total = ref(0), tab = ref('IN_USE')
const q = reactive({ pageNum: 1, pageSize: 10 })
const visible = ref(false), cur = ref(null)
const f = reactive({ functionCheck: '正常', appearanceCheck: '完好', partsCheck: '齐全', cleanStatus: '已清洁', damageLevel: '无损坏', damageDuty: '无', photoUrl: '', remark: '' })
const headers = computed(() => ({ Authorization: 'Bearer ' + localStorage.getItem('token') }))
const damaged = computed(() => f.damageLevel !== '无损坏')
async function load() {
  loading.value = true
  try { const p = await api.borrowPage({ ...q, status: tab.value }); rows.value = p.records; total.value = p.total }
  finally { loading.value = false }
}
function search() { q.pageNum = 1; load() }
function open(row) {
  cur.value = row
  Object.assign(f, { functionCheck: '正常', appearanceCheck: '轻微磨损', partsCheck: '齐全', cleanStatus: '需清洁', damageLevel: '无损坏', damageDuty: '无', photoUrl: '/uploads/seed/scene/04.jpg', remark: '' })
  visible.value = true
}
function onUp(res) { if (res.code === 200) f.photoUrl = res.data.url }
async function submit() {
  await api.borrowReturn(cur.value.id, { ...f })
  ElMessage.success(damaged.value ? '已提交，生成维修工单' : '已提交，器具转待消毒')
  visible.value = false
  load()
}
onMounted(load)
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;margin-top:14px}</style>
