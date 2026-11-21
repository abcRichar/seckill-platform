<template>
  <div v-loading="loading" class="page">
    <div class="container narrow">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/orders' }">我的订单</el-breadcrumb-item>
        <el-breadcrumb-item>订单详情</el-breadcrumb-item>
      </el-breadcrumb>

      <template v-if="order">
        <section class="status-card" :class="statusClass">
          <div class="status-main">
            <div class="status-icon">{{ statusIcon }}</div>
            <div>
              <h1>{{ statusText }}</h1>
              <p>{{ statusHint }}</p>
            </div>
          </div>
          <div v-if="order.status === 0" class="status-actions">
            <el-button round @click="cancel">取消订单</el-button>
            <el-button type="primary" round :loading="paying" @click="pay">立即支付</el-button>
          </div>
          <div v-else-if="canApplyRefund" class="status-actions">
            <el-button type="warning" plain round @click="openRefund">
              {{ order.refundStatus === 3 ? '重新申请退款' : '申请退款' }}
            </el-button>
          </div>
        </section>

        <section v-if="order.status !== 2 && order.status !== 4" class="steps card">
          <el-steps :active="activeStep" align-center finish-status="success">
            <el-step title="提交订单" :description="formatTime(order.createTime)" />
            <el-step title="完成支付" :description="order.payTime ? formatTime(order.payTime) : '等待支付'" />
            <el-step title="交易完成" description="支付后商家发货" />
          </el-steps>
        </section>

        <section class="product card">
          <img :src="order.productCover" :alt="order.productName" />
          <div class="p-info">
            <h2>{{ order.productName }}</h2>
            <p class="muted">秒杀商品 · 数量 {{ order.quantity }}</p>
            <div class="p-price">
              <span class="price"><small>¥</small>{{ formatPrice(order.seckillPrice) }}</span>
              <span class="muted">× {{ order.quantity }}</span>
            </div>
          </div>
          <router-link :to="`/seckill/${order.activityId}`" class="p-link">查看活动</router-link>
        </section>

        <section v-if="refund || order.refundStatus" class="refund-card card">
          <div class="refund-head">
            <h3>退款信息</h3>
            <span class="refund-status" :class="refundStatusClass(order.refundStatus)">
              {{ refundStatusText(order.refundStatus) }}
            </span>
          </div>

          <div v-if="refund" class="refund-rows">
            <div><span>退款单号</span><b>{{ refund.refundNo }}</b></div>
            <div><span>退款金额</span><b class="price">¥{{ formatPrice(refund.refundAmount) }}</b></div>
            <div><span>退款原因</span><b>{{ refund.reason }}</b></div>
            <div v-if="refund.description"><span>补充说明</span><b>{{ refund.description }}</b></div>
            <div><span>申请时间</span><b>{{ formatTime(refund.createTime) }}</b></div>
            <div v-if="refund.auditTime"><span>审核时间</span><b>{{ formatTime(refund.auditTime) }}</b></div>
            <div v-if="refund.auditRemark"><span>审核备注</span><b>{{ refund.auditRemark }}</b></div>
            <div v-if="refund.refundTime"><span>退款时间</span><b>{{ formatTime(refund.refundTime) }}</b></div>
          </div>

          <div v-if="order.refundStatus === 1" class="refund-note">
            退款申请已提交，管理员审核通过后订单会变为“已退款”。
          </div>
          <div v-if="order.refundStatus === 3" class="refund-note rejected">
            退款申请被拒绝，你可以修改原因后重新申请。
          </div>

        </section>

        <section class="info card">
          <h3>订单信息</h3>
          <div class="rows">
            <div><span>订单编号</span><b>{{ order.orderNo }}</b></div>
            <div><span>下单时间</span><b>{{ formatTime(order.createTime) }}</b></div>
            <div><span>支付时间</span><b>{{ order.payTime ? formatTime(order.payTime) : '-' }}</b></div>
            <div><span>订单状态</span><b>{{ statusText }}</b></div>
            <div v-if="order.refundTime"><span>退款时间</span><b>{{ formatTime(order.refundTime) }}</b></div>
            <div><span>支付方式</span><b>模拟支付（演示）</b></div>
            <div class="total"><span>实付金额</span><b class="price"><small>¥</small>{{ totalAmount }}</b></div>
          </div>
        </section>

        <div class="tips">
          温馨提示：秒杀订单需在 15 分钟内支付，超时系统会自动取消并释放库存；退款只退金额，不回补秒杀库存。
        </div>
      </template>
    </div>

    <el-dialog v-model="refundVisible" title="申请退款" width="460px">
      <div v-if="order" class="refund-order">
        <img :src="order.productCover" alt="" />
        <div>
          <strong>{{ order.productName }}</strong>
          <p class="muted">订单 {{ order.orderNo }}</p>
          <span class="price">¥{{ totalAmount }}</span>
        </div>
      </div>
      <el-form :model="refundForm" label-position="top">
        <el-form-item label="退款原因" required>
          <el-select
            v-model="refundForm.reason"
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入退款原因"
            style="width: 100%"
          >
            <el-option v-for="item in reasons" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input
            v-model="refundForm.description"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="选填，帮助客服更快处理"
          />
        </el-form-item>
      </el-form>
      <div class="refund-tip">
        <el-icon><InfoFilled /></el-icon>
        提交后进入管理员审核，审核通过后订单才会变为“已退款”。
      </div>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="warning" :loading="refunding" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { applyRefund, cancelOrder, getOrder, getOrderRefund, payOrder } from '@/api'
