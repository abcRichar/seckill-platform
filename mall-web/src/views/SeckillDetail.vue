<template>
  <div v-loading="loading" class="page seckill-page">
    <div class="container">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>秒杀专场</el-breadcrumb-item>
        <el-breadcrumb-item>{{ activity?.title || '活动详情' }}</el-breadcrumb-item>
      </el-breadcrumb>

      <section v-if="activity" class="hero">
        <div class="glow"></div>
        <div class="hero-content">
          <div class="hero-main">
            <div class="state-line">
              <span class="state-pill" :class="activity.state.toLowerCase()">{{ stateText }}</span>
              <span class="limit">每人限购 {{ activity.limitPerUser }} 件</span>
            </div>
            <h1>{{ activity.title }}</h1>
            <p class="subtitle">{{ activity.productSubtitle }}</p>

            <div class="price-line">
              <span class="seckill-price"><small>¥</small>{{ splitPrice(activity.seckillPrice).int }}<span class="decimal">.{{ splitPrice(activity.seckillPrice).dec }}</span></span>
              <span class="original-price">日常价 ¥{{ formatPrice(activity.originalPrice) }}</span>
              <span class="save">立省 ¥{{ formatPrice(activity.originalPrice - activity.seckillPrice) }}</span>
            </div>

            <div class="progress-line">
              <div class="progress">
                <i :style="{ width: activity.soldPercent + '%' }"></i>
              </div>
              <div class="progress-meta">
                <span>已抢 {{ activity.soldPercent }}%</span>
                <span>仅剩 <b>{{ activity.stock }}</b> 件</span>
              </div>
            </div>

            <div class="countdown-line">
              <span class="cd-title">{{ activity.state === 'UPCOMING' ? '距开始' : '距结束' }}</span>
              <CountdownTimer
                :start="activity.startTime"
                :end="activity.endTime"
                tone="dark"
                hide-label
                @ended="loadDetail"
              />
              <span class="server-time">活动时间：{{ activity.startTime }} ~ {{ activity.endTime }}</span>
            </div>

            <div class="action-line">
              <el-button
                type="primary"
                size="large"
                round
                class="buy-btn"
                :loading="submitting"
                :disabled="!canBuy && !alreadyBuy"
                @click="handleBuy"
              >
                {{ buttonText }}
              </el-button>
              <span class="hint">
                <el-icon><Lock /></el-icon>
                秒杀令牌一次性有效 · 每人限购 {{ activity.limitPerUser }} 件
              </span>
            </div>
          </div>

          <div class="hero-side">
            <div class="image-card">
              <img :src="activity.productCover" :alt="activity.title" />
              <span class="corner">SECKILL</span>
            </div>
            <div class="rules">
              <div><span>01</span> 活动开始前可提前查看，准时开抢</div>
              <div><span>02</span> 下单后 15 分钟内完成支付，超时自动取消</div>
              <div><span>03</span> 同一账号同一活动仅可参与一次</div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="activity" class="desc card">
        <h2>商品详情</h2>
        <p class="desc-meta">{{ activity.productName }} · {{ activity.productSubtitle }}</p>
        <div class="desc-body">{{ activity.productDescription || '暂无更多描述' }}</div>
      </section>
    </div>

    <el-dialog
      v-model="queueVisible"
      width="420px"
      :close-on-click-modal="false"
      :show-close="false"
      align-center
    >
      <div class="queue">
        <template v-if="queueState === 'queue'">
          <div class="spinner"></div>
          <h3>正在为你创建订单</h3>
          <p>请求已进入队列，正在异步落库，请稍候…</p>
          <div class="steps-text">Lua 扣减库存 → Redis Stream → 订单落库</div>
        </template>
        <template v-else-if="queueState === 'success'">
          <div class="circle success">✓</div>
          <h3>恭喜，抢到了！</h3>
          <p>订单已创建，请在 15 分钟内完成支付，否则自动取消。</p>
          <div class="queue-actions">
            <el-button type="primary" round @click="goOrder">去支付</el-button>
            <el-button round @click="queueVisible = false">继续逛</el-button>
          </div>
        </template>
        <template v-else>
          <div class="circle fail">!</div>
          <h3>很遗憾，没有抢到</h3>
          <p>{{ failMessage }}</p>
          <div class="queue-actions">
            <el-button round @click="queueVisible = false">知道了</el-button>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getMyActivityStatus,
  getSeckillDetail,
  getSeckillResult,
  getSeckillToken,
  placeSeckillOrder
} from '@/api'
import CountdownTimer from '@/components/CountdownTimer.vue'
import { useUserStore } from '@/stores/user'
import { formatPrice, splitPrice } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const submitting = ref(false)
const activity = ref(null)
const mine = ref({ purchased: false })
const queueVisible = ref(false)
const queueState = ref('queue')
const failMessage = ref('')
const currentOrderId = ref(null)
let polling = false

