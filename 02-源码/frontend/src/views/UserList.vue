<template>
  <div>
    <div class="pagehead">
      <div><h1>用户与角色</h1><p>菜单权限、按钮权限与数据范围分别控制；页面无权限不代表接口可调用</p></div>
      <div class="head-actions"><el-button type="primary" @click="openForm()">新增用户</el-button></div>
    </div>
    <div class="card">
      <div class="filters">
        <el-input v-model="q.keyword" placeholder="账号 / 姓名" clearable style="width: 200px" @keyup.enter="search" />
        <el-select v-model="q.status" placeholder="角色" clearable style="width: 170px">
          <el-option label="全部角色" value="ALL" /><el-option v-for="r in roles" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
    </div>
    <div class="grid2">
      <div class="card">
        <el-table :data="rows" v-loading="loading" border stripe>
          <el-table-column prop="username" label="账号" width="110" />
          <el-table-column prop="realName" label="姓名" width="90" />
          <el-table-column label="角色" width="120"><template #default="{ row }">{{ roleText(row.roleCode) }}</template></el-table-column>
          <el-table-column prop="orgName" label="所属机构" min-width="150" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column label="状态" width="80"><template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template></el-table-column>
          <el-table-column label="操作" width="160"><template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button link @click="reset(row)">重置密码</el-button>
            <el-button link type="danger" @click="del(row)">删除</el-button>
          </template></el-table-column>
        </el-table>
        <div class="pager"><el-pagination background layout="total, prev, pager, next" :total="total"
          v-model:current-page="q.pageNum" v-model:page-size="q.pageSize" @current-change="load" /></div>
      </div>
      <div class="card">
        <div class="card-title">角色与数据范围</div>
        <el-table :data="roles" size="small" border>
          <el-table-column prop="roleName" label="角色" width="120" />
          <el-table-column prop="dataScope" label="数据范围" width="100" />
          <el-table-column prop="remark" label="职责" min-width="200" />
          <el-table-column label="菜单" width="100"><template #default="{ row }">
            <el-button link type="primary" @click="openMenu(row)">分配菜单</el-button>
          </template></el-table-column>
        </el-table>
        <div class="hint" style="margin-top: 8px">「菜单管理」本身在分配菜单树中被隐藏，避免误删后无法进入系统。</div>
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑用户' : '新增用户'" width="460px">
      <el-form label-width="86px">
        <el-form-item label="账号"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="form.roleCode" style="width: 200px">
          <el-option v-for="r in roles" :key="r.roleCode" :label="r.roleName" :value="r.roleCode" /></el-select></el-form-item>
        <el-form-item label="所属机构"><el-input v-model="form.orgName" /></el-form-item>
        <div class="hint">新增用户默认密码 123456，首次登录后请自行修改。</div>
      </el-form>
      <template #footer><el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="menuVisible" :title="'为「' + curRole?.roleName + '」分配菜单'" width="520px">
      <el-tree ref="treeRef" :data="tree" show-checkbox node-key="id" :props="{ label: 'name', children: 'children' }"
               :default-checked-keys="checked" style="max-height: 420px; overflow: auto" />
      <template #footer><el-button @click="menuVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMenus">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { roleText } from '../utils'
const loading = ref(false), rows = ref([]), total = ref(0), roles = ref([])
const q = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: 'ALL' })
const formVisible = ref(false), form = ref({})
const menuVisible = ref(false), tree = ref([]), checked = ref([]), curRole = ref(null), treeRef = ref()
async function load() {
  loading.value = true
  try { const p = await api.userPage(q); rows.value = p.records; total.value = p.total } finally { loading.value = false }
}
function search() { q.pageNum = 1; load() }
function openForm(row) { form.value = row ? { ...row } : { roleCode: 'WAREHOUSE', orgName: '阳光社区康复驿站' }; formVisible.value = true }
async function save() {
  await api.userSave(form.value)
  ElMessage.success('已保存')
  formVisible.value = false
  load()
}
async function reset(row) {
  await ElMessageBox.confirm(`确认将 ${row.username} 的密码重置为 123456？`, '重置密码', { type: 'warning' })
  await api.userReset(row.id)
  ElMessage.success('密码已重置')
}
async function del(row) {
  await ElMessageBox.confirm(`确认删除用户 ${row.realName}？`, '删除确认', { type: 'warning' })
  await api.userDelete(row.id)
  ElMessage.success('已删除')
  load()
}
async function openMenu(role) {
  curRole.value = role
  tree.value = await api.menuTree()
  checked.value = await api.roleMenus(role.id)
  menuVisible.value = true
}
async function saveMenus() {
  const ids = [...treeRef.value.getCheckedKeys(), ...treeRef.value.getHalfCheckedKeys()]
  await api.roleAssign(curRole.value.id, ids)
  ElMessage.success('菜单权限已更新')
  menuVisible.value = false
}
onMounted(async () => { await load(); roles.value = await api.roleList() })
</script>
<style scoped>.pager{display:flex;justify-content:flex-end;margin-top:14px}</style>
