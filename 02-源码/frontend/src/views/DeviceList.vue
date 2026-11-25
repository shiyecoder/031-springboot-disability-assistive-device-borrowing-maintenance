<template>
  <div>
    <div class="pagehead">
      <div>
        <h1>器具资源目录</h1>
        <p>在册 {{ total }} 台 · 可借 {{ kpi.available }} 台 · 检测过期与待消毒器具不可预留</p>
      </div>
      <div class="head-actions">
        <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon>器具入库</el-button>
        <el-button @click="exportCsv">导出当前结果</el-button>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <el-input v-model="q.keyword" placeholder="编号 / 名称 / 型号" clearable @keyup.enter="search" />
        <el-select v-model="q.category" placeholder="器具类别" clearable>
          <el-option label="全部类别" value="ALL" />
          <el-option v-for="c in options.categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="q.status" placeholder="借用状态" clearable>
          <el-option label="全部状态" value="ALL" />
          <el-option v-for="(v, k) in DEVICE_STATUS" :key="k" :label="v.text" :value="k" />
        </el-select>
        <el-input v-model="q.location" placeholder="存放点，如 A 区" clearable style="width: 160px" />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" border stripe style="width: 100%">
        <el-table-column label="图片" width="86">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" fit="cover" style="width: 62px; height: 46px; border-radius: 4px"
                      :preview-src-list="[row.imageUrl]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="deviceNo" label="器具编号" min-width="130" />
        <el-table-column prop="name" label="器具名称" min-width="96" />
        <el-table-column prop="spec" label="品牌型号" min-width="130" />
        <el-table-column prop="loadCapacity" label="载重" width="86" />
        <el-table-column prop="location" label="存放点" min-width="110" />
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="deviceMeta(row.status).type" effect="light">{{ deviceMeta(row.status).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastMaintainDate" label="最近维护" width="116">
          <template #default="{ row }">{{ formatDate(row.lastMaintainDate) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="176" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button link type="primary" @click="openInspect(row)">登记检测</el-button>
            <el-button link @click="openForm(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
                       v-model:current-page="q.pageNum" v-model:page-size="q.pageSize"
                       :page-sizes="[10, 20, 50]" @current-change="load" @size-change="search" />
      </div>
    </div>

    <!-- 详情抽屉：基础信息 + 检测 + 维修 + 预留 -->
    <el-drawer v-model="detailVisible" title="器具详情" size="720px">
      <div v-if="detail.device">
        <div style="display: flex; gap: 16px">
          <el-image :src="detail.device.imageUrl" fit="cover" style="width: 220px; height: 160px; border-radius: 8px" />
          <div style="flex: 1">
            <div style="font-size: 17px; font-weight: 600">{{ detail.device.name }} · {{ detail.device.deviceNo }}</div>
            <div class="hint" style="margin-top: 6px">{{ detail.device.capacityDesc }}</div>
            <div style="margin-top: 10px">
              <el-tag :type="deviceMeta(detail.device.status).type">{{ deviceMeta(detail.device.status).text }}</el-tag>
              <span class="hint" style="margin-left: 10px">当前状况：{{ detail.device.currentCondition || '-' }}</span>
            </div>
          </div>
        </div>
        <el-descriptions :column="2" border style="margin-top: 16px">
          <el-descriptions-item label="器具类别">{{ detail.device.name }}</el-descriptions-item>
          <el-descriptions-item label="适用人群">{{ detail.device.suitPeople }}</el-descriptions-item>
          <el-descriptions-item label="品牌型号">{{ detail.device.spec }}</el-descriptions-item>
          <el-descriptions-item label="载重">{{ detail.device.loadCapacity }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ detail.device.source }}</el-descriptions-item>
          <el-descriptions-item label="入库日期">{{ formatDate(detail.device.inboundDate) }}</el-descriptions-item>
          <el-descriptions-item label="存放点">{{ detail.device.location }}</el-descriptions-item>
          <el-descriptions-item label="累计维护">{{ detail.device.maintainCount }} 次</el-descriptions-item>
        </el-descriptions>

        <div class="card-title" style="margin-top: 18px">检测消毒记录</div>
        <el-table :data="detail.inspections" size="small" border>
          <el-table-column prop="inspectionNo" label="检测单号" min-width="130" />
          <el-table-column prop="checkItems" label="检查项" min-width="150" />
          <el-table-column prop="result" label="结论" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.result === 'PASS' ? 'success' : 'danger'">
                {{ row.result === 'PASS' ? '合格' : '不合格' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="inspectTime" label="检测时间" width="150">
            <template #default="{ row }">{{ formatTime(row.inspectTime) }}</template>
          </el-table-column>
        </el-table>

        <div class="card-title" style="margin-top: 18px">维修工单</div>
        <el-table :data="detail.repairs" size="small" border empty-text="暂无维修记录">
          <el-table-column prop="orderNo" label="工单号" min-width="130" />
          <el-table-column prop="faultDesc" label="故障描述" min-width="180" />
          <el-table-column prop="cost" label="费用" width="90">
            <template #default="{ row }">¥{{ money(row.cost) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">{{ repairMeta(row.status).text }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <!-- 入库 / 编辑 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑器具' : '器具入库'" width="620px" :close-on-click-modal="false">
      <el-form :model="form" label-width="96px">
        <el-form-item label="器具编号" required>
          <el-input v-model="form.deviceNo" :disabled="!!form.id" maxlength="50" placeholder="如 AS-2026-0101" />
        </el-form-item>
        <el-form-item label="器具名称" required>
          <el-select v-model="form.name" style="width: 200px" filterable allow-create placeholder="器具类别">
            <el-option v-for="c in options.categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用人群" required>
          <el-input v-model="form.suitPeople" placeholder="如 成人,老年人" />
        </el-form-item>
        <el-form-item label="品牌型号" required><el-input v-model="form.spec" /></el-form-item>
        <el-form-item label="载重"><el-input v-model="form.loadCapacity" placeholder="如 100kg" /></el-form-item>
        <el-form-item label="适用说明"><el-input v-model="form.capacityDesc" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="来源"><el-input v-model="form.source" /></el-form-item>
        <el-form-item label="入库日期">
          <el-date-picker v-model="form.inboundDate" type="date" value-format="YYYY-MM-DD" style="width: 200px" />
        </el-form-item>
        <el-form-item label="存放点"><el-input v-model="form.location" maxlength="100" /></el-form-item>
        <el-form-item label="器具图片">
          <el-upload :action="api.uploadUrl" :headers="uploadHeaders" :show-file-list="false"
                     :on-success="onImgUpload" name="file" :data="{ type: 'device' }" accept="image/*">
            <el-image v-if="form.imageUrl" :src="form.imageUrl" fit="cover"
                      style="width: 132px; height: 96px; border-radius: 6px; border: 1px solid #DCE3E8" />
            <el-button v-else>上传图片</el-button>
          </el-upload>
          <div class="hint" style="margin-left: 12px">仅支持本地上传，保存到项目 uploads/ 目录</div>
        </el-form-item>
        <el-form-item label="当前状况"><el-input v-model="form.currentCondition" maxlength="100" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 登记检测 -->
    <el-dialog v-model="inspectVisible" title="登记检测消毒" width="520px">
      <el-form :model="inspect" label-width="88px">
        <el-form-item label="器具"><b>{{ current?.deviceNo }} · {{ current?.name }}</b></el-form-item>
        <el-form-item label="检查项"><el-input v-model="inspect.checkItems" placeholder="如 刹车/轮组/坐垫" /></el-form-item>
        <el-form-item label="消毒方式"><el-input v-model="inspect.disinfectMethod" placeholder="如 含氯消毒液擦拭" /></el-form-item>
        <el-form-item label="结论">
          <el-radio-group v-model="inspect.result">
            <el-radio label="PASS">合格</el-radio>
            <el-radio label="FAIL">不合格（自动生成维修工单）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="inspect.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inspectVisible = false">取消</el-button>
        <el-button type="primary" @click="submitInspect">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { DEVICE_STATUS, deviceMeta, formatDate, formatTime, money, repairMeta } from '../utils'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const options = ref({ categories: [] })
const kpi = ref({ available: 0 })
const q = reactive({ pageNum: 1, pageSize: 10, keyword: '', category: 'ALL', status: 'ALL', location: '' })

const detailVisible = ref(false)
const detail = ref({ inspections: [], repairs: [] })
const formVisible = ref(false)
const form = ref({})
const inspectVisible = ref(false)
const inspect = reactive({ checkItems: '', disinfectMethod: '', result: 'PASS', remark: '' })
const current = ref(null)

const uploadHeaders = computed(() => ({ Authorization: 'Bearer ' + localStorage.getItem('token') }))

async function load() {
  loading.value = true
  try {
    const page = await api.devicePage(q)
    rows.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function search() {
  q.pageNum = 1
  load()
}

function reset() {
  Object.assign(q, { pageNum: 1, keyword: '', category: 'ALL', status: 'ALL', location: '' })
  load()
}

async function openDetail(row) {
  detail.value = await api.deviceDetail(row.id)
  detailVisible.value = true
}

function openForm(row) {
  form.value = row ? { ...row } : { name: '轮椅', suitPeople: '成人,老年人', imageUrl: '/uploads/seed/device/01.jpg' }
  formVisible.value = true
}

function openInspect(row) {
  current.value = row
  Object.assign(inspect, { checkItems: '刹车/轮组/坐垫/车架', disinfectMethod: '含氯消毒液擦拭', result: 'PASS', remark: '' })
  inspectVisible.value = true
}

async function submitInspect() {
  await api.deviceInspect(current.value.id, inspect)
  ElMessage.success('检测记录已保存')
  inspectVisible.value = false
  load()
}

async function save() {
  if (!form.value.deviceNo || !form.value.name || !form.value.spec || !form.value.suitPeople) {
    ElMessage.warning('编号、名称、规格型号、适用人群为必填项')
    return
  }
  await api.deviceSave(form.value)
  ElMessage.success('保存成功')
  formVisible.value = false
  load()
}

function onImgUpload(res) {
  if (res.code === 200) {
    form.value.imageUrl = res.data.url
    ElMessage.success('图片已上传')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

function exportCsv() {
  const head = ['器具编号', '名称', '类别', '品牌型号', '载重', '存放点', '状态', '入库日期']
  const body = rows.value.map((r) => [r.deviceNo, r.name, r.name, r.spec, r.loadCapacity, r.location, deviceMeta(r.status).text, formatDate(r.inboundDate)])
  const csv = '\ufeff' + [head, ...body].map((l) => l.join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `器具台账_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  ElMessage.success('已导出当前筛选结果')
}

onMounted(async () => {
  await load()
  const opt = await api.deviceOptions()
  options.value = opt
  kpi.value.available = opt.available.length
})
</script>

<style scoped>
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