import { formatPrice, formatTime } from '@/utils/format'

const route = useRoute()
const loading = ref(true)
const paying = ref(false)
const order = ref(null)
const refund = ref(null)

const refundVisible = ref(false)
const refunding = ref(false)
const refundForm = reactive({ reason: '', description: '' })
const reasons = ['临时不需要了', '拍错/多拍', '商品与描述不符', '商品质量问题', '物流问题', '其他']

const statusText = computed(() => {
  if (!order.value) return ''
  return { 0: '等待支付', 1: '支付成功', 2: '订单已取消', 3: '交易完成', 4: '订单已退款' }[order.value.status]
})

const statusHint = computed(() => {
  if (!order.value) return ''
  if (order.value.status === 0) return '请在 15 分钟内完成支付，超时订单将自动取消'
  if (order.value.status === 1) return '支付已完成，我们会尽快为你发货'
  if (order.value.status === 2) return '订单已取消，秒杀库存已释放'
  if (order.value.status === 4) return '退款已完成，金额将按原支付方式退回'
  return '交易已完成，感谢你的支持'
})

const statusIcon = computed(() => ({ 0: '⏳', 1: '✅', 2: '🚫', 3: '🎉', 4: '💸' }[order.value?.status] || '📦'))

const statusClass = computed(
  () => ({ 0: 'warn', 1: 'ok', 2: 'cancel', 3: 'ok', 4: 'refunded' }[order.value?.status] || '')
)

const activeStep = computed(() => {
  if (!order.value) return 0
  if (order.value.status === 0) return 1
  if (order.value.status === 1) return 2
  if (order.value.status === 3) return 3
  return 1
})

const totalAmount = computed(() =>
  order.value ? formatPrice(order.value.seckillPrice * order.value.quantity) : '0.00'
)

const canApplyRefund = computed(() => {
  if (!order.value) return false
  const statusOk = order.value.status === 1 || order.value.status === 3
  const refundOk =
    !order.value.refundStatus || order.value.refundStatus === 0 || order.value.refundStatus === 3
  return statusOk && refundOk
})

