<template>
  <div>
    <div class="pagehead">
      <div><h1>菜单管理</h1><p>菜单按角色动态下发；修改后对应角色刷新页面生效</p></div>
      <div class="head-actions"><el-button type="primary" @click="openForm()">新增菜单</el-button></div>
    </div>
    <div class="card">
      <el-table :data="menus" border row-key="id" default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column prop="name" label="菜单名称" min-width="200" />
        <el-table-column prop="path" label="路由" min-width="180" />
        <el-table-column label="操作" width="140"><template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
        </template></el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="visible" :title="form.id ? '编辑菜单' : '新增菜单'" width="460px">
      <el-form label-width="96px">
        <el-form-item label="菜单名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="路由"><el-input v-model="form.path" placeholder="/device/list" /></el-form-item>
        <el-form-item label="父级"><el-select v-model="form.parentId" style="width: 200px">
          <el-option label="顶级菜单" :value="0" />
          <el-option v-for="m in flat" :key="m.id" :label="m.name" :value="m.id" /></el-select></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="1" :max="99" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
const menus = ref([]), visible = ref(false), form = ref({})
const flat = computed(() => menus.value.filter(m => m.children && m.children.length))
async function load() { menus.value = await api.menuTree() }
function openForm(row) { form.value = row ? { ...row } : { parentId: 0, sort: 50 }; visible.value = true }
async function save() {
  await api.menuSave(form.value)
  ElMessage.success('已保存')
  visible.value = false
  load()
}
onMounted(load)
</script>
