<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>{{ greeting }}，{{ adminStore.nickname }}</h1>
        <p>{{ today }} · 这是今天的平台运营概览</p>
      </div>
      <div class="head-actions">
        <button v-if="data.pendingRefunds" class="refund-chip" @click="$router.push('/refunds')">
          <span class="dot"></span> 待审核退款 {{ data.pendingRefunds }} 笔
        </button>
        <el-button round @click="load">
          <el-icon><Refresh /></el-icon> 刷新数据
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="stat-grid">
      <div class="stat-card" v-for="item in stats" :key="item.label" :style="{ '--c': item.color }">
        <div class="stat-icon"><el-icon><component :is="item.icon" /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">{{ item.label }}</span>
          <strong class="stat-value">{{ item.value }}</strong>
          <em class="stat-sub">{{ item.sub }}</em>
        </div>
        <div class="stat-glow"></div>
      </div>
    </div>

    <div class="middle">
      <section class="card chart-card">
        <header class="card-head">
          <div>
            <h2>近 7 天订单趋势</h2>
            <p>按下单时间统计，含已取消订单</p>
          </div>
          <span class="legend"><i></i>订单量</span>
        </header>
        <div class="chart">
          <div v-for="(item, index) in trend" :key="index" class="bar-col">
            <span class="bar-value">{{ item.orders }}</span>
            <div class="bar-track">
              <div class="bar" :style="{ height: barHeight(item.orders) + '%' }"></div>
            </div>
            <span class="bar-label">{{ item.day }}</span>
          </div>
        </div>
      </section>

      <section class="card donut-card">
        <header class="card-head">
          <div>
            <h2>订单状态</h2>
            <p>当前全部订单</p>
          </div>
        </header>
        <div class="donut-wrap">
          <div class="donut" :style="{ background: donutGradient }">
            <div class="donut-hole">
              <strong>{{ totalOrders }}</strong>
              <span>订单总数</span>
            </div>
          </div>
          <div class="legend-list">
            <div><i class="amber"></i><span>待支付</span><b>{{ data.unpaidOrders || 0 }}</b></div>
            <div><i class="green"></i><span>已支付</span><b>{{ data.paidOrders || 0 }}</b></div>
            <div><i class="orange"></i><span>已退款</span><b>{{ refundedOrders }}</b></div>
            <div><i class="gray"></i><span>其他</span><b>{{ otherOrders }}</b></div>
          </div>
        </div>
      </section>
    </div>

    <section class="card recent">
      <header class="card-head">
        <div>
          <h2>最新订单</h2>
          <p>实时展示最近 8 笔秒杀订单</p>
        </div>
        <el-button text type="primary" @click="$router.push('/orders')">查看全部 →</el-button>
      </header>
      <div class="table-wrap">
        <el-table :data="data.recentOrders || []" style="width: 100%">
          <el-table-column prop="orderNo" label="订单号" min-width="210" />
          <el-table-column label="商品" min-width="220">
            <template #default="{ row }">
              <div class="product-cell">
                <img class="thumb" :src="row.productCover" alt="" />
                <span>{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="120">
            <template #default="{ row }">
              <span class="price">¥{{ formatPrice(row.seckillPrice * row.quantity) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" effect="light" round size="small">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getDashboard } from '@/api'
import { useAdminStore } from '@/stores/admin'
import { formatPrice, formatTime } from '@/utils/format'

const adminStore = useAdminStore()
const loading = ref(false)
const data = reactive({})

const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const stats = computed(() => [
  {
    label: '今日订单',
    value: data.todayOrders ?? 0,
    sub: `待支付 ${data.unpaidOrders ?? 0} 笔`,
    icon: 'Tickets',
    color: '#2563eb'
  },
  {
    label: '今日成交额',
    value: `¥${formatPrice(data.todaySales).replace('.00', '')}`,
    sub: `已支付 ${data.paidOrders ?? 0} 笔`,
    icon: 'Money',
    color: '#10b981'
  },
  {
    label: '在售商品',
    value: data.productCount ?? 0,
    sub: `平台用户 ${data.userCount ?? 0} 人`,
    icon: 'Goods',
    color: '#f59e0b'
  },
  {
    label: '活跃秒杀',
    value: data.activeActivities ?? 0,
    sub: 'Redis 库存已预热',
    icon: 'Timer',
    color: '#8b5cf6'
  }
])

const trend = computed(() => data.trend || [])

const maxOrders = computed(() =>
  Math.max(1, ...trend.value.map((item) => Number(item.orders || 0)))
)

function barHeight(value) {
  return Math.max(4, (Number(value || 0) / maxOrders.value) * 100)
}

const refundedOrders = computed(() => Number(data.refundedOrders || 0))

const totalOrders = computed(() => {
  const a = Number(data.unpaidOrders || 0)
  const b = Number(data.paidOrders || 0)
  return a + b + refundedOrders.value + otherOrders.value
})

const otherOrders = computed(
  () => Number(data.cancelledOrders || 0) + Number(data.finishedOrders || 0)
)

const donutGradient = computed(() => {
  const total = totalOrders.value || 1
  const p1 = (Number(data.unpaidOrders || 0) / total) * 100
  const p2 = p1 + (Number(data.paidOrders || 0) / total) * 100
  const p3 = p2 + (refundedOrders.value / total) * 100
  return `conic-gradient(from -90deg, #f59e0b 0 ${p1}%, #22c55e ${p1}% ${p2}%, #f97316 ${p2}% ${p3}%, #e2e8f0 ${p3}% 100%)`
})

function statusText(status) {
  return { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已完成', 4: '已退款' }[status] || '未知'
}

function statusType(status) {
  return { 0: 'warning', 1: 'success', 2: 'info', 3: 'success', 4: 'warning' }[status] || 'info'
}

async function load() {
  loading.value = true
  try {
    const result = await getDashboard()
    Object.assign(data, result)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  min-height: 100px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  display: flex;
  gap: 14px;
  padding: 20px;
  background: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  font-size: 21px;
  color: var(--c);
  background: color-mix(in srgb, var(--c) 11%, white);
  flex-shrink: 0;
}

.stat-body {
  min-width: 0;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: var(--ink-3);
}

.stat-value {
  display: block;
  font-size: 27px;
  letter-spacing: -0.8px;
  margin: 5px 0 4px;
  font-variant-numeric: tabular-nums;
}

.stat-sub {
  font-style: normal;
  font-size: 11px;
  color: var(--ink-3);
}

.stat-glow {
  position: absolute;
  right: -36px;
  bottom: -46px;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: var(--c);
  opacity: 0.06;
}

.middle {
  display: grid;
  grid-template-columns: minmax(0, 1.9fr) minmax(300px, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.chart-card,
.donut-card,
.recent {
  padding: 18px 20px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.card-head h2 {
  font-size: 16px;
  margin: 0 0 4px;
}

.card-head p {
  margin: 0;
  font-size: 12px;
  color: var(--ink-3);
}

.legend {
  font-size: 11px;
  color: var(--ink-3);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend i {
  width: 9px;
  height: 9px;
  border-radius: 3px;
  background: linear-gradient(180deg, #60a5fa, #2563eb);
}

.chart {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  height: 190px;
  padding-top: 10px;
}

.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.bar-value {
  font-size: 11px;
  color: var(--ink-3);
  height: 16px;
}

.bar-track {
  flex: 1;
  width: 100%;
  max-width: 34px;
  display: flex;
  align-items: flex-end;
  background: #f6f8fb;
  border-radius: 8px;
  overflow: hidden;
}

.bar {
  width: 100%;
  border-radius: 8px 8px 0 0;
  background: linear-gradient(180deg, #60a5fa, #2563eb);
  transition: height 0.5s;
  min-height: 4px;
}

.bar-col:nth-child(7) .bar {
  background: linear-gradient(180deg, #fbbf24, #f59e0b);
}

.bar-label {
  font-size: 11px;
  color: var(--ink-3);
  margin-top: 8px;
}

.donut-wrap {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 8px 0 16px;
}

.donut {
  width: 148px;
  height: 148px;
  border-radius: 50%;
  position: relative;
  flex-shrink: 0;
}

.donut-hole {
  position: absolute;
  inset: 20px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.donut-hole strong {
  font-size: 26px;
  letter-spacing: -1px;
}

.donut-hole span {
  font-size: 11px;
  color: var(--ink-3);
  margin-top: 2px;
}

.legend-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.legend-list div {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--ink-2);
}

.legend-list i {
  width: 9px;
  height: 9px;
  border-radius: 50%;
}

.legend-list i.amber {
  background: #f59e0b;
}

.legend-list i.green {
  background: #22c55e;
}

.legend-list i.gray {
  background: #e2e8f0;
}

.legend-list i.orange {
  background: #f97316;
}

.legend-list b {
  margin-left: auto;
  font-size: 14px;
  color: var(--ink);
}

.recent {
  margin-top: 16px;
}

.recent .table-wrap {
  padding: 0;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-cell .thumb {
  width: 38px;
  height: 38px;
}

.product-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.refund-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: 1px solid #fde68a;
  background: #fffbeb;
  color: #b45309;
  font-size: 12px;
  font-weight: 600;
  padding: 8px 13px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}

.refund-chip:hover {
  background: #fef3c7;
}

.refund-chip .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.14);
}

@media (max-width: 1100px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .middle {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
  .donut-wrap {
    flex-direction: column;
  }
}
</style>
