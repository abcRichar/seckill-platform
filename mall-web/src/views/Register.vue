<template>
  <div class="auth-page">
    <section class="brand-panel">
      <div class="brand-top">
        <span class="mark"><el-icon><Lightning /></el-icon></span>
        <span class="name">秒杀集市</span>
      </div>
      <div class="brand-copy">
        <h1>加入我们<br />好物一键抢</h1>
        <p>注册即可参与每日限时秒杀，库存有限，先到先得。</p>
        <ul>
          <li><span>01</span> 正品保障，品牌直供</li>
          <li><span>02</span> 秒杀价低至 5 折</li>
          <li><span>03</span> 15 分钟支付保护</li>
        </ul>
      </div>
      <div class="brand-float">🎁</div>
      <div class="brand-foot">仅用于学习演示 · 不要填入真实密码</div>
    </section>

    <section class="form-panel">
      <div class="form-card">
        <header>
          <h2>创建账号</h2>
          <p>30 秒完成注册，立即参与秒杀</p>
        </header>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" size="large" placeholder="4-20 位字母、数字或下划线" prefix-icon="User" />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" size="large" placeholder="怎么称呼你（选填）" prefix-icon="Avatar" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="6-20 位密码" prefix-icon="Lock" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirm">
            <el-input
              v-model="form.confirm"
              size="large"
              type="password"
              show-password
              placeholder="再次输入密码"
              prefix-icon="Lock"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
            注册并登录
          </el-button>
        </el-form>

        <div class="switch">
          已有账号？<router-link to="/login">去登录</router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', nickname: '', password: '', confirm: '' })

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '需为 4-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.register({
      username: form.username.trim(),
      password: form.password,
      nickname: form.nickname.trim() || undefined
    })
    ElMessage.success('注册成功，欢迎加入')
    router.replace('/')
  } catch (e) {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(420px, 0.9fr);
  background: #fff;
}

.brand-panel {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(600px 420px at 80% 15%, rgba(255, 159, 28, 0.32), transparent 60%),
    radial-gradient(700px 500px at 5% 95%, rgba(255, 59, 48, 0.3), transparent 60%),
    linear-gradient(145deg, #0b0e13, #161a22 55%, #201310);
  color: #fff;
  padding: 44px 56px;
  display: flex;
  flex-direction: column;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 1px;
}

.mark {
  width: 40px;
  height: 40px;
  border-radius: 13px;
  background: linear-gradient(145deg, #ff3b30, #ff7a45);
  display: grid;
  place-items: center;
  font-size: 21px;
}

.brand-copy {
  margin: auto 0;
  z-index: 1;
}

.brand-copy h1 {
  font-size: 54px;
  line-height: 1.16;
  letter-spacing: -2px;
  margin: 0 0 20px;
}

.brand-copy p {
  color: rgba(255, 255, 255, 0.55);
  font-size: 15px;
  line-height: 1.9;
  margin: 0 0 34px;
}

.brand-copy ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.brand-copy li {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-copy li span {
  font-size: 11px;
  color: #ffb020;
  font-weight: 700;
  border: 1px solid rgba(255, 176, 32, 0.35);
  border-radius: 6px;
  padding: 3px 7px;
}

.brand-float {
  position: absolute;
  right: 12%;
  bottom: 22%;
  width: 100px;
  height: 100px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: grid;
  place-items: center;
  font-size: 46px;
  animation: floaty 6s ease-in-out infinite;
}

.brand-foot {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.28);
}

.form-panel {
  display: grid;
  place-items: center;
  padding: 40px;
  background: #fff;
}

.form-card {
  width: min(400px, 100%);
}

header h2 {
  font-size: 30px;
  margin: 0 0 8px;
  letter-spacing: -0.8px;
}

header p {
  color: var(--ink-3);
  font-size: 14px;
  margin: 0 0 26px;
}

.submit {
  width: 100%;
  height: 48px;
  font-size: 16px;
  margin-top: 4px;
}

.switch {
  margin-top: 24px;
  font-size: 13px;
  color: var(--ink-3);
}

.switch a {
  color: var(--brand);
  font-weight: 600;
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }
  .brand-panel {
    display: none;
  }
  .form-panel {
    min-height: 100vh;
  }
}
</style>
