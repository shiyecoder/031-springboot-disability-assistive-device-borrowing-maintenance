<template>
  <div>
    <div class="pagehead"><div><h1>个人中心</h1><p>修改资料、密码与头像，头像保存后右上角立即同步</p></div></div>
    <div class="grid2">
      <div class="card">
        <div class="card-title">基本资料</div>
        <div style="display: flex; gap: 20px">
          <el-upload :action="api.uploadUrl" :headers="headers" :show-file-list="false" :on-success="onAvatar" name="file" :data="{ type: 'avatar' }" accept="image/*">
            <img :src="form.avatar" class="big-avatar" alt="头像" />
            <div class="hint" style="text-align: center; margin-top: 6px">点击更换头像</div>
          </el-upload>
          <div style="flex: 1">
            <el-form label-width="80px">
              <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
              <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
              <el-form-item label="角色">{{ roleText(store.user?.roleCode) }}</el-form-item>
              <el-form-item label="所属机构">{{ store.user?.orgName }}</el-form-item>
            </el-form>
            <el-button type="primary" @click="save">保存资料</el-button>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-title">修改密码</div>
        <el-form label-width="90px">
          <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password /></el-form-item>
          <el-form-item label="确认新密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
        </el-form>
        <el-button type="primary" @click="changePwd">更新密码</el-button>
        <div class="notice" style="margin-top: 14px">密码使用 BCrypt 保存；连续 5 次登录失败将锁定账号 10 分钟。</div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
import { loadUser, setAvatar, store } from '../store'
import { roleText } from '../utils'
const form = reactive({ realName: '', phone: '', avatar: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const headers = computed(() => ({ Authorization: 'Bearer ' + localStorage.getItem('token') }))
async function save() {
  await api.updateProfile(form)
  setAvatar(form.avatar)
  await loadUser(true)
  ElMessage.success('资料已保存')
}
async function changePwd() {
  if (!pwd.oldPassword || pwd.newPassword.length < 6) { ElMessage.warning('新密码至少 6 位'); return }
  if (pwd.newPassword !== pwd.confirm) { ElMessage.error('两次输入的新密码不一致'); return }
  await api.changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码已更新')
  Object.assign(pwd, { oldPassword: '', newPassword: '', confirm: '' })
}
function onAvatar(res) {
  if (res.code === 200) { form.avatar = res.data.url; setAvatar(form.avatar); ElMessage.success('头像已更新') }
}
onMounted(async () => {
  await loadUser(true)
  form.realName = store.user?.realName
  form.phone = store.user?.phone
  form.avatar = store.user?.avatar
})
</script>
<style scoped>
.big-avatar { width: 84px; height: 84px; border-radius: 50%; object-fit: cover; border: 1px solid #DCE3E8; cursor: pointer; }
</style>
