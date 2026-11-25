<template>
  <div class="dash">
    <div class="pagehead">
      <div>
        <h1>辅具服务看板</h1>
        <p>统计区间 {{ rangeText }} · 数据每 10 分钟刷新 · 点击图表可下钻到明细</p>
      </div>
      <div class="head-actions">
        <el-radio-group v-model="range" size="small" @change="load">
          <el-radio-button label="30">近 30 天</el-radio-button>
          <el-radio-button label="month">本月</el-radio-button>
          <el-radio-button label="quarter">本季度</el-radio-button>
          <el-radio-button label="year">本年</el-radio-button>
        </el-radio-group>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="kpis">
      <div class="kpi" v-for="k in kpiCards" :key="k.label">
        <div class="lb">{{ k.label }}</div>
        <div class="vl" :class="k.tone">{{ k.value }}</div>
        <div class="dt">{{ k.desc }}</div>
      </div>
    </div>

    <div class="charts">
      <div class="card">
        <div class="card-title">借用趋势（近 6 个月）</div>
        <EChart :option="trendOption" :height="172" />
      </div>
      <div class="card">
        <div class="card-title">器具状态分布</div>
        <EChart :option="statusOption" :height="172" @click="toDevice('')" />
      </div>
      <div class="card">
        <div class="card-title">分类在册 / 可借</div>
        <EChart :option="catOption" :height="172" />
      </div>
      <div class="card">
        <div class="card-title">借用单阶段分布</div>
        <EChart :option="stageOption" :height="172" />
      </div>
      <div class="card">
        <div class="card-title">维修费用（元 / 月）</div>
        <EChart :option="costOption" :height="172" />
      </div>
      <div class="card">
        <div class="card-title">适配提醒等级分布</div>
        <EChart :option="levelOption" :height="172" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import EChart from '../components/EChart.vue'
import { api } from '../api'
import { CHART_COLORS, deviceMeta, statusMeta } from '../utils'

const router = useRouter()
const range = ref('30')
const data = ref({ kpi: {}, charts: {} })
const rangeText = computed(() => ({ '30': '最近 30 天', month: '本月', quarter: '本季度', year: '本年' }[range.value]))

const kpiCards = computed(() => {
  const k = data.value.kpi || {}
  return [
    { label: '器具可用率', value: (k.availableRate ?? 0) + '%', desc: `可借 ${k.available ?? 0} / 在册 ${k.total ?? 0}` , tone: 'ok' },
    { label: '在借器具', value: (k.lentCount ?? 0) + ' 台', desc: '借用中的器具数量' },
    { label: '平均借期', value: (k.avgDays ?? 0) + ' 天', desc: '按已提交申请期限统计' },
    { label: '逾期未还', value: (k.overdueCount ?? 0) + ' 单', desc: '已超期望归还日期', tone: (k.overdueCount ? 'bad' : '') },
    { label: '维护完成率', value: (k.maintainRate ?? 0) + '%', desc: '维修工单完成占比' },
    { label: '适配人工调整率', value: (k.adjustRate ?? 0) + '%', desc: '创新模块人工修正占比' }
  ]
})

const trendOption = computed(() => {
  const t = data.value.charts?.trend || { months: [], apply: [], done: [] }
  return {
    grid: { left: 34, right: 12, top: 26, bottom: 24 },
    tooltip: { trigger: 'axis' },
    legend: { right: 0, top: 0, itemWidth: 10, itemHeight: 8, textStyle: { fontSize: 11 } },
    xAxis: { type: 'category', data: t.months, axisLine: { lineStyle: { color: '#DCE3E8' } },
      axisLabel: { fontSize: 9, interval: 0, margin: 6, formatter: (v) => String(v).slice(2) } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EEF2F5' } }, axisLabel: { fontSize: 10 } },
    series: [
      { name: '申请量', type: 'line', smooth: true, data: t.apply, itemStyle: { color: CHART_COLORS[0] }, areaStyle: { opacity: 0.08 } },
      { name: '归档量', type: 'line', smooth: true, data: t.done, itemStyle: { color: CHART_COLORS[1] } }
    ]
  }
})

const statusOption = computed(() => {
  const d = data.value.charts?.statusDist || {}
  return {
    grid: { left: 40, right: 16, top: 10, bottom: 10 },
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['46%', '72%'], center: ['46%', '52%'],
      label: { fontSize: 10, formatter: '{b}\n{c} 台' },
      data: Object.entries(d).map(([k, v], i) => ({ name: deviceMeta(k).text, value: v, itemStyle: { color: CHART_COLORS[i % 7] } }))
    }]
  }
})

