<template>
  <div>
    <div class="pagehead">
      <div><h1>适配与维护周期联合提醒</h1><p>规则 {{ rule.version }} · 权重合计 100% · 每次计算保存输入快照与明细分，结果可追溯</p></div>
      <div class="head-actions"><el-button type="primary" @click="$router.push('/admin/rule')">规则配置</el-button></div>
    </div>

    <div class="kpis">
      <div class="kpi"><div class="lb">累计计算</div><div class="vl">{{ effect.total }}</div><div class="dt">含历史版本结果</div></div>
      <div class="kpi"><div class="lb">推荐采纳率</div><div class="vl ok">{{ effect.adoptRate }}%</div><div class="dt">未人工调整占比</div></div>
      <div class="kpi"><div class="lb">人工调整率</div><div class="vl">{{ effect.adjustRate }}%</div><div class="dt">已记录原因</div></div>
      <div class="kpi"><div class="lb">A 级结果</div><div class="vl">{{ effect.dist?.A ?? 0 }}</div><div class="dt">推荐出借</div></div>
      <div class="kpi"><div class="lb">C / D 级</div><div class="vl bad">{{ (effect.dist?.C ?? 0) + (effect.dist?.D ?? 0) }}</div><div class="dt">需改型或缩短借期</div></div>
      <div class="kpi"><div class="lb">当前规则</div><div class="vl">{{ rule.version }}</div><div class="dt">{{ formatTime(rule.effectTime) }}</div></div>
    </div>

    <div class="card" style="margin-top: 12px">
      <div class="filters">
        <el-input v-model="q.keyword" placeholder="业务编号 / 计算编号" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="q.status" placeholder="等级" clearable style="width: 150px">
          <el-option label="全部等级" value="ALL" /><el-option label="A" value="A" /><el-option label="B" value="B" />
          <el-option label="C" value="C" /><el-option label="D" value="D" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="scoreNo" label="计算编号" min-width="160" />
        <el-table-column prop="appNo" label="业务编号" min-width="140" />
        <el-table-column prop="ruleVersion" label="规则版本" width="90" />
        <el-table-column label="原始分" width="90"><template #default="{ row }"><b>{{ row.totalScore }}</b></template></el-table-column>
        <el-table-column label="等级" width="130"><template #default="{ row }">
          <el-tag :type="levelMeta(row.level).type">{{ levelMeta(row.level).text }}</el-tag></template></el-table-column>
        <el-table-column label="人工修正" width="120"><template #default="{ row }">
          <span v-if="row.manualScore" style="color: #B42318">{{ row.manualScore }} · {{ row.manualLevel }}</span>
          <span v-else class="hint">未修正</span></template></el-table-column>
        <el-table-column prop="maintainLevel" label="维护等级" width="140" />
        <el-table-column label="计算时间" width="160"><template #default="{ row }">{{ formatTime(row.calcTime) }}</template></el-table-column>
        <el-table-column label="操作" width="120"><template #default="{ row }">
          <el-button link type="primary" @click="open(row)">依据明细</el-button></template></el-table-column>
      </el-table>
      <div class="pager"><el-pagination background layout="total, prev, pager, next" :total="total"
        v-model:current-page="q.pageNum" v-model:page-size="q.pageSize" @current-change="load" /></div>
    </div>

    <el-drawer v-model="visible" title="计算依据与结果解释" size="700px">
      <div v-if="cur">
        <div class="grid2">
          <div>
            <div class="big-score">{{ cur.manualScore ?? cur.totalScore }}</div>
            <el-tag :type="levelMeta(cur.manualLevel || cur.level).type">{{ levelMeta(cur.manualLevel || cur.level).text }}</el-tag>
            <div class="hint mt8">{{ cur.maintainLevel }}</div>
            <div class="hint mt8">{{ cur.suggestText }}</div>
            <div class="hint mt8">规则版本 {{ cur.ruleVersion }} · {{ formatTime(cur.calcTime) }}</div>
          </div>
          <div>
            <div class="card-title" style="font-size: 13px">各因素得分</div>
            <div v-for="(v, k) in detail" :key="k" class="factor">
              <span>{{ FACTOR[k] || k }}</span>
              <div class="bar"><i :style="{ width: v + '%', background: v >= 80 ? '#287271' : v >= 60 ? '#A9761F' : '#B42318' }"></i></div>
              <b>{{ v }}</b>
            </div>
          </div>
        </div>
        <div class="card-title" style="margin-top: 16px">输入快照</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item v-for="(v, k) in snapshot" :key="k" :label="SNAP[k] || k">{{ Array.isArray(v) ? (v.length ? v.join('；') : '无缺失') : v ?? '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="card-title" style="margin-top: 16px">未满足条件与可改进项</div>
        <ul class="advice"><li v-for="(a, i) in advice" :key="i">{{ a }}</li></ul>
        <div v-if="cur.manualReason" class="notice" style="margin-top: 12px">
          人工修正：{{ cur.manualScore }} 分（{{ cur.manualLevel }}）· {{ cur.manualReason }} · {{ cur.manually }} {{ formatTime(cur.manualTime) }}
        </div>
      </div>
    </el-drawer>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../api'
import { formatTime, levelMeta } from '../utils'
const loading = ref(false), rows = ref([]), total = ref(0), visible = ref(false), cur = ref(null)
const effect = ref({}), rule = ref({})
const q = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: 'ALL' })
const FACTOR = { demand: '需求类别', condition: '使用者条件', spec: '器具规格', days: '累计借用天数', fault: '历史故障', maintain: '最近维护日期' }
const SNAP = { demandCategory: '需求类别', suggestModel: '评估建议型号', userCondition: '使用者条件', deviceSpec: '器具规格', deviceCategory: '器具类别', borrowDays: '累计借用天数', faultCount: '历史故障次数', lastMaintain: '最近维护日期', missing: '缺失项' }
const detail = computed(() => { try { return JSON.parse(cur.value?.detailJson || '{}') } catch { return {} } })
const snapshot = computed(() => { try { return JSON.parse(cur.value?.inputSnapshot || '{}') } catch { return {} } })
const advice = computed(() => { try { return JSON.parse(cur.value?.adviceJson || '[]') } catch { return [] } })
async function load() {
  loading.value = true
  try { const p = await api.scorePage(q); rows.value = p.records; total.value = p.total } finally { loading.value = false }
}
function search() { q.pageNum = 1; load() }
function open(row) { cur.value = row; visible.value = true }
onMounted(async () => {
  await load()
  effect.value = await api.scoreEffect()
  rule.value = await api.ruleCurrent()
})
</script>
<style scoped>
.kpis { grid-template-columns: repeat(6, 1fr); }
.kpi .vl.ok { color: #287D4F; } .kpi .vl.bad { color: #B42318; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.big-score { font-size: 34px; font-weight: 600; color: #173B57; }
.factor { display: grid; grid-template-columns: 96px 1fr 32px; gap: 8px; align-items: center; font-size: 12px; margin-bottom: 8px; }
.bar { height: 6px; background: #EEF2F5; border-radius: 3px; overflow: hidden; }
.bar i { display: block; height: 6px; }
.advice { padding-left: 18px; font-size: 13px; line-height: 1.9; color: #42535F; }
.mt8 { margin-top: 8px; }
</style>