async function load() {
  loading.value = true
  try {
    order.value = await getOrder(route.params.id)
    try {
      refund.value = await getOrderRefund(route.params.id)
    } catch (e) {
      refund.value = null
    }
  } catch (e) {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function openRefund() {
  refundForm.reason = ''
  refundForm.description = ''
  refundVisible.value = true
}

async function submitRefund() {
  if (!refundForm.reason || !refundForm.reason.trim()) {
    ElMessage.warning('请选择或填写退款原因')
    return
  }
  refunding.value = true
  try {
    await applyRefund(order.value.id, {
      reason: refundForm.reason.trim(),
      description: refundForm.description.trim()
    })
    ElMessage.success('退款申请已提交，请等待管理员审核')
    refundVisible.value = false
    await load()
  } finally {
    refunding.value = false
  }
}

async function pay() {
  try {
    await ElMessageBox.confirm(`确认支付 ¥${totalAmount.value} 吗？`, '确认支付', {
      confirmButtonText: '确认支付',
      cancelButtonText: '再想想',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  paying.value = true
  try {
    await payOrder(order.value.id)
    ElMessage.success('支付成功')
    await load()
  } finally {
    paying.value = false
  }
}

async function cancel() {
  try {
    await ElMessageBox.confirm('取消后库存会释放，确定取消吗？', '取消订单', {
      confirmButtonText: '确定取消',
      cancelButtonText: '保留订单',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await cancelOrder(order.value.id)
  ElMessage.success('订单已取消')
  await load()
}

function refundStatusText(status) {
  return { 0: '无退款', 1: '退款审核中', 2: '已退款', 3: '退款被拒' }[status || 0] || '未知'
}

function refundStatusClass(status) {
  return { 1: 'pending', 2: 'refunded', 3: 'rejected' }[status] || ''
}

onMounted(load)
</script>

<style scoped>
.narrow {
  max-width: 900px;
}

.el-breadcrumb {
  margin-bottom: 18px;
  font-size: 13px;
}

.status-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-radius: 20px;
  padding: 28px 32px;
  color: #fff;
  background: linear-gradient(135deg, #0f172a, #263244);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.18);
  flex-wrap: wrap;
}

.status-card.warn {
  background:
    radial-gradient(400px 200px at 90% 0%, rgba(255, 159, 28, 0.4), transparent 60%),
    linear-gradient(135deg, #1c1408, #3b2510);
}

.status-card.ok {
  background:
    radial-gradient(400px 200px at 90% 0%, rgba(74, 222, 128, 0.3), transparent 60%),
    linear-gradient(135deg, #071a12, #113626);
}

.status-card.cancel {
  background: linear-gradient(135deg, #1f2329, #3b4048);
}

.status-card.refunded {
  background:
    radial-gradient(400px 200px at 90% 0%, rgba(251, 146, 60, 0.35), transparent 60%),
    linear-gradient(135deg, #1c1008, #43220f);
}

.status-main {
  display: flex;
  align-items: center;
  gap: 18px;
}

.status-icon {
  width: 60px;
  height: 60px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
  display: grid;
  place-items: center;
  font-size: 28px;
}

.status-card h1 {
  margin: 0 0 6px;
  font-size: 25px;
  letter-spacing: -0.4px;
}

.status-card p {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.status-actions {
  display: flex;
  gap: 10px;
}

.steps {
  margin-top: 18px;
  padding: 26px 20px 14px;
}

.product {
  margin-top: 18px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 18px;
  position: relative;
}

.product img {
  width: 96px;
  height: 96px;
  border-radius: 14px;
  object-fit: cover;
  background: #f3f4f7;
}

.p-info {
  flex: 1;
  min-width: 0;
}

.p-info h2 {
  font-size: 17px;
  margin: 0 0 7px;
}

.p-info p {
  margin: 0 0 10px;
  font-size: 12px;
}

.p-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.p-price .price {
  font-size: 22px;
}

.p-link {
  font-size: 13px;
  color: var(--ink-3);
  position: absolute;
  right: 20px;
  top: 20px;
}

.p-link:hover {
  color: var(--brand);
}

.refund-card {
  margin-top: 18px;
  padding: 22px 28px 24px;
}

.refund-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.refund-head h3 {
  margin: 0;
  font-size: 17px;
}

.refund-status {
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 999px;
  color: #b45309;
  background: #fffbeb;
}

.refund-status.refunded {
  color: #b45309;
  background: #fff7ed;
}

.refund-status.rejected {
  color: #b91c1c;
  background: #fef2f2;
}

.refund-rows {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.refund-rows div {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  font-size: 13px;
  color: var(--ink-3);
}

.refund-rows b {
  color: var(--ink);
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}

.refund-note {
  margin-top: 15px;
  font-size: 12px;
  color: #b45309;
  background: #fffbeb;
  border-radius: 9px;
  padding: 9px 12px;
}

.refund-note.rejected {
  color: #b91c1c;
  background: #fef2f2;
}

.info {
  margin-top: 18px;
  padding: 24px 28px;
}

.info h3 {
  margin: 0 0 18px;
  font-size: 17px;
}

.rows {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.rows div {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  font-size: 13px;
  color: var(--ink-3);
}

.rows b {
  color: var(--ink);
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}

.rows .total {
  border-top: 1px dashed var(--line);
  padding-top: 15px;
  align-items: baseline;
}

.rows .total .price {
  font-size: 26px;
}

.tips {
  margin-top: 16px;
  font-size: 12px;
  color: var(--ink-3);
  text-align: center;
}

.refund-order {
  display: flex;
  gap: 13px;
  background: #f8fafc;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 16px;
}

.refund-order img {
  width: 62px;
  height: 62px;
  border-radius: 10px;
  object-fit: cover;
}

.refund-order strong {
  display: block;
  font-size: 14px;
  margin-bottom: 4px;
}

.refund-order p {
  margin: 0 0 6px;
  font-size: 11px;
}

.refund-tip {
  display: flex;
  gap: 7px;
  font-size: 12px;
  color: var(--ink-3);
  background: #f8fafc;
  border-radius: 10px;
  padding: 10px 12px;
  line-height: 1.6;
}

.refund-tip .el-icon {
  color: #f59e0b;
  margin-top: 2px;
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .status-card {
    padding: 22px;
  }
  .rows div,
  .refund-rows div {
    flex-direction: column;
    gap: 4px;
  }
  .rows b,
  .refund-rows b {
    text-align: left;
  }
}
</style>
