<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>角色权限</h1>
        <p>RBAC：用户 → 角色 → 权限点，接口用 @RequirePerm 校验</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增角色
      </el-button>
    </div>

    <section class="card">
      <div class="toolbar">
        <span class="muted">共 {{ roles.length }} 个角色</span>
        <span class="spacer"></span>
        <el-button text @click="fetchRoles"><el-icon><Refresh /></el-icon> 刷新</el-button>
      </div>
      <div class="table-wrap">
        <el-table v-loading="loading" :data="roles" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="角色" min-width="180">
            <template #default="{ row }">
              <div class="cell-title">{{ row.roleName }}</div>
              <div class="cell-sub">{{ row.roleCode }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="说明" min-width="220" />
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPerm(row)">分配权限</el-button>
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button
                link
                type="danger"
                :disabled="row.roleCode === 'ADMIN'"
                @click="remove(row)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="440px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" maxlength="50" placeholder="如：运营专员" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input
            v-model="form.roleCode"
            maxlength="50"
            placeholder="如：OPERATOR"
            :disabled="form.roleCode === 'ADMIN'"
          />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.remark" maxlength="255" placeholder="角色职责说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="permVisible" :title="`分配权限 · ${currentRole?.roleName || ''}`" size="560px">
      <div class="perm-head">
        <span class="muted">勾选该角色可访问的功能权限</span>
        <el-checkbox v-model="checkAll" @change="toggleAll">全选</el-checkbox>
      </div>
      <el-checkbox-group v-model="selectedMenus" class="perm-list">
        <label v-for="menu in menus" :key="menu.id" class="perm-item" :class="{ checked: selectedMenus.includes(menu.id) }">
          <el-checkbox :value="menu.id">
            <div class="perm-info">
              <strong>{{ menu.permName }}</strong>
              <code>{{ menu.permCode }}</code>
            </div>
          </el-checkbox>
        </label>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePerm">保存权限</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  assignRoleMenus,
  createRole,
  deleteRole,
  getMenus,
  getRoleMenus,
  getRoles,
  updateRole
} from '@/api'
import { formatTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const roles = ref([])
const menus = ref([])
const dialogVisible = ref(false)
const permVisible = ref(false)
const formRef = ref()
const currentRole = ref(null)
const selectedMenus = ref([])

const form = reactive({ id: null, roleName: '', roleCode: '', remark: '' })
const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z_]{2,50}$/, message: '请输入大写字母或下划线', trigger: 'blur' }
  ]
}

const checkAll = computed({
  get: () => menus.value.length > 0 && selectedMenus.value.length === menus.value.length,
  set: () => {}
})

function toggleAll(value) {
  selectedMenus.value = value ? menus.value.map((m) => m.id) : []
}

async function fetchRoles() {
  loading.value = true
  try {
    roles.value = (await getRoles()) || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, roleName: '', roleCode: '', remark: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    roleName: row.roleName,
    roleCode: row.roleCode,
    remark: row.remark
  })
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { roleName: form.roleName, roleCode: form.roleCode, remark: form.remark }
    if (form.id) {
      await updateRole(form.id, payload)
    } else {
      await createRole(payload)
    }
    ElMessage.success('角色已保存')
    dialogVisible.value = false
    fetchRoles()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '删除角色', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await deleteRole(row.id)
  ElMessage.success('已删除')
  fetchRoles()
}

async function openPerm(row) {
  currentRole.value = row
  permVisible.value = true
  if (!menus.value.length) {
    menus.value = (await getMenus()) || []
  }
  selectedMenus.value = (await getRoleMenus(row.id)) || []
}

async function savePerm() {
  saving.value = true
  try {
    await assignRoleMenus(currentRole.value.id, selectedMenus.value)
    ElMessage.success('权限已更新，相关用户权限缓存已失效')
    permVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(fetchRoles)
</script>

<style scoped>
.perm-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  font-size: 13px;
}

.perm-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.perm-item {
  display: block;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.perm-item.checked {
  border-color: #bfdbfe;
  background: #f5f9ff;
}

.perm-item :deep(.el-checkbox) {
  height: auto;
  width: 100%;
  align-items: flex-start;
}

.perm-info strong {
  display: block;
  font-size: 12.5px;
}

.perm-info code {
  display: block;
  margin-top: 4px;
  font-size: 10px;
  color: var(--ink-3);
  font-family: Consolas, monospace;
}

</style>
