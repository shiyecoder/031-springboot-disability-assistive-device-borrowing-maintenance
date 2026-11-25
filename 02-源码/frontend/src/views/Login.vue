<template>
  <div class="login">
    <div class="left">
      <div class="brandline"><span class="dot">辅</span> 阳光社区康复驿站</div>
      <h1>让每一件辅具<br />都送到真正需要的人手里</h1>
      <p class="desc">器具台账 · 检测消毒 · 适配评估 · 借用交接<br />归还检查 · 故障维修 · 全流程可追溯</p>
      <div class="nums">
        <div><b>78</b><span>在册器具</span></div>
        <div><b>128</b><span>累计借用</span></div>
        <div><b>96.1%</b><span>归还完好率</span></div>
      </div>
      <div class="foot">本系统只管理器具与服务流程，不作医疗诊断</div>
    </div>
    <div class="right">
      <div class="box">
        <h2>登录管理平台</h2>
        <p class="hint">请使用机构分配的账号登录</p>
        <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent>
          <el-form-item prop="username">
            <el-input v-model="form.username" size="large" placeholder="账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码"
                      :prefix-icon="Lock" @keyup.enter="submit" />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">登 录</el-button>
        </el-form>
        <div class="tip">
          <div><b>演示账号</b>（密码均为 123456，管理员 admin / admin123）</div>
          <div>admin 机构管理员 · zhangpg 评估员 · wangcg 仓管员</div>
          <div>liwx 维修员 · ligf 借用人</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { api } from '../api'
import { loadUser } from '../store'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate().catch(() => {})
  if (!form.username || !form.password) return
  loading.value = true
  try {
    const data = await api.login(form)
    localStorage.setItem('token', data.token)
    await loadUser(true)
    ElMessage.success('登录成功，欢迎回来')
    router.push('/dashboard')
  } catch (e) {
    // 错误提示已由拦截器统一展示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login { display: grid; grid-template-columns: 1.08fr 0.92fr; height: 100vh; background: #fff; }
.left { position: relative; color: #fff; padding: 64px 56px; display: flex; flex-direction: column; justify-content: center;
  background: linear-gradient(150deg, rgba(23,59,87,.94), rgba(40,114,113,.88)), url('/login-bg.jpg') center/cover no-repeat; }
.brandline { position: absolute; top: 40px; left: 56px; display: flex; align-items: center; gap: 10px; font-size: 14px; letter-spacing: 1px; color: #D7E7EE; }
.dot { width: 28px; height: 28px; border-radius: 6px; background: #287271; display: grid; place-items: center; font-weight: 600; }
h1 { font-size: 34px; line-height: 1.4; font-weight: 600; margin: 0; letter-spacing: 1px; }
.desc { margin-top: 20px; color: #CFE0E9; line-height: 2; font-size: 14px; }
.nums { display: flex; gap: 44px; margin-top: 46px; }
.nums b { display: block; font-size: 26px; font-weight: 600; font-variant-numeric: tabular-nums; }
.nums span { font-size: 12px; color: #BCD5DF; }
.foot { position: absolute; bottom: 32px; left: 56px; font-size: 12px; color: #9FB8C4; }
.right { display: flex; align-items: center; justify-content: center; background: #F4F7F9; }
.box { width: 380px; }
h2 { font-size: 24px; margin: 0 0 6px; color: #173B57; }
.hint { color: #5F707C; font-size: 13px; margin: 0 0 26px; }
.submit { width: 100%; height: 46px; margin-top: 8px; font-size: 15px; letter-spacing: 4px; background: #173B57; border-color: #173B57; }
.tip { margin-top: 22px; background: #EDF3F7; border-radius: 8px; padding: 12px 14px; font-size: 12px; color: #5F707C; line-height: 1.9; }
.tip b { color: #173B57; }
:deep(.el-form-item) { margin-bottom: 18px; }
</style>
