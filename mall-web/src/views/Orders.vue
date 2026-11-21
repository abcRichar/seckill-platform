<template>
  <div class="page">
    <div class="container">
      <div class="head">
        <div>
          <h1>我的订单</h1>
          <p class="muted">秒杀订单请在 15 分钟内完成支付；退款需管理员审核通过</p>
        </div>
        <router-link to="/products" class="more-link">继续逛逛 <el-icon><ArrowRight /></el-icon></router-link>
      </div>

      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          class="tab"
          :class="{ active: status === tab.value }"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-loading="loading" class="list">
        <article v-for="order in orders" :key="order.id" class="order-card">
          <header>
            <div class="meta">
              <span class="time">{{ formatTime(order.createTime) }}</span>
              <span class="no">订单号 {{ order.orderNo }}</span>
            </div>
            <div class="badges">
              <span v-if="order.refundStatus === 1" class="refund-badge pending">退款审核中</span>
              <span v-else-if="order.refundStatus === 3" class="refund-badge rejected">退款被拒</span>
              <span class="status" :class="statusClass(order.status)">{{ statusText(order.status) }}</span>
            </div>
          </header>
          <div class="body">
            <img class="cover" :src="order.productCover" :alt="order.productName" @click="goDetail(order.id)" />
            <div class="product" @click="goDetail(order.id)">
              <h3>{{ order.productName }}</h3>
              <p class="muted">秒杀价 ¥{{ formatPrice(order.seckillPrice) }} × {{ order.quantity }}</p>
            </div>
            <div class="amount">
              <span class="muted">实付</span>
              <strong class="price"><small>¥</small>{{ formatPrice(order.seckillPrice * order.quantity) }}</strong>
            </div>
          </div>
          <footer>
            <el-button size="small" round @click="goDetail(order.id)">订单详情</el-button>
            <el-button
              v-if="order.status === 0"
              size="small"
              round
              @click="cancel(order)"
            >取消订单</el-button>
            <el-button
              v-if="canApplyRefund(order)"
              size="small"
              round
              type="warning"
              plain
              @click="openRefund(order)"
            >{{ order.refundStatus === 3 ? '重新申请退款' : '申请退款' }}</el-button>
            <el-button
              v-if="order.status === 0"
              type="primary"
              size="small"
              round
              :loading="payingId === order.id"
              @click="pay(order)"
            >去支付</el-button>
          </footer>
        </article>
      </div>

      <EmptyBlock v-if="!loading && orders.length === 0" text="还没有订单，去抢一件好物吧" icon="🧾">
        <el-button type="primary" round @click="$router.push('/products')">去逛逛</el-button>
      </EmptyBlock>

      <el-pagination
        v-if="total > size"
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="fetchOrders"
      />
    </div>

    <el-dialog v-model="refundVisible" title="申请退款" width="460px">
      <div v-if="refundTarget" class="refund-order">
        <img :src="refundTarget.productCover" alt="" />
        <div>
          <strong>{{ refundTarget.productName }}</strong>
          <p class="muted">订单 {{ refundTarget.orderNo }}</p>
          <span class="price">¥{{ formatPrice(refundTarget.seckillPrice * refundTarget.quantity) }}</span>
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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { applyRefund, cancelOrder, getOrders, payOrder } from '@/api'
import EmptyBlock from '@/components/EmptyBlock.vue'
import { formatPrice, formatTime } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const payingId = ref(null)
const orders = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(6)
const status = ref('')

const refundVisible = ref(false)
const refunding = ref(false)
const refundTarget = ref(null)
const refundForm = reactive({ reason: '', description: '' })
const reasons = ['临时不需要了', '拍错/多拍', '商品与描述不符', '商品质量问题', '物流问题', '其他']

const tabs = [
  { label: '全部', value: '' },
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '已退款', value: 4 },
  { label: '已取消', value: 2 }
]

