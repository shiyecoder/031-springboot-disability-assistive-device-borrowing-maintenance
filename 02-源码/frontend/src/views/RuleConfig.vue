<template>
  <div>
    <div class="pagehead">
      <div><h1>规则配置 · 适配度与维护周期联合提醒</h1><p>权重合计必须等于 100%；发布后仅影响新计算，历史结果保留原版本号</p></div>
    </div>
    <div class="grid2">
      <div class="card">
        <div class="card-title">权重配置（当前 {{ current.version }}）</div>
        <el-table :data="factors" size="small" border>
          <el-table-column prop="label" label="输入因素" width="130" />
          <el-table-column prop="source" label="数据来源" min-width="130" />
          <el-table-column label="权重" width="130">
            <template #default="{ row }">
              <el-input-number v-model="row.weight" :min="0" :max="100" :step="5" size="small" controls-position="right" style="width: 100px" />
            </template>
          </el-table-column>
          <el-table-column prop="missing" label="缺失处理" min-width="160" />
        </el-table>
        <div style="display: flex; align-items: center; gap: 14px; margin-top: 14px">
          <b :style="{ color: totalWeight === 100 ? '#287D4F' : '#B42318' }">合计 {{ totalWeight }}%</b>
          <el-button type="primary" :disabled="totalWeight !== 100" @click="publish">保存并发布新版本</el-button>
          <el-button @click="loadCurrent">重置</el-button>
        </div>
        <el-alert v-if="totalWeight !== 100" type="error" :closable="false" style="margin-top: 10px"
                  title="权重合计不等于 100%，无法发布" />
      </div>
      <div class="card">
        <div class="card-title">等级阈值</div>
        <el-form label-width="120px">
          <el-form-item label="A 级（推荐）≥"><el-input-number v-model="th.A" :min="50" :max="100" /></el-form-item>
          <el-form-item label="B 级（基本适配）≥"><el-input-number v-model="th.B" :min="40" :max="99" /></el-form-item>
          <el-form-item label="C 级（有风险）≥"><el-input-number v-model="th.C" :min="30" :max="90" /></el-form-item>
        </el-form>
        <div class="hint">低于 C 级阈值即为 D 级（不推荐直接出借），并给出改型建议。</div>
        <div class="card-title" style="margin-top: 18px">版本历史</div>
        <el-table :data="versions" size="small" border>
          <el-table-column prop="version" label="版本" width="80" />
          <el-table-column label="生效时间" width="160"><template #default="{ row }">{{ formatTime(row.effectTime) }}</template></el-table-column>
          <el-table-column prop="creator" label="修改人" width="90" />
          <el-table-column prop="remark" label="说明" min-width="150" />
          <el-table-column label="状态" width="80"><template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '历史' }}</el-tag>
          </template></el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { formatTime } from '../utils'
const current = ref({}), versions = ref([])
const th = reactive({ A: 85, B: 70, C: 55 })
const factors = ref([
  { key: 'demand', label: '需求类别', source: '借用申请', weight: 25, missing: '不参与计算并提示补录' },
  { key: 'condition', label: '使用者条件', source: '使用人档案', weight: 20, missing: '不参与计算并提示补录' },
  { key: 'spec', label: '器具规格', source: '器具台账', weight: 15, missing: '取同类别均值' },
  { key: 'days', label: '累计借用天数', source: '历史单据', weight: 15, missing: '按 30 天计' },
  { key: 'fault', label: '历史故障', source: '维修工单', weight: 15, missing: '按无故障计' },
  { key: 'maintain', label: '最近维护日期', source: '检测记录', weight: 10, missing: '不参与计算并提示补录' }
])
const totalWeight = computed(() => factors.value.reduce((a, b) => a + Number(b.weight || 0), 0))
async function loadCurrent() {
  versions.value = await api.ruleList()
  const r = await api.ruleCurrent()
  current.value = r
  try {
    const w = JSON.parse(r.weights || '{}')
    factors.value.forEach(f => { if (w[f.key] !== undefined) f.weight = Number(w[f.key]) })
    const t = JSON.parse(r.thresholds || '{}')
    th.A = t.A ?? 85; th.B = t.B ?? 70; th.C = t.C ?? 55
  } catch (e) { /* 格式异常时保留默认 */ }
}
async function publish() {
  const weights = {}
  factors.value.forEach(f => (weights[f.key] = Number(f.weight)))
  await api.rulePublish({ weights: JSON.stringify(weights), thresholds: JSON.stringify(th), remark: '调整权重后发布' })
  ElMessage.success('新版本已发布，仅影响后续计算')
  loadCurrent()
}
onMounted(loadCurrent)
</script>