const catOption = computed(() => {
  const c = data.value.charts?.catDist || { total: {}, available: {} }
  const cats = Object.keys(c.total || {})
  return {
    grid: { left: 40, right: 12, top: 26, bottom: 22 },
    tooltip: { trigger: 'axis' },
    legend: { right: 0, top: 0, itemWidth: 10, itemHeight: 8, textStyle: { fontSize: 11 } },
    xAxis: { type: 'category', data: cats, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EEF2F5' } }, axisLabel: { fontSize: 10 } },
    series: [
      { name: '在册', type: 'bar', barWidth: 14, data: cats.map((k) => c.total[k] || 0), itemStyle: { color: '#173B57' } },
      { name: '可借', type: 'bar', barWidth: 14, data: cats.map((k) => c.available[k] || 0), itemStyle: { color: '#287271' } }
    ]
  }
})

const stageOption = computed(() => {
  const d = data.value.charts?.stageDist || {}
  const keys = Object.keys(d)
  return {
    grid: { left: 40, right: 12, top: 12, bottom: 28 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: keys.map((k) => statusMeta(k).text), axisLabel: { fontSize: 10, rotate: 24 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EEF2F5' } }, axisLabel: { fontSize: 10 } },
    series: [{ type: 'bar', barWidth: 18, data: keys.map((k) => d[k]), itemStyle: { color: '#2F6B8A', borderRadius: [3, 3, 0, 0] } }]
  }
})

const costOption = computed(() => {
  const d = data.value.charts?.cost || {}
  return {
    grid: { left: 44, right: 12, top: 14, bottom: 24 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: Object.keys(d).map((k) => k.slice(5) + '月'), axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EEF2F5' } }, axisLabel: { fontSize: 10 } },
    series: [{
      type: 'bar', barWidth: 20, data: Object.values(d),
      itemStyle: { color: '#287D4F', borderRadius: [3, 3, 0, 0] },
      markLine: { silent: true, symbol: 'none', label: { fontSize: 10 }, data: [] }
    }]
  }
})

const levelOption = computed(() => {
  const d = data.value.charts?.levelDist || {}
  return {
    grid: { left: 36, right: 12, top: 14, bottom: 24 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: Object.keys(d).map((k) => k + ' 级'), axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EEF2F5' } }, axisLabel: { fontSize: 10 } },
    series: [{
      type: 'bar', barWidth: 26, data: Object.entries(d).map(([k, v]) => ({
        value: v, itemStyle: { color: { A: '#287D4F', B: '#2F6B8A', C: '#A9761F', D: '#B42318' }[k] }
      }))
    }]
  }
})

function toDevice() {
  router.push('/device/list')
}

async function load() {
  data.value = await api.dashboard(range.value)
}

onMounted(load)
</script>

<style scoped>
.dash { height: calc(100vh - 104px); display: flex; flex-direction: column; overflow: hidden; }
.pagehead { margin-bottom: 12px; }
.kpis { grid-template-columns: repeat(6, 1fr); gap: 12px; flex: 0 0 auto; }
.kpi { padding: 12px 14px; }
.kpi .vl { font-size: 22px; margin-top: 4px; color: #173B57; }
.kpi .vl.ok { color: #287D4F; }
.kpi .vl.bad { color: #B42318; }
.charts { margin-top: 12px; display: grid; grid-template-columns: repeat(3, 1fr); grid-template-rows: repeat(2, 1fr);
  gap: 12px; flex: 1; min-height: 0; }
.charts .card { padding: 12px 14px; display: flex; flex-direction: column; overflow: hidden; }
.charts .card-title { margin-bottom: 4px; font-size: 13px; }
</style>