async function fetchOrders() {
  loading.value = true
  try {
    const data = await getOrders({
      page: page.value,
      size: size.value,
      status: status.value === '' ? undefined : status.value
    })
    orders.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function switchTab(value) {
  status.value = value
  page.value = 1
  fetchOrders()
}

function canApplyRefund(order) {
  const statusOk = order.status === 1 || order.status === 3
  const refundOk = !order.refundStatus || order.refundStatus === 0 || order.refundStatus === 3
  return statusOk && refundOk
}

function openRefund(order) {
  refundTarget.value = order
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
    await applyRefund(refundTarget.value.id, {
      reason: refundForm.reason.trim(),
      description: refundForm.description.trim()
    })
    ElMessage.success('退款申请已提交，请等待管理员审核')
    refundVisible.value = false
    fetchOrders()
  } finally {
    refunding.value = false
  }
}

async function pay(order) {
  try {
    await ElMessageBox.confirm(
      `确认支付 ¥${formatPrice(order.seckillPrice * order.quantity)} 吗？（演示环境模拟支付）`,
      '确认支付',
      { confirmButtonText: '确认支付', cancelButtonText: '再想想', type: 'warning' }
    )
  } catch (e) {
    return
  }
  payingId.value = order.id
  try {
    await payOrder(order.id)
    ElMessage.success('支付成功')
    fetchOrders()
  } finally {
    payingId.value = null
  }
}

async function cancel(order) {
  try {
    await ElMessageBox.confirm('取消后库存会释放，确定取消该订单吗？', '取消订单', {
      confirmButtonText: '确定取消',
      cancelButtonText: '保留订单',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await cancelOrder(order.id)
  ElMessage.success('订单已取消，库存已释放')
  fetchOrders()
}

function goDetail(id) {
  router.push(`/order/${id}`)
}

function statusText(value) {
  return { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已完成', 4: '已退款' }[value] || '未知'
}

function statusClass(value) {
  return { 0: 'warn', 1: 'ok', 2: 'cancel', 3: 'ok', 4: 'refunded' }[value] || ''
}

onMounted(fetchOrders)
</script>

<style scoped>
.head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 22px;
}

.head h1 {
  margin: 0 0 6px;
  font-size: 26px;
  letter-spacing: -0.6px;
}

.head p {
  margin: 0;
  font-size: 13px;
}

.tabs {
  display: inline-flex;
  background: #fff;
  border-radius: 999px;
  padding: 4px;
  gap: 2px;
  margin-bottom: 18px;
  box-shadow: 0 2px 12px rgba(16, 24, 40, 0.05);
}

.tab {
  border: none;
  background: transparent;
  padding: 8px 20px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--ink-2);
  cursor: pointer;
  transition: all 0.2s;
}

.tab:hover {
  color: var(--ink);
}

.tab.active {
  background: var(--ink);
  color: #fff;
  font-weight: 600;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 140px;
}

.order-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--shadow);
  transition: box-shadow 0.2s;
}

.order-card:hover {
  box-shadow: var(--shadow-hover);
}

.order-card header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 18px;
  background: #fafbfc;
  border-bottom: 1px solid var(--line);
}

.meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--ink-3);
}

.badges {
  display: flex;
  align-items: center;
  gap: 8px;
}

.refund-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 9px;
  border-radius: 999px;
}

.refund-badge.pending {
  color: #b45309;
  background: #fffbeb;
}

.refund-badge.rejected {
  color: #b91c1c;
  background: #fef2f2;
}

.status {
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 999px;
}

.status.warn {
  color: #b45309;
  background: #fffbeb;
}

.status.ok {
  color: #15803d;
  background: #f0fdf4;
}

.status.cancel {
  color: #6b7280;
  background: #f3f4f6;
}

.status.refunded {
  color: #b45309;
  background: #fff7ed;
}

.body {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
}

.cover {
  width: 78px;
  height: 78px;
  border-radius: 12px;
  object-fit: cover;
  background: #f3f4f7;
  cursor: pointer;
  flex-shrink: 0;
}

.product {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.product h3 {
  margin: 0 0 7px;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product p {
  margin: 0;
  font-size: 12px;
}

.amount {
  text-align: right;
  flex-shrink: 0;
}

.amount .muted {
  display: block;
  font-size: 11px;
  margin-bottom: 3px;
}

.amount .price {
  font-size: 21px;
}

.order-card footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  padding: 0 18px 16px;
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
  .meta .no {
    display: none;
  }
  .body {
    flex-wrap: wrap;
  }
  .amount {
    width: 100%;
    text-align: left;
    padding-left: 94px;
  }
}
</style>
