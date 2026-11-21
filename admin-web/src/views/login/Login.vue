<template>
  <div class="login-page">
    <section class="visual">
      <div class="visual-inner">
        <div class="brand">
          <span class="mark"><el-icon><Lightning /></el-icon></span>
          <span>秒杀集市 · 运营后台</span>
        </div>

        <div class="copy">
          <p class="eyebrow">SECKILL OPERATIONS</p>
          <h1>把每一场秒杀<br />都运营得明明白白</h1>
          <p class="desc">
            商品、活动、订单、用户与权限，一屏掌控。实时查看秒杀库存与订单流转，关键操作全程留痕。
          </p>
        </div>

        <div class="mock">
          <div class="mock-card">
            <span class="mock-label">今日成交</span>
            <strong>¥ 128,640</strong>
            <em>↑ 23.5% 同比昨日</em>
          </div>
          <div class="mock-card">
            <span class="mock-label">待支付订单</span>
            <strong>36</strong>
            <em class="amber">15 分钟自动关闭</em>
          </div>
          <div class="mock-chart">
            <div class="bars">
              <i v-for="(h, i) in bars" :key="i" :style="{ height: h + '%' }"></i>
            </div>
            <span>近 7 天订单趋势</span>
          </div>
        </div>

        <div class="visual-foot">Spring Boot 3 · Redis Lua · Redis Stream · MySQL</div>
      </div>
      <div class="grid-bg"></div>
    </section>

    <section class="form-side">
      <div class="form-card">
        <h2>登录管理后台</h2>
        <p class="sub">使用管理员账号进入运营控制台</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" size="large" placeholder="管理员账号" prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="登录密码"
              prefix-icon="Lock"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
            进入控制台
          </el-button>
        </el-form>

        <div class="quick">
          <span>演示账号</span>
          <button @click="fill">admin / 123456</button>
        </div>

        <div class="hint">
          <el-icon><InfoFilled /></el-icon>
          管理端与用户端账号体系共用，但只有 ADMIN 身份可登录
        </div>

        <div class="links">
          <a href="http://localhost:5180" target="_blank">前往用户端商城 →</a>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

const formRef = ref()
const loading = ref(false)
const bars = [42, 58, 36, 74, 62, 88, 96]

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function fill() {
  form.username = 'admin'
  form.password = '123456'
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await adminStore.login({ username: form.username.trim(), password: form.password })
    ElMessage.success('欢迎回来')
    router.replace(route.query.redirect || '/dashboard')
  } catch (e) {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(420px, 0.85fr);
  background: #fff;
}

.visual {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(700px 480px at 80% 0%, rgba(37, 99, 235, 0.35), transparent 60%),
    radial-gradient(600px 480px at 0% 100%, rgba(14, 165, 233, 0.18), transparent 60%),
    linear-gradient(145deg, #060b16, #0d1730 55%, #0b1a3a);
  color: #fff;
  padding: 40px 52px;
}

.grid-bg {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.06) 1px, transparent 1px);
  background-size: 46px 46px;
  mask-image: radial-gradient(circle at 50% 40%, #000 20%, transparent 78%);
  pointer-events: none;
}

.visual-inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.mark {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: linear-gradient(145deg, #2563eb, #60a5fa);
  display: grid;
  place-items: center;
  font-size: 20px;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.4);
}

.copy {
  margin-top: 70px;
  max-width: 560px;
}

.eyebrow {
  font-size: 10px;
  letter-spacing: 4px;
  color: #60a5fa;
  margin: 0 0 18px;
}

.copy h1 {
  font-size: 44px;
  line-height: 1.24;
  letter-spacing: -1.5px;
  margin: 0 0 22px;
}

.desc {
  font-size: 14px;
  line-height: 1.9;
  color: #94a3b8;
  max-width: 460px;
  margin: 0;
}

.mock {
  margin-top: 52px;
  display: grid;
  grid-template-columns: 1fr 1fr 1.2fr;
  gap: 14px;
  max-width: 620px;
}

.mock-card,
.mock-chart {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 14px;
  padding: 16px;
  backdrop-filter: blur(8px);
}

.mock-label {
  font-size: 11px;
  color: #64748b;
}

.mock-card strong {
  display: block;
  font-size: 24px;
  margin: 9px 0 6px;
  letter-spacing: -0.5px;
}

.mock-card em {
  font-style: normal;
  font-size: 10px;
  color: #4ade80;
}

.mock-card em.amber {
  color: #fbbf24;
}

.mock-chart {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.bars {
  display: flex;
  align-items: flex-end;
  gap: 5px;
  height: 54px;
}

.bars i {
  flex: 1;
  border-radius: 3px 3px 0 0;
  background: linear-gradient(180deg, #60a5fa, #2563eb);
  min-height: 6px;
  opacity: 0.85;
}

.mock-chart span {
  font-size: 10px;
  color: #64748b;
  margin-top: 8px;
}

.visual-foot {
  margin-top: auto;
  font-size: 11px;
  color: #334155;
  letter-spacing: 0.6px;
}

.form-side {
  display: grid;
  place-items: center;
  padding: 40px;
  background: #fff;
}

.form-card {
  width: min(390px, 100%);
}

.form-card h2 {
  font-size: 28px;
  margin: 0 0 8px;
  letter-spacing: -0.8px;
}

.sub {
  color: var(--ink-3);
  font-size: 13px;
  margin: 0 0 30px;
}

.submit {
  width: 100%;
  height: 48px;
  font-size: 15px;
  margin-top: 4px;
}

.quick {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
}

.quick span {
  font-size: 12px;
  color: var(--ink-3);
}

.quick button {
  border: 1px solid var(--line);
  background: #f8fafc;
  color: var(--ink-2);
  font-size: 12px;
  padding: 5px 10px;
  border-radius: 999px;
  cursor: pointer;
}

.quick button:hover {
  border-color: #bfdbfe;
  color: var(--brand);
}

.hint {
  margin-top: 22px;
  display: flex;
  gap: 7px;
  font-size: 12px;
  color: var(--ink-3);
  line-height: 1.6;
  background: #f8fafc;
  border-radius: 10px;
  padding: 11px 13px;
}

.hint .el-icon {
  color: #60a5fa;
  margin-top: 2px;
}

.links {
  margin-top: 24px;
  font-size: 13px;
}

.links a {
  color: var(--brand);
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }
  .visual {
    display: none;
  }
  .form-side {
    min-height: 100vh;
  }
}
</style>
