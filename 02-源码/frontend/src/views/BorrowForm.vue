<template>
  <div>
    <div class="pagehead">
      <div><h1>新建借用申请</h1><p>带 * 为必填项 · 提交后进入适配评估，工作人员完成评估与审批后方可领取</p></div>
      <div class="head-actions"><el-button @click="$router.back()">返回</el-button></div>
    </div>

    <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
      <div class="grid2">
        <div class="card">
          <div class="card-title">使用人信息</div>
          <el-form-item label="使用人姓名" prop="applicantName"><el-input v-model="form.applicantName" maxlength="20" /></el-form-item>
          <el-form-item label="联系电话" prop="applicantPhone"><el-input v-model="form.applicantPhone" maxlength="20" placeholder="11 位手机号" /></el-form-item>
          <el-form-item label="代办人"><el-input v-model="form.agentName" placeholder="无则留空" /></el-form-item>
          <el-form-item label="身体条件" prop="userCondition">
            <el-select v-model="form.userCondition" style="width: 240px" placeholder="请选择或输入">
              <el-option v-for="c in conditions" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
        </div>
        <div class="card">
          <div class="card-title">需求信息</div>
          <el-form-item label="器具类别" prop="deviceCategory">
            <el-select v-model="form.deviceCategory" style="width: 200px" placeholder="请选择">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="借用期限" prop="expectStart">
            <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
                            start-placeholder="开始日期" end-placeholder="结束日期" style="width: 320px" />
          </el-form-item>
          <el-form-item label="用途说明" prop="purpose">
            <el-input v-model="form.purpose" type="textarea" :rows="2" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="配送方式">
            <el-radio-group v-model="form.deliveryType">
              <el-radio label="SELF">自提</el-radio><el-radio label="DELIVER">上门配送</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-alert v-if="days > 0" :title="`预计借用 ${days} 天，超过 90 天需评估员拆分审核`" :type="days > 90 ? 'warning' : 'info'" :closable="false" />
        </div>
      </div>

      <div class="card">
        <div class="card-title">需求证明与附件 <span class="hint" style="font-weight: 400">（必须通过上传组件提交，不接受外链）</span></div>
        <el-form-item label="需求证明" prop="proofUrl">
          <el-upload :action="api.uploadUrl" :headers="headers" :show-file-list="false" :on-success="onProof" name="file" :data="{ type: 'proof' }" accept="image/*,.pdf">
            <div v-if="form.proofUrl" class="up-box">
              <img v-if="!form.proofUrl.endsWith('.pdf')" :src="form.proofUrl" alt="需求证明" />
              <div v-else class="pdf">PDF 已上传</div>
              <span>点击重新上传</span>
            </div>
            <div v-else class="up-empty"><el-icon><Plus /></el-icon><span>上传诊断证明 / 残疾证</span></div>
          </el-upload>
        </el-form-item>
        <div class="hint">支持 JPG / PNG / PDF，单个不超过 10MB；文件保存于服务端 uploads/ 目录，断网也可显示。</div>
      </div>
    </el-form>

    <div style="margin-top: 16px; display: flex; gap: 10px">
      <el-button type="primary" :loading="saving" @click="submit">提交申请</el-button>
      <el-button @click="saveDraft">保存草稿</el-button>
      <el-button @click="$router.back()">取消</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { calcDays } from '../utils'

const router = useRouter()
const formRef = ref()
const saving = ref(false)
const categories = ref([])
const dateRange = ref([])
const conditions = ['下肢行动不便，可短距离行走', '长期卧床，需护理支持', '上肢力量不足，握持困难',
  '偏瘫后单侧无力', '术后康复期，需短期代步', '高龄体弱，平衡能力下降', '脊髓损伤，双下肢无负重能力']

const form = reactive({
  applicantName: '', applicantPhone: '', agentName: '', userCondition: '',
  deviceCategory: '轮椅', expectStart: '', expectEnd: '', purpose: '', deliveryType: 'SELF', proofUrl: ''
})

const rules = {
  applicantName: [{ required: true, message: '请填写使用人姓名', trigger: 'blur' }],
  applicantPhone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入 11 位手机号', trigger: 'blur' }],
  userCondition: [{ required: true, message: '请选择身体条件', trigger: 'change' }],
  deviceCategory: [{ required: true, message: '请选择器具类别', trigger: 'change' }],
  expectStart: [{ required: true, message: '请选择借用期限', trigger: 'change' }],
  purpose: [{ required: true, message: '请填写用途说明', trigger: 'blur' }],
  proofUrl: [{ required: true, message: '请上传需求证明', trigger: 'change' }]
}

const headers = computed(() => ({ Authorization: 'Bearer ' + localStorage.getItem('token') }))
const days = computed(() => (dateRange.value?.length === 2 ? calcDays(dateRange.value[0], dateRange.value[1]) : 0))

function onProof(res) {
  if (res.code === 200) { form.proofUrl = res.data.url; ElMessage.success('证明已上传') }
  else ElMessage.error(res.msg || '上传失败')
}

function collect() {
  if (dateRange.value?.length === 2) {
    form.expectStart = dateRange.value[0]
    form.expectEnd = dateRange.value[1]
  }
  return form
}

function saveDraft() {
  ElMessage.info('草稿已保存在本地，正式提交前请完成必填项')
  localStorage.setItem('borrow-draft', JSON.stringify(form))
}

async function submit() {
  collect()
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) { ElMessage.error('请检查标红字段后再提交'); return }
  if (days.value <= 0) { ElMessage.error('借用期限必须晚于开始日期'); return }
  saving.value = true
  try {
    const app = await api.borrowCreate({ ...form })
    ElMessage.success('申请已提交，编号 ' + app.appNo)
    router.push('/borrow/detail/' + app.id)
  } finally { saving.value = false }
}

onMounted(async () => {
  const opt = await api.deviceOptions()
  categories.value = opt.categories || []
  const draft = localStorage.getItem('borrow-draft')
  if (draft) Object.assign(form, JSON.parse(draft))
})
</script>

<style scoped>
.up-box { width: 190px; height: 120px; border: 1px dashed #DCE3E8; border-radius: 8px; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 4px; color: #5F707C; font-size: 12px; overflow: hidden; }
.up-box img { width: 100%; height: 84px; object-fit: cover; }
.up-box .pdf { height: 84px; display: grid; place-items: center; color: #2F6B8A; font-weight: 600; }
.up-empty { width: 190px; height: 120px; border: 1px dashed #DCE3E8; border-radius: 8px; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 6px; color: #7E93A2; font-size: 12px; }
:deep(.el-form-item) { margin-bottom: 16px; }
</style>
