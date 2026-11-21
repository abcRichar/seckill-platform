<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>分类管理</h1>
        <p>商品分类用于前台筛选与商品归类</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增分类
      </el-button>
    </div>

    <section class="card">
      <div class="toolbar">
        <span class="muted">共 {{ list.length }} 个分类</span>
        <span class="spacer"></span>
        <el-button text @click="fetchList"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </div>
      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="分类名称" min-width="200">
            <template #default="{ row }">
              <span class="cell-title">{{ row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="100" />
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="420px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="如：手机数码" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :step="1" style="width: 100%" />
          <div class="tip">数字越小越靠前</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCategory,
  deleteCategory,
  getAdminCategories,
  updateCategory
} from '@/api'
import { formatTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, name: '', sort: 0 })

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

async function fetchList() {
  loading.value = true
  try {
    list.value = (await getAdminCategories()) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, name: '', sort: 0 })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, { id: row.id, name: row.name, sort: row.sort })
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { name: form.name, sort: form.sort }
    if (form.id) {
      await updateCategory(form.id, payload)
    } else {
      await createCategory(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '删除分类', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await deleteCategory(row.id)
  ElMessage.success('已删除')
  fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.tip {
  font-size: 11px;
  color: var(--ink-3);
  margin-top: 4px;
}
</style>
