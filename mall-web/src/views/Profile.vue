<template>
  <div class="page">
    <div class="container narrow">
      <section class="profile-head card">
        <div class="avatar-wrap">
          <el-avatar :size="86" :src="form.avatar">{{ user?.nickname?.slice(0, 1) }}</el-avatar>
          <span class="role">{{ user?.roleType === 'ADMIN' ? '管理员' : '普通会员' }}</span>
        </div>
        <div class="head-info">
          <h1>{{ user?.nickname || user?.username }}</h1>
          <p class="muted">@{{ user?.username }} · 注册于 {{ formatTime(user?.createTime) }}</p>
          <div class="head-tags">
            <span v-for="name in user?.roleNames || []" :key="name" class="tag-soft">{{ name }}</span>
          </div>
        </div>
        <div class="head-actions">
          <el-button round @click="$router.push('/orders')">
            <el-icon><Tickets /></el-icon> 我的订单
          </el-button>
          <el-button round @click="passwordVisible = true">修改密码</el-button>
        </div>
      </section>

      <section class="form-card card">
        <h2>个人资料</h2>
        <p class="muted">完善资料后，商家可以更好地联系你</p>

        <el-form :model="form" label-position="top" class="form">
          <div class="grid">
            <el-form-item label="用户名">
              <el-input :model-value="user?.username" disabled />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="form.nickname" placeholder="怎么称呼你" maxlength="20" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="example@mail.com" />
            </el-form-item>
          </div>

          <el-form-item label="选择头像">
            <div class="avatar-picker">
              <button
                v-for="item in avatars"
                :key="item"
                type="button"
                class="avatar-option"
                :class="{ active: form.avatar === item }"
                @click="form.avatar = item"
              >
                <img :src="item" alt="avatar" />
              </button>
            </div>
          </el-form-item>

          <el-button type="primary" round :loading="saving" @click="save">保存修改</el-button>
        </el-form>
      </section>

      <el-dialog v-model="passwordVisible" title="修改密码" width="420px">
        <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-position="top">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="pwdForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="pwdForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirm">
            <el-input v-model="pwdForm.confirm" type="password" show-password />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button round @click="passwordVisible = false">取消</el-button>
          <el-button type="primary" round :loading="changing" @click="changePwd">确认修改</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword, getMe, updateProfile } from '@/api'
import { useUserStore } from '@/stores/user'
import { formatTime } from '@/utils/format'

const userStore = useUserStore()
const user = ref(null)
const saving = ref(false)
const changing = ref(false)
const passwordVisible = ref(false)
const pwdRef = ref()

const avatars = [
  '/uploads/seed/avatar-alice.svg',
  '/uploads/seed/avatar-bob.svg',
  '/uploads/seed/avatar-carol.svg',
  '/uploads/seed/avatar-david.svg',
  '/uploads/seed/avatar-emma.svg',
  '/uploads/seed/avatar-admin.svg'
]

const form = reactive({ nickname: '', phone: '', email: '', avatar: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) =>
        value === pwdForm.newPassword ? callback() : callback(new Error('两次密码不一致')),
      trigger: 'blur'
    }
  ]
}

async function load() {
  const data = await getMe()
  user.value = data
  form.nickname = data.nickname || ''
  form.phone = data.phone || ''
  form.email = data.email || ''
  form.avatar = data.avatar || ''
}

async function save() {
  saving.value = true
  try {
    await updateProfile({ ...form })
    ElMessage.success('资料已更新')
    await userStore.loadMe()
    await load()
  } finally {
    saving.value = false
  }
}

async function changePwd() {
  await pwdRef.value.validate()
  changing.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordVisible.value = false
    userStore.logout()
    setTimeout(() => {
      location.href = '/login'
    }, 600)
  } finally {
    changing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.narrow {
  max-width: 900px;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 28px 30px;
}

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.avatar-wrap :deep(img) {
  border-radius: 24px;
  border: 4px solid #fff;
  box-shadow: 0 8px 24px rgba(16, 24, 40, 0.12);
}

.role {
  position: absolute;
  left: 50%;
  bottom: -10px;
  transform: translateX(-50%);
  white-space: nowrap;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--ink);
  padding: 4px 10px;
  border-radius: 999px;
}

.head-info {
  flex: 1;
  min-width: 0;
}

.head-info h1 {
  margin: 0 0 6px;
  font-size: 24px;
}

.head-info p {
  margin: 0 0 10px;
  font-size: 13px;
}

.head-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.form-card {
  margin-top: 18px;
  padding: 28px 30px 32px;
}

.form-card h2 {
  font-size: 20px;
  margin: 0 0 5px;
}

.form-card > .muted {
  font-size: 13px;
  margin: 0 0 24px;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 20px;
}

.avatar-picker {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.avatar-option {
  padding: 0;
  border: 2px solid transparent;
  border-radius: 16px;
  background: none;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.2s;
  line-height: 0;
}

.avatar-option img {
  width: 50px;
  height: 50px;
  border-radius: 14px;
}

.avatar-option:hover {
  transform: translateY(-2px);
}

.avatar-option.active {
  border-color: var(--brand);
  box-shadow: 0 0 0 4px rgba(255, 59, 48, 0.1);
}

@media (max-width: 720px) {
  .profile-head {
    flex-direction: column;
    align-items: flex-start;
  }
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
