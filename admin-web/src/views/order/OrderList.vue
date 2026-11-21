<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>订单管理</h1>
        <p>查询秒杀订单、取消待支付订单、审核/直接退款、导出 Excel</p>
      </div>
      <el-button v-if="canExport" :loading="exporting" @click="exportExcel">
        <el-icon><Download /></el-icon> 导出 Excel
      </el-button>
    </div>

    <section class="card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="订单号 / 商品名"
          clearable
          style="width: 220px"
          prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="search">
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已取消" :value="2" />
          <el-option label="已完成" :value="3" />
          <el-option label="已退款" :value="4" />
        </el-select>
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
          @change="search"
        />
        <el-button @click="search">查询</el-button>
        <el-button text @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted total-tip">共 {{ total }} 笔订单</span>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" style="width: 100%">
          <el-table-column prop="orderNo" label="订单号" min-width="200">
            <template #default="{ row }">
              <span class="order-no">{{ row.orderNo }}</span>
            </template>
          </el-table-column>
          <el-table-column label="商品" min-width="190">
            <template #default="{ row }">
              <div class="product-cell">
                <img class="thumb" :src="row.productCover" alt="" />
                <div>
                  <div class="cell-title">{{ row.productName }}</div>
                  <div class="cell-sub">× {{ row.quantity }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="用户ID" width="80">
            <template #default="{ row }">#{{ row.userId }}</template>
          </el-table-column>
          <el-table-column label="金额" width="105">
            <template #default="{ row }">
              <span class="price">¥{{ formatPrice(row.seckillPrice * row.quantity) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" effect="light" round size="small">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="退款" width="105">
            <template #default="{ row }">
              <el-tag
                v-if="row.refundStatus === 1 || row.refundStatus === 3"
                :type="refundTagType(row.refundStatus)"
                effect="plain"
                round
                size="small"
              >
                {{ refundStatusText(row.refundStatus) }}
              </el-tag>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="155">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button
                v-if="row.status === 0 && canEdit"
                link
                type="danger"
                @click="cancel(row)"
              >取消</el-button>
              <el-button
                v-if="row.refundStatus === 1 && canAudit"
                link
                type="warning"
                @click="goAudit(row)"
              >去审核</el-button>
              <el-button
                v-else-if="canRefund(row) && canAudit"
                link
                type="danger"
                @click="openRefund(row)"
              >退款</el-button>
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

    <el-drawer v-model="detailVisible" title="订单详情" size="480px">
      <div v-if="current" class="detail">
        <div class="detail-status" :class="statusClass(current.status)">
          <strong>{{ statusText(current.status) }}</strong>
          <span>{{ current.orderNo }}</span>
        </div>

        <div class="product-box">
          <img :src="current.productCover" alt="" />
          <div>
            <h3>{{ current.productName }}</h3>
            <p class="muted">秒杀价 ¥{{ formatPrice(current.seckillPrice) }} × {{ current.quantity }}</p>
            <span class="price">¥{{ formatPrice(current.seckillPrice * current.quantity) }}</span>
          </div>
        </div>

        <el-descriptions :column="1" border class="desc">
          <el-descriptions-item label="订单编号">{{ current.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="用户 ID">{{ current.userId }}</el-descriptions-item>
          <el-descriptions-item label="活动 ID">{{ current.activityId }}</el-descriptions-item>
          <el-descriptions-item label="商品 ID">{{ current.productId }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatTime(current.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">
            {{ current.payTime ? formatTime(current.payTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="取消时间">
            {{ current.cancelTime ? formatTime(current.cancelTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="退款状态">
            {{ refundStatusText(current.refundStatus) }}
          </el-descriptions-item>
          <el-descriptions-item label="退款时间">
            {{ current.refundTime ? formatTime(current.refundTime) : '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-button
          v-if="current.status === 0 && canEdit"
          type="danger"
          plain
          style="width: 100%; margin-top: 18px"
          @click="cancel(current, true)"
        >
          取消该订单并回补库存
        </el-button>
        <div v-if="canAudit" class="drawer-actions">
          <el-button
            v-if="current.refundStatus === 1"
            type="warning"
            plain
            @click="goAudit(current)"
          >去退款审核</el-button>
          <el-button
            v-else-if="canRefund(current)"
            type="danger"
            plain
            @click="openRefund(current)"
          >直接退款</el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="refundVisible" title="订单退款" width="480px">
      <div v-if="refundTarget" class="refund-head">
        <img :src="refundTarget.productCover" alt="" />
        <div>
          <strong>{{ refundTarget.productName }}</strong>
          <p class="muted">订单 {{ refundTarget.orderNo }}</p>
          <span class="price">¥{{ formatPrice(refundTarget.seckillPrice * refundTarget.quantity) }}</span>
        </div>
      </div>
      <el-form :model="refundForm" label-position="top">
        <el-form-item label="退款原因" required>
          <el-input v-model="refundForm.reason" maxlength="100" placeholder="请填写退款原因" />
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input
            v-model="refundForm.description"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="选填"
          />
        </el-form-item>
      </el-form>
      <div class="refund-tip">
        <el-icon><InfoFilled /></el-icon>
        直接退款会立即把订单置为“已退款”；演示环境只退金额，不回补秒杀库存。
      </div>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="danger" :loading="refunding" @click="submitRefund">确认退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, exportOrders, getOrders, refundOrder } from '@/api'
import { useAdminStore } from '@/stores/admin'
import { formatPrice, formatTime } from '@/utils/format'

const router = useRouter()
const adminStore = useAdminStore()
const canEdit = computed(() => adminStore.hasPerm('order:edit'))
const canExport = computed(() => adminStore.hasPerm('order:export'))
const canAudit = computed(() => adminStore.hasPerm('refund:audit'))

const loading = ref(false)
const exporting = ref(false)
const list = ref([])
const total = ref(0)
const detailVisible = ref(false)
const current = ref(null)
const refundVisible = ref(false)
const refunding = ref(false)
const refundTarget = ref(null)
const refundForm = reactive({ reason: '管理员直接退款', description: '' })

const query = reactive({ page: 1, size: 10, keyword: '', status: null, dateRange: [] })

function params() {
  return {
    page: query.page,
    size: query.size,
    keyword: query.keyword || undefined,
    status: query.status === null || query.status === '' ? undefined : query.status,
    startDate: query.dateRange?.[0] || undefined,
    endDate: query.dateRange?.[1] || undefined
  }
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getOrders(params())
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
  Object.assign(query, { page: 1, keyword: '', status: null, dateRange: [] })
  fetchList()
}

function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

function canRefund(row) {
  return [1, 3].includes(row.status) && [0, 3].includes(row.refundStatus ?? 0)
}

function openRefund(row) {
  refundTarget.value = row
  refundForm.reason = '管理员直接退款'
  refundForm.description = ''
  detailVisible.value = false
  refundVisible.value = true
}

async function submitRefund() {
  if (!refundForm.reason.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }
  refunding.value = true
  try {
    await refundOrder(refundTarget.value.id, {
      reason: refundForm.reason.trim(),
      description: refundForm.description.trim()
    })
    ElMessage.success('退款已完成，订单状态已更新')
    refundVisible.value = false
    fetchList()
  } finally {
    refunding.value = false
  }
}

function goAudit(row) {
  detailVisible.value = false
  router.push({ path: '/refunds', query: { keyword: row.orderNo } })
}

async function cancel(row, fromDrawer = false) {
  try {
    await ElMessageBox.confirm('取消后数据库与 Redis 库存都会回补，确定继续吗？', '取消订单', {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await cancelOrder(row.id)
  ElMessage.success('订单已取消，库存已回补')
  if (fromDrawer) {
    detailVisible.value = false
  }
  fetchList()
}

async function exportExcel() {
  exporting.value = true
  try {
    const blob = await exportOrders(params())
    const url = URL.createObjectURL(new Blob([blob]))
    const link = document.createElement('a')
    const date = new Date().toISOString().slice(0, 10)
    link.href = url
    link.download = `秒杀订单-${date}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

function statusText(status) {
  return { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已完成', 4: '已退款' }[status] || '未知'
}

function statusType(status) {
  return { 0: 'warning', 1: 'success', 2: 'info', 3: 'success', 4: 'warning' }[status] || 'info'
}

function statusClass(status) {
  return { 0: 'warn', 1: 'ok', 2: 'cancel', 3: 'ok', 4: 'refunded' }[status] || ''
}

function refundStatusText(status) {
  return { 0: '无退款', 1: '退款审核中', 2: '已退款', 3: '退款被拒' }[status || 0] || '未知'
}

function refundTagType(status) {
  return { 1: 'warning', 2: 'success', 3: 'danger' }[status] || 'info'
}

onMounted(fetchList)
</script>

<style scoped>
.total-tip {
  font-size: 12px;
}

.order-no {
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 12px;
  color: var(--ink-2);
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-cell .thumb {
  width: 40px;
  height: 40px;
}

.detail-status {
  border-radius: 14px;
  padding: 18px 20px;
  color: #fff;
  background: linear-gradient(135deg, #334155, #475569);
  margin-bottom: 18px;
}

.detail-status.warn {
  background: linear-gradient(135deg, #b45309, #f59e0b);
}

.detail-status.ok {
  background: linear-gradient(135deg, #047857, #10b981);
}

.detail-status.cancel {
  background: linear-gradient(135deg, #475569, #94a3b8);
}

.detail-status.refunded {
  background: linear-gradient(135deg, #7c2d12, #ea580c);
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.refund-head {
  display: flex;
  gap: 13px;
  background: #f8fafc;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 16px;
}

.refund-head img {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  object-fit: cover;
}

.refund-head strong {
  display: block;
  font-size: 14px;
  margin-bottom: 4px;
}

.refund-head p {
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
  color: #60a5fa;
  margin-top: 2px;
  flex-shrink: 0;
}

.detail-status strong {
  display: block;
  font-size: 19px;
  margin-bottom: 6px;
}

.detail-status span {
  font-size: 12px;
  opacity: 0.7;
  font-family: Consolas, monospace;
}

.product-box {
  display: flex;
  gap: 14px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  margin-bottom: 18px;
}

.product-box img {
  width: 76px;
  height: 76px;
  border-radius: 10px;
  object-fit: cover;
}

.product-box h3 {
  margin: 0 0 6px;
  font-size: 14px;
}

.product-box p {
  margin: 0 0 8px;
  font-size: 12px;
}

.desc :deep(.el-descriptions__label) {
  width: 100px;
}
</style>
