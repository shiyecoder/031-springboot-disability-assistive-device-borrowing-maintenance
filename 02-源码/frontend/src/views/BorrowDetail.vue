<template>
  <div v-if="d.app">
    <div class="pagehead">
      <div>
        <h1>{{ d.app.appNo }} · {{ d.app.applicantName }}</h1>
        <p>当前状态 <el-tag size="small" :type="statusMeta(d.app.status).type">{{ statusMeta(d.app.status).text }}</el-tag>
          · 责任人 {{ d.app.ownerName }} · 创建 {{ formatTime(d.app.createTime) }} · 最近更新 {{ formatTime(d.app.updateTime) }}</p>
      </div>
      <div class="head-actions">
        <el-button @click="$router.back()">返回列表</el-button>
        <el-button type="primary" @click="evaluate" :loading="scoring">计算适配提醒</el-button>
        <el-button v-if="d.app.status === 'PENDING_APPROVE'" type="primary" @click="act('approve')">审批通过</el-button>
        <el-button v-if="d.app.status === 'PENDING_HANDOVER'" type="primary" @click="goHandover">办理交接</el-button>
        <el-button v-if="d.app.status === 'IN_USE'" @click="goReturn">归还检查</el-button>
        <el-button v-if="canReturnBack" type="danger" plain @click="act('return_back')">退回</el-button>
      </div>
    </div>

    <div class="grid2">
      <div class="card">
        <div class="card-title">申请信息</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="使用人">{{ d.app.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ d.app.applicantPhone }}</el-descriptions-item>
          <el-descriptions-item label="代办人">{{ d.app.agentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="器具类别">{{ d.app.deviceCategory }}</el-descriptions-item>
          <el-descriptions-item label="身体条件" :span="2">{{ d.app.userCondition }}</el-descriptions-item>
          <el-descriptions-item label="借用期限" :span="2">{{ formatDate(d.app.expectStart) }} 至 {{ formatDate(d.app.expectEnd) }}（{{ d.app.borrowDays }} 天）</el-descriptions-item>
          <el-descriptions-item label="配送方式">{{ d.app.deliveryType === 'DELIVER' ? '上门配送' : '自提' }}</el-descriptions-item>
          <el-descriptions-item label="锁定器具">{{ d.app.deviceNo || '尚未预留' }}</el-descriptions-item>
          <el-descriptions-item label="用途说明" :span="2">{{ d.app.purpose || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="d.app.rejectReason" label="退回/驳回原因" :span="2">
            <span style="color: #B42318">{{ d.app.rejectReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div class="card-title" style="margin-top: 16px">需求证明</div>
        <el-image v-if="d.app.proofUrl" :src="d.app.proofUrl" fit="cover" style="width: 220px; height: 150px; border-radius: 6px"
                  :preview-src-list="[d.app.proofUrl]" preview-teleported />
        <span v-else class="hint">未上传</span>
      </div>

      <div class="card">
        <div class="card-title">流转时间轴</div>
        <el-timeline>
          <el-timeline-item v-for="(t, i) in d.timeline" :key="i" :timestamp="formatTime(t.time)" placement="top">
            <div class="timeline-title">{{ t.text }}</div>
            <div class="timeline-meta">{{ t.who }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </div>

    <div class="grid2 mt">
      <div class="card">
        <div class="card-title">适配评估</div>
        <el-descriptions v-if="d.assessment" :column="2" border size="small">
          <el-descriptions-item label="评估员">{{ d.assessment.assessorName }}</el-descriptions-item>
          <el-descriptions-item label="建议型号">{{ d.assessment.suggestModel }}</el-descriptions-item>
          <el-descriptions-item label="建议附件">{{ d.assessment.suggestParts }}</el-descriptions-item>
          <el-descriptions-item label="借用时长">{{ d.assessment.borrowDays }} 天</el-descriptions-item>
          <el-descriptions-item label="保证金">{{ d.assessment.needDeposit ? '¥' + money(d.assessment.depositAmount) : '不需要' }}</el-descriptions-item>
          <el-descriptions-item label="评估时间">{{ formatTime(d.assessment.assessTime) }}</el-descriptions-item>
          <el-descriptions-item label="使用指导" :span="2">{{ d.assessment.guidance }}</el-descriptions-item>
        </el-descriptions>
        <el-empty v-else description="尚未完成适配评估" :image-size="60" />
      </div>
      <div class="card">
        <div class="card-title">库存预留与交接</div>
        <el-descriptions v-if="d.reservation" :column="2" border size="small">
          <el-descriptions-item label="预留单号">{{ d.reservation.reserveNo }}</el-descriptions-item>
          <el-descriptions-item label="器具">{{ d.reservation.deviceNo }}</el-descriptions-item>
          <el-descriptions-item label="锁定时间">{{ formatTime(d.reservation.lockTime) }}</el-descriptions-item>
          <el-descriptions-item label="到期时间">{{ formatTime(d.reservation.expireTime) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="d.handover" class="mt8">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="交接单号">{{ d.handover.handoverNo }}</el-descriptions-item>
            <el-descriptions-item label="交接时间">{{ formatTime(d.handover.handoverTime) }}</el-descriptions-item>
            <el-descriptions-item label="状况">{{ d.handover.conditionCheck }}</el-descriptions-item>
            <el-descriptions-item label="押金">{{ d.handover.depositStatus }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <el-empty v-if="!d.reservation" description="审批通过后自动预留器具" :image-size="60" />
      </div>
    </div>

    <div class="card mt" v-if="lastScore">
      <div class="card-title">适配度与维护周期联合提醒（规则 {{ lastScore.ruleVersion }}）</div>
      <div class="grid3">
        <div>
          <div class="big-score">{{ lastScore.manualScore ?? lastScore.totalScore }}</div>
          <el-tag :type="levelMeta(lastScore.manualLevel || lastScore.level).type">
            {{ levelMeta(lastScore.manualLevel || lastScore.level).text }}
          </el-tag>
          <div class="hint mt8">{{ lastScore.maintainLevel }}</div>
          <div class="hint mt8">计算编号 {{ lastScore.scoreNo }} · {{ formatTime(lastScore.calcTime) }}</div>
        </div>
        <div>
          <div class="card-title" style="font-size: 13px">各因素得分</div>
          <div v-for="(v, k) in detailMap" :key="k" class="factor">
            <span>{{ factorLabel(k) }}</span>
            <div class="bar"><i :style="{ width: v + '%', background: v >= 80 ? '#287271' : v >= 60 ? '#A9761F' : '#B42318' }"></i></div>
            <b>{{ v }}</b>
          </div>
        </div>
        <div>
          <div class="card-title" style="font-size: 13px">推荐结论与改进项</div>
          <div style="font-size: 13px; line-height: 1.9">{{ lastScore.suggestText }}</div>
          <ul class="advice"><li v-for="(a, i) in adviceList" :key="i">{{ a }}</li></ul>
          <el-button size="small" style="margin-top: 8px" @click="openAdjust">人工调整结果</el-button>
          <div v-if="lastScore.manualReason" class="hint mt8">人工修正：{{ lastScore.manualScore }} 分 · {{ lastScore.manualReason }}（{{ lastScore.manually }} {{ formatTime(lastScore.manualTime) }}）</div>
        </div>
      </div>
    </div>

    <div class="card mt" v-if="d.scores?.length">
      <div class="card-title">计算留痕（共 {{ d.scores.length }} 次）</div>
      <el-table :data="d.scores" size="small" border>
        <el-table-column prop="scoreNo" label="计算编号" min-width="150" />
        <el-table-column prop="ruleVersion" label="规则版本" width="90" />
        <el-table-column prop="totalScore" label="原始分" width="90" />
        <el-table-column prop="level" label="等级" width="70" />
        <el-table-column label="人工修正" width="110">
          <template #default="{ row }">{{ row.manualScore ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="manualReason" label="修正原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="计算时间" width="160"><template #default="{ row }">{{ formatTime(row.calcTime) }}</template></el-table-column>
      </el-table>
    </div>

    <div class="card mt" v-if="d.returnInspection">
      <div class="card-title">归还检查</div>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="归还单号">{{ d.returnInspection.returnNo }}</el-descriptions-item>
        <el-descriptions-item label="功能">{{ d.returnInspection.functionCheck }}</el-descriptions-item>
        <el-descriptions-item label="外观">{{ d.returnInspection.appearanceCheck }}</el-descriptions-item>
        <el-descriptions-item label="配件">{{ d.returnInspection.partsCheck }}</el-descriptions-item>
        <el-descriptions-item label="损坏责任">{{ d.returnInspection.damageDuty }}</el-descriptions-item>
        <el-descriptions-item label="结论">{{ d.returnInspection.conclusion === 'TO_REPAIR' ? '转维修' : '转待消毒' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <el-dialog v-model="adjustVisible" title="人工调整适配结果" width="440px">
      <el-form label-width="90px">
        <el-form-item label="原始分数">{{ lastScore?.totalScore }}</el-form-item>
        <el-form-item label="调整后"><el-input-number v-model="adjust.score" :min="0" :max="100" :precision="2" /></el-form-item>
        <el-form-item label="调整原因"><el-input v-model="adjust.reason" type="textarea" :rows="3" placeholder="不少于 5 个字，原始结果不会被覆盖" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjust">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { formatDate, formatTime, levelMeta, money, statusMeta } from '../utils'

const route = useRoute()
const router = useRouter()
const d = ref({ timeline: [], scores: [] })
const scoring = ref(false)
const adjustVisible = ref(false)
const adjust = reactive({ score: 80, reason: '' })

const lastScore = computed(() => (d.value.scores?.length ? d.value.scores[0] : null))
const detailMap = computed(() => {
  try { return JSON.parse(lastScore.value?.detailJson || '{}') } catch { return {} }
})
const adviceList = computed(() => {
  try { return JSON.parse(lastScore.value?.adviceJson || '[]') } catch { return [] }
})
const canReturnBack = computed(() => ['PENDING_ASSESS', 'PENDING_APPROVE', 'PENDING_HANDOVER'].includes(d.value.app?.status))
const FACTOR = { demand: '需求类别', condition: '使用者条件', spec: '器具规格', days: '累计借用天数', fault: '历史故障', maintain: '最近维护日期' }
const factorLabel = (k) => FACTOR[k] || k

async function load() {
  d.value = await api.borrowDetail(route.params.id)
}
async function evaluate() {
  scoring.value = true
  try {
    await api.evaluate(route.params.id)
    ElMessage.success('计算完成，结果与留痕已保存')
    await load()
  } finally { scoring.value = false }
}
async function act(action) {
  const { value } = await ElMessageBox.prompt(
    action === 'approve' ? '确认审批通过？通过后将锁定一台检测合格的器具' : '请填写退回原因（不少于 5 个字）',
    action === 'approve' ? '审批通过' : '退回申请',
    { inputValue: action === 'approve' ? '库存与检测均满足要求' : '', inputValidator: (v) => (v && v.trim().length >= 5) || '原因不少于 5 个字' }
  )
  await api.borrowTransition(route.params.id, { action, reason: value })
  ElMessage.success('操作成功')
  load()
}
function goHandover() { router.push('/borrow/handover') }
function goReturn() { router.push('/borrow/return') }
function openAdjust() {
  adjust.score = Number(lastScore.value?.totalScore || 80)
  adjust.reason = ''
  adjustVisible.value = true
}
async function submitAdjust() {
  if (adjust.reason.trim().length < 5) { ElMessage.warning('原因不少于 5 个字'); return }
  await api.scoreAdjust(lastScore.value.id, { score: adjust.score, reason: adjust.reason })
  ElMessage.success('已记录人工修正，原始计算结果保持不变')
  adjustVisible.value = false
  load()
}
onMounted(load)
</script>

<style scoped>
.mt { margin-top: 16px; }
.big-score { font-size: 34px; font-weight: 600; color: #173B57; font-variant-numeric: tabular-nums; }
.factor { display: grid; grid-template-columns: 92px 1fr 32px; gap: 8px; align-items: center; font-size: 12px; margin-bottom: 8px; }
.bar { height: 6px; background: #EEF2F5; border-radius: 3px; overflow: hidden; }
.bar i { display: block; height: 6px; border-radius: 3px; }
.advice { margin: 8px 0 0; padding-left: 18px; font-size: 12px; color: #42535F; line-height: 1.9; }
</style>