const stateText = computed(() => {
  const map = { ONGOING: '抢购中', UPCOMING: '即将开始', ENDED: '已结束' }
  return map[activity.value?.state] || '活动详情'
})

const alreadyBuy = computed(() => mine.value?.purchased)

const canBuy = computed(() => {
  return (
    activity.value &&
    activity.value.state === 'ONGOING' &&
    activity.value.stock > 0 &&
    !mine.value?.purchased
  )
})

const buttonText = computed(() => {
  if (!userStore.isLogin) return '登录后抢购'
  if (alreadyBuy.value) return '已参与 · 查看订单'
  if (!activity.value) return '立即抢购'
  if (activity.value.state === 'UPCOMING') return '即将开始'
  if (activity.value.state === 'ENDED') return '活动已结束'
  if (activity.value.stock <= 0) return '已抢光'
  return '立即秒杀'
})

async function loadDetail() {
  loading.value = true
  try {
    activity.value = await getSeckillDetail(route.params.id)
    if (userStore.isLogin) {
      await loadMine()
    }
  } catch (e) {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function loadMine() {
  try {
    mine.value = await getMyActivityStatus(route.params.id)
  } catch (e) {
    mine.value = { purchased: false }
  }
}

async function handleBuy() {
  if (!userStore.isLogin) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (alreadyBuy.value) {
    if (mine.value.orderId) router.push(`/order/${mine.value.orderId}`)
    else router.push('/orders')
    return
  }
  if (activity.value.state === 'UPCOMING') {
    ElMessage.info('活动还没开始，请稍等')
    return
  }
  if (activity.value.state === 'ENDED' || activity.value.stock <= 0) {
    ElMessage.warning('本场活动已结束')
    return
  }
  if (!activity.value.seckillPath) {
    ElMessage.error('秒杀链接未就绪，请刷新页面')
    return
  }

  submitting.value = true
  try {
    const tokenData = await getSeckillToken(activity.value.id, activity.value.seckillPath)
    const result = await placeSeckillOrder({
      activityId: activity.value.id,
      token: tokenData.token,
      path: activity.value.seckillPath,
      quantity: 1
    })
    currentOrderId.value = null
    queueState.value = 'queue'
    failMessage.value = ''
    queueVisible.value = true
    await pollResult(result.orderNo)
  } catch (e) {
    // 1002 已参与：刷新参与状态，把按钮切到"查看订单"
    if (e.bizCode === 1002 || String(e.message || '').includes('已参与')) {
      await loadMine()
    }
  } finally {
    submitting.value = false
  }
}

async function pollResult(orderNo) {
  if (polling) return
  polling = true
  try {
    for (let i = 0; i < 20; i++) {
      await sleep(900)
      try {
        const data = await getSeckillResult(orderNo)
        if (data.status === 'SUCCESS') {
          currentOrderId.value = data.orderId
          queueState.value = 'success'
          await loadMine()
          loadDetail()
          return
        }
        if (data.status === 'FAILED') {
          failMessage.value = data.message || '库存不足或系统繁忙'
          queueState.value = 'fail'
          await loadMine()
          return
        }
      } catch (e) {
        // 订单还未落库时偶尔会返回不存在，继续轮询
      }
    }
    queueState.value = 'fail'
    failMessage.value = '订单仍在排队中，可稍后到「我的订单」查看'
  } finally {
    polling = false
  }
}

function goOrder() {
  if (currentOrderId.value) {
    router.push(`/order/${currentOrderId.value}`)
  } else {
    router.push('/orders')
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

onMounted(loadDetail)
onBeforeUnmount(() => {
  polling = false
})
</script>

<style scoped>
.el-breadcrumb {
  margin-bottom: 18px;
  font-size: 13px;
}

.hero {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  background:
    radial-gradient(700px 300px at 90% 0%, rgba(255, 122, 69, 0.28), transparent 60%),
    radial-gradient(600px 340px at 0% 100%, rgba(255, 59, 48, 0.22), transparent 60%),
    linear-gradient(135deg, #0c0f14, #191d26 60%, #22130f);
  color: #fff;
  padding: 38px 40px 34px;
  box-shadow: 0 22px 60px rgba(12, 15, 20, 0.22);
}

.glow {
  position: absolute;
  right: -120px;
  top: -140px;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: inset 0 0 120px rgba(255, 159, 28, 0.08);
}

.hero-content {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.9fr);
  gap: 40px;
}

.state-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.state-pill {
  font-size: 12px;
  font-weight: 700;
  padding: 5px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.state-pill.ongoing {
  background: linear-gradient(135deg, #ff3b30, #ff7a45);
  border-color: transparent;
}

.state-pill.upcoming {
  background: rgba(255, 159, 28, 0.18);
  color: #ffcf8a;
  border-color: rgba(255, 159, 28, 0.28);
}

.limit {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.52);
}

.hero h1 {
  font-size: 36px;
  line-height: 1.25;
  letter-spacing: -1px;
  margin: 18px 0 10px;
}

.subtitle {
  color: rgba(255, 255, 255, 0.58);
  font-size: 14px;
  margin: 0;
}

.price-line {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin: 24px 0 20px;
  flex-wrap: wrap;
}

.seckill-price {
  font-size: 48px;
  font-weight: 800;
  color: #ff8a5b;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.seckill-price small {
  font-size: 0.5em;
  margin-right: 2px;
}

.seckill-price .decimal {
  font-size: 0.58em;
}

.original-price {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.42);
  text-decoration: line-through;
}

.save {
  font-size: 12px;
  color: #ffcf8a;
  background: rgba(255, 159, 28, 0.14);
  padding: 4px 10px;
  border-radius: 999px;
}

.progress-line {
  max-width: 520px;
}

.progress {
  height: 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
}

.progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #ff9f1c, #ff3b30);
  box-shadow: 0 0 18px rgba(255, 90, 60, 0.6);
  transition: width 0.8s;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 8px;
}

.progress-meta b {
  color: #ff8a5b;
  font-size: 14px;
}

.countdown-line {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 24px 0;
  flex-wrap: wrap;
}

.cd-title {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
}

.server-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.32);
}

.action-line {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.buy-btn {
  min-width: 190px;
  height: 52px;
  font-size: 17px;
  border: none;
  background: linear-gradient(135deg, #ff3b30, #ff7a45);
  box-shadow: 0 14px 30px rgba(255, 59, 48, 0.35);
}

.hint {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.image-card {
  position: relative;
  border-radius: 18px;
  overflow: hidden;
  aspect-ratio: 4 / 3;
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.35);
}

.image-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.corner {
  position: absolute;
  right: 12px;
  bottom: 12px;
  font-size: 10px;
  letter-spacing: 3px;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(0, 0, 0, 0.32);
  padding: 4px 9px;
  border-radius: 6px;
  backdrop-filter: blur(6px);
}

.rules {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.rules div {
  display: flex;
  gap: 9px;
  line-height: 1.6;
}

.rules span {
  color: rgba(255, 138, 91, 0.8);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.desc {
  margin-top: 22px;
  padding: 28px 32px 34px;
}

.desc h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.desc-meta {
  color: var(--ink-3);
  font-size: 13px;
  margin: 0 0 18px;
}

.desc-body {
  white-space: pre-line;
  line-height: 2;
  color: var(--ink-2);
  font-size: 14px;
}

.queue {
  text-align: center;
  padding: 12px 10px 4px;
}

.queue h3 {
  font-size: 20px;
  margin: 18px 0 8px;
}

.queue p {
  color: var(--ink-3);
  font-size: 13px;
  line-height: 1.7;
  margin: 0 auto;
  max-width: 300px;
}

.spinner {
  width: 62px;
  height: 62px;
  margin: 6px auto 0;
  border-radius: 50%;
  border: 4px solid #ffe4df;
  border-top-color: var(--brand);
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.circle {
  width: 62px;
  height: 62px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  margin: 6px auto 0;
  font-size: 30px;
  font-weight: 800;
  color: #fff;
}

.circle.success {
  background: linear-gradient(135deg, #16a34a, #4ade80);
  box-shadow: 0 12px 26px rgba(22, 163, 74, 0.28);
}

.circle.fail {
  background: linear-gradient(135deg, #64748b, #94a3b8);
}

.steps-text {
  margin-top: 18px;
  font-size: 11px;
  color: var(--ink-3);
  background: #f6f7f9;
  padding: 8px 12px;
  border-radius: 999px;
  display: inline-block;
}

.queue-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 22px;
}

@media (max-width: 960px) {
  .hero-content {
    grid-template-columns: 1fr;
  }
  .hero {
    padding: 26px 22px;
  }
  .hero h1 {
    font-size: 26px;
  }
  .hero-side {
    display: none;
  }
}
</style>
