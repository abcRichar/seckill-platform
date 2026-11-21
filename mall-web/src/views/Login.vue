<template>
  <div class="auth-page">
    <section class="brand-panel">
      <div class="brand-top">
        <span class="mark"><el-icon><Lightning /></el-icon></span>
        <span class="name">秒杀集市</span>
      </div>
      <div class="brand-copy">
        <h1>好物不该<br />错过一秒</h1>
        <p>Redis 预扣库存、异步下单、订单状态机——一个能写进简历的秒杀项目。</p>
        <div class="stats">
          <div><strong>{{ stats.activities }}</strong><em>正在进行</em></div>
          <div><strong>15min</strong><em>支付超时自动关闭</em></div>
          <div><strong>0</strong><em>超卖订单</em></div>
        </div>
      </div>
      <div class="brand-float f1">⚡</div>
      <div class="brand-float f2">🛍️</div>
      <div class="brand-float f3">⏱️</div>
      <div class="brand-foot">仅用于学习演示 · 不要填入真实密码</div>
    </section>

    <section class="form-panel">
      <div class="form-card">
        <header>
          <h2>欢迎回来</h2>
          <p>登录后参与秒杀、查看订单</p>
        </header>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
              prefix-icon="Lock"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
            登录
          </el-button>
        </el-form>

        <div class="quick">
          <span>演示账号</span>
          <button @click="fill('alice')">alice / 123456</button>
          <button @click="fill('bob')">bob / 123456</button>
        </div>

        <div class="switch">
          还没有账号？<router-link to="/register">免费注册</router-link>
        </div>

        <div class="admin-link">
          商家入口：<a href="http://localhost:5181" target="_blank">管理后台</a>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSeckillList } from '@/api'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const stats = reactive({ activities: 0 })

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function fill(name) {
  form.username = name
  form.password = '123456'
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login({ username: form.username.trim(), password: form.password })
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/')
  } catch (e) {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const list = await getSeckillList()
    stats.activities = (list || []).filter((a) => a.state === 'ONGOING').length
  } catch (e) {
    stats.activities = 0
  }
})
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
    radial-gradient(700px 500px at 85% 10%, rgba(255, 159, 28, 0.35), transparent 60%),
    radial-gradient(700px 500px at 10% 90%, rgba(255, 59, 48, 0.35), transparent 60%),
    linear-gradient(145deg, #0b0e13, #171b24 55%, #211310);
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
  box-shadow: 0 10px 24px rgba(255, 59, 48, 0.35);
}

.brand-copy {
  margin: auto 0;
  max-width: 520px;
  position: relative;
  z-index: 1;
}

.brand-copy h1 {
  font-size: 58px;
  line-height: 1.14;
  letter-spacing: -2px;
  margin: 0 0 24px;
}

.brand-copy p {
  color: rgba(255, 255, 255, 0.55);
  font-size: 15px;
  line-height: 1.9;
  max-width: 400px;
  margin: 0 0 40px;
}

.stats {
  display: flex;
  gap: 40px;
}

.stats strong {
  display: block;
  font-size: 30px;
  color: #ffb020;
  letter-spacing: -1px;
}

.stats em {
  display: block;
  font-style: normal;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  margin-top: 6px;
}

.brand-float {
  position: absolute;
  display: grid;
  place-items: center;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  animation: floaty 6s ease-in-out infinite;
}

.f1 {
  width: 96px;
  height: 96px;
  font-size: 42px;
  right: 12%;
  top: 22%;
}

.f2 {
  width: 74px;
  height: 74px;
  font-size: 32px;
  right: 22%;
  bottom: 18%;
  animation-delay: -2s;
}

.f3 {
  width: 60px;
  height: 60px;
  font-size: 26px;
  left: 8%;
  bottom: 12%;
  animation-delay: -4s;
}

.brand-foot {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.28);
  letter-spacing: 0.6px;
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
  margin: 0 0 30px;
}

.submit {
  width: 100%;
  height: 48px;
  font-size: 16px;
  margin-top: 6px;
}

.quick {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.quick span {
  font-size: 12px;
  color: var(--ink-3);
}

.quick button {
  border: 1px solid var(--line);
  background: #fafbfc;
  color: var(--ink-2);
  font-size: 12px;
  padding: 5px 10px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick button:hover {
  border-color: #ffc9c2;
  color: var(--brand);
}

.switch {
  margin-top: 26px;
  font-size: 13px;
  color: var(--ink-3);
}

.switch a {
  color: var(--brand);
  font-weight: 600;
}

.admin-link {
  margin-top: 12px;
  font-size: 12px;
  color: var(--ink-3);
}

.admin-link a {
  color: var(--ink-2);
  text-decoration: underline;
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
