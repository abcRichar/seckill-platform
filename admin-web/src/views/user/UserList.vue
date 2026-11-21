<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>用户管理</h1>
        <p>管理商城用户状态、重置密码与分配角色</p>
      </div>
    </div>

    <section class="card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="用户名 / 昵称 / 手机号"
          clearable
          style="width: 240px"
          prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="search">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button @click="search">查询</el-button>
        <el-button text @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted total-tip">共 {{ total }} 位用户</span>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" style="width: 100%">
          <el-table-column label="用户" min-width="220">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar :size="38" :src="row.avatar">{{ (row.nickname || row.username).slice(0, 1) }}</el-avatar>
                <div>
                  <div class="cell-title">{{ row.nickname || row.username }}</div>
                  <div class="cell-sub">@{{ row.username }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" width="130">
            <template #default="{ row }">{{ row.phone || '-' }}</template>
          </el-table-column>
          <el-table-column prop="email" label="邮箱" min-width="180">
            <template #default="{ row }">{{ row.email || '-' }}</template>
          </el-table-column>
          <el-table-column label="身份" width="100">
            <template #default="{ row }">
              <el-tag :type="row.roleType === 'ADMIN' ? 'danger' : 'primary'" effect="light" round size="small">
                {{ row.roleType === 'ADMIN' ? '管理员' : '普通用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                :disabled="row.roleType === 'ADMIN'"
                inline-prompt
                active-text="正常"
                inactive-text="禁用"
                @change="(val) => toggleStatus(row, val)"
              />
            </template>
          </el-table-column>
          <el-table-column label="注册时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRoles(row)">角色</el-button>
              <el-button link type="warning" @click="resetPwd(row)">重置密码</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="fetchList"
          @size-change="search"
        />
      </div>
    </section>

    <el-dialog v-model="roleVisible" title="分配角色" width="480px">
      <div class="role-user">
        <el-avatar :size="40" :src="currentUser?.avatar">
          {{ (currentUser?.nickname || currentUser?.username || '?').slice(0, 1) }}
        </el-avatar>
        <div>
          <strong>{{ currentUser?.nickname || currentUser?.username }}</strong>
          <p class="muted">角色变更后需要用户重新登录才会生效</p>
        </div>
      </div>
      <el-checkbox-group v-model="selectedRoles" class="role-list">
        <label v-for="role in roles" :key="role.id" class="role-item" :class="{ checked: selectedRoles.includes(role.id) }">
          <el-checkbox :value="role.id">
            <div class="role-info">
              <strong>{{ role.roleName }}</strong>
              <span>{{ role.roleCode }} · {{ role.remark }}</span>
            </div>
          </el-checkbox>
        </label>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  assignUserRoles,
  getRoles,
  getUserRoles,
  getUsers,
  resetPassword,
  updateUserStatus
} from '@/api'
import { formatTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const roles = ref([])
const roleVisible = ref(false)
const selectedRoles = ref([])
const currentUser = ref(null)

const query = reactive({ page: 1, size: 10, keyword: '', status: null })

async function fetchList() {
  loading.value = true
  try {
    const data = await getUsers({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status === null || query.status === '' ? undefined : query.status
    })
    list.value = data?.records || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  fetchList()
}

function reset() {
  Object.assign(query, { page: 1, keyword: '', status: null })
  fetchList()
}

async function toggleStatus(row, value) {
  const status = value ? 1 : 0
  try {
    await updateUserStatus(row.id, status)
    row.status = status
    ElMessage.success(value ? '用户已启用' : '用户已禁用')
  } catch (e) {
    fetchList()
  }
}

async function resetPwd(row) {
  try {
    const { value } = await ElMessageBox.prompt('输入新的登录密码', '重置密码', {
      confirmButtonText: '确认重置',
      cancelButtonText: '取消',
      inputValue: '123456',
      inputPattern: /^.{6,20}$/,
      inputErrorMessage: '密码长度为 6-20 位'
    })
    await resetPassword(row.id, value)
    ElMessage.success('密码已重置')
  } catch (e) {
    // 用户取消
  }
}

async function openRoles(row) {
  currentUser.value = row
  roleVisible.value = true
  const [allRoles, roleIds] = await Promise.all([getRoles(), getUserRoles(row.id)])
  roles.value = allRoles || []
  selectedRoles.value = roleIds || []
}

async function saveRoles() {
  saving.value = true
  try {
    await assignUserRoles(currentUser.value.id, selectedRoles.value)
    ElMessage.success('角色已更新，用户重新登录后生效')
    roleVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.total-tip {
  font-size: 12px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 11px;
}

.role-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 12px;
  margin-bottom: 16px;
}

.role-user p {
  margin: 4px 0 0;
  font-size: 11px;
}

.role-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.role-item {
  display: block;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 11px 14px;
  transition: all 0.2s;
  cursor: pointer;
}

.role-item.checked {
  border-color: #bfdbfe;
  background: #f5f9ff;
}

.role-item :deep(.el-checkbox) {
  height: auto;
  width: 100%;
  align-items: flex-start;
}

.role-info strong {
  display: block;
  font-size: 13px;
}

.role-info span {
  display: block;
  font-size: 11px;
  color: var(--ink-3);
  margin-top: 3px;
}
</style>
