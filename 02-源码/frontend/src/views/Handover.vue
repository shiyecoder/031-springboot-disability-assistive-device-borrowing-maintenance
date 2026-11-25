<template>
  <div>
    <div class="pagehead"><div><h1>交接管理</h1><p>记录器具状况、配件、押金线下状态与使用指导确认；交接后器具转为借出</p></div></div>
    <div class="card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="appNo" label="业务编号" min-width="140" />
        <el-table-column prop="applicantName" label="使用人" width="90" />
        <el-table-column prop="deviceNo" label="锁定器具" min-width="130" />
        <el-table-column prop="deviceCategory" label="类别" width="90" />
        <el-table-column label="交接期限" width="190">
          <template #default="{ row }">{{ formatDate(row.expectStart) }} ~ {{ formatDate(row.expectEnd) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }">
          <el-tag :type="statusMeta(row.status).type">{{ statusMeta(row.status).text }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="140"><template #default="{ row }">
          <el-button link type="primary" @click="open(row)">办理交接</el-button>
          <el-button link @click="$router.push('/borrow/detail/' + row.id)">查看</el-button>
        </template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="q.pageNum" v-model:page-size="q.pageSize" @current-change="load" /></div>
    </div>

    <el-drawer v-model="visible" title="办理交接" size="560px">
      <div v-if="cur">
        <div class="notice">{{ cur.appNo }} · {{ cur.applicantName }} · {{ cur.deviceNo || '未预留器具' }}</div>
        <el-form label-width="104px" style="margin-top: 14px">
          <el-form-item label="器具状况">
            <el-checkbox-group v-model="checks">
              <el-checkbox label="功能正常（刹车、轮组、升降）" />
              <el-checkbox label="外观无破损、无锈蚀" />
              <el-checkbox label="配件齐全（说明书、工具、防滑垫）" />
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="使用指导确认"><el-switch v-model="guide" /><span class="hint" style="margin-left: 10px">使用人已确认口头指导</span></el-form-item>
          <el-form-item label="押金状态">
            <el-radio-group v-model="deposit"><el-radio label="无需押金">无需押金</el-radio>
              <el-radio label="已线下收取">已线下收取</el-radio><el-radio label="未收取">未收取</el-radio></el-radio-group>
          </el-form-item>
          <el-form-item label="押金金额"><el-input-number v-model="amount" :min="0" :step="50" :disabled="deposit !== '已线下收取'" /></el-form-item>
          <el-form-item label="交接照片">
            <el-upload :action="api.uploadUrl" :headers="headers" :show-file-list="false" :on-success="onUp" name="file" :data="{ type: 'handover' }" accept="image/*">
              <el-image v-if="photo" :src="photo" fit="cover" style="width: 160px; height: 108px; border-radius: 6px" />
              <el-button v-else>上传照片</el-button>
            </el-upload>
          </el-form-item>
          <el-form-item label="备注"><el-input v-model="remark" type="textarea" :rows="2" /></el-form-item>
        </el-form>
        <div style="display: flex; gap: 10px; justify-content: flex-end">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" @click="submit">确认交接</el-button>
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
const loading = ref(false), rows = ref([]), total = ref(0), q = reactive({ pageNum: 1, pageSize: 10 })
const visible = ref(false), cur = ref(null), checks = ref([]), guide = ref(true), deposit = ref('无需押金')
const amount = ref(0), photo = ref(''), remark = ref('')
const headers = computed(() => ({ Authorization: 'Bearer ' + localStorage.getItem('token') }))
async function load() {
  loading.value = true
  try { const p = await api.borrowPage({ ...q, status: 'PENDING_HANDOVER' }); rows.value = p.records; total.value = p.total }
  finally { loading.value = false }
}
function open(row) {
  cur.value = row
  checks.value = ['功能正常（刹车、轮组、升降）', '外观无破损、无锈蚀', '配件齐全（说明书、工具、防滑垫）']
  guide.value = true; deposit.value = '无需押金'; amount.value = 0; photo.value = '/uploads/seed/scene/02.jpg'; remark.value = ''
  visible.value = true
}
function onUp(res) { if (res.code === 200) { photo.value = res.data.url; ElMessage.success('照片已上传') } }
async function submit() {
  if (checks.value.length < 2) { ElMessage.warning('请至少勾选两项交接检查'); return }
  await api.borrowHandover(cur.value.id, {
    conditionCheck: checks.value.join('；'), parts: '说明书、工具、防滑垫', guideConfirm: guide.value ? 1 : 0,
    depositStatus: deposit.value, depositAmount: amount.value, photoUrl: photo.value, remark: remark.value
  })
  ElMessage.success('交接完成，器具已转借出')
  visible.value = false
  load()
}
onMounted(load)
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;margin-top:14px}</style>
