<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>退款审核</h1>
        <p>用户申请 → 管理员审核 → 通过后订单变为已退款（演示只退金额，不回补库存）</p>
      </div>
      <el-button @click="loadAll"><el-icon><Refresh /></el-icon> 刷新</el-button>
    </div>

    <div class="summary-grid">
      <div class="summary-card pending">
        <span class="label">待审核</span>
        <strong>{{ summary.pending || 0 }}</strong>
        <em>需要及时处理</em>
      </div>
      <div class="summary-card approved">
        <span class="label">已通过</span>
        <strong>{{ summary.approved || 0 }}</strong>
        <em>已原路退回</em>
      </div>
      <div class="summary-card rejected">
        <span class="label">已拒绝</span>
        <strong>{{ summary.rejected || 0 }}</strong>
        <em>用户可重新申请</em>
      </div>
      <div class="summary-card total">
        <span class="label">退款单总数</span>
        <strong>{{ summary.total || 0 }}</strong>
        <em>含全部状态</em>
      </div>
    </div>

    <section class="card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="退款单号 / 订单号 / 商品"
          clearable
          style="width: 260px"
          prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="search">
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已拒绝" :value="2" />
        </el-select>
        <el-button @click="search">查询</el-button>
        <el-button text @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted total-tip">共 {{ total }} 条退款单</span>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" style="width: 100%">
          <el-table-column label="退款单号" min-width="205">
            <template #default="{ row }">
              <div class="cell-title mono">{{ row.refundNo }}</div>
              <div class="cell-sub">{{ formatTime(row.createTime) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="订单 / 商品" min-width="250">
            <template #default="{ row }">
              <div class="order-cell">
                <img class="thumb" :src="row.productCover" alt="" />
                <div>
                  <div class="cell-title">{{ row.productName }}</div>
                  <div class="cell-sub mono">{{ row.orderNo }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="用户ID" width="95">
            <template #default="{ row }">#{{ row.userId }}</template>
          </el-table-column>
          <el-table-column label="退款金额" width="120">
            <template #default="{ row }">
              <span class="price">¥{{ formatPrice(row.refundAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="退款原因" min-width="170" show-overflow-tooltip>
            <template #default="{ row }">
              <div>{{ row.reason }}</div>
              <div class="cell-sub" v-if="row.description">{{ row.description }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" effect="light" round size="small">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" width="165">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <template v-if="row.status === 0 && canAudit">
                <el-button link type="success" @click="openAudit(row, 'approve')">通过</el-button>
                <el-button link type="danger" @click="openAudit(row, 'reject')">拒绝</el-button>
              </template>
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

    <el-dialog v-model="auditVisible" :title="auditMode === 'approve' ? '通过退款' : '拒绝退款'" width="480px">
      <div v-if="current" class="audit-head">
        <div>
          <strong>{{ current.refundNo }}</strong>
          <p class="muted">订单 {{ current.orderNo }}</p>
        </div>
        <span class="price">¥{{ formatPrice(current.refundAmount) }}</span>
      </div>
      <el-form :model="auditForm" label-position="top">
        <el-form-item :label="auditMode === 'approve' ? '审核备注（选填）' : '拒绝原因（必填）'">
          <el-input
            v-model="auditForm.remark"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            :placeholder="auditMode === 'approve' ? '如：同意退款，原路退回' : '请说明拒绝原因，用户可以看到'"
          />
        </el-form-item>
      </el-form>
      <div class="audit-tip">
        <el-icon><InfoFilled /></el-icon>
        {{ auditMode === 'approve'
          ? '通过后订单状态变为“已退款”，演示环境不退库存。'
          : '拒绝后订单保持“已支付”，用户可以修改原因后重新申请。' }}
      </div>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button
          :type="auditMode === 'approve' ? 'primary' : 'danger'"
          :loading="submitting"
          @click="submitAudit"
        >
          {{ auditMode === 'approve' ? '确认通过' : '确认拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="退款单详情" size="480px">
      <div v-if="current" class="detail">
        <div class="detail-status" :class="statusClass(current.status)">
          <strong>{{ statusText(current.status) }}</strong>
          <span>{{ current.refundNo }}</span>
        </div>

        <div class="product-box">
          <img :src="current.productCover" alt="" />
          <div>
            <h3>{{ current.productName }}</h3>
            <p class="muted">订单号 {{ current.orderNo }}</p>
            <span class="price">¥{{ formatPrice(current.refundAmount) }}</span>
          </div>
        </div>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户 ID">#{{ current.userId }}</el-descriptions-item>
          <el-descriptions-item label="退款原因">{{ current.reason }}</el-descriptions-item>
          <el-descriptions-item label="补充说明">{{ current.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatTime(current.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ current.auditName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">
            {{ current.auditTime ? formatTime(current.auditTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="审核备注">{{ current.auditRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="退款时间">
            {{ current.refundTime ? formatTime(current.refundTime) : '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="current.status === 0 && canAudit" class="detail-actions">
          <el-button type="danger" plain @click="openAudit(current, 'reject')">拒绝退款</el-button>
          <el-button type="primary" @click="openAudit(current, 'approve')">通过退款</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  approveRefund,
  getRefundSummary,
  getRefunds,
  rejectRefund
} from '@/api'
import { useAdminStore } from '@/stores/admin'
import { formatPrice, formatTime } from '@/utils/format'

const route = useRoute()
const adminStore = useAdminStore()
const canAudit = computed(() => adminStore.hasPerm('refund:audit'))

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const summary = ref({})
const auditVisible = ref(false)
const detailVisible = ref(false)
const auditMode = ref('approve')
const current = ref(null)
const auditForm = reactive({ remark: '' })

const query = reactive({
  page: 1,
  size: 10,
  keyword: route.query.keyword || '',
  status: route.query.status !== undefined ? Number(route.query.status) : null
})

async function fetchList() {
  loading.value = true
  try {
    const data = await getRefunds({
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

async function fetchSummary() {
  summary.value = (await getRefundSummary()) || {}
}

function loadAll() {
  fetchList()
  fetchSummary()
}

function search() {
  query.page = 1
  fetchList()
}

function reset() {
  Object.assign(query, { page: 1, keyword: '', status: null })
  loadAll()
}

function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

function openAudit(row, mode) {
  current.value = row
  auditMode.value = mode
  auditForm.remark = mode === 'approve' ? '同意退款，原路退回' : ''
  detailVisible.value = false
  auditVisible.value = true
}

async function submitAudit() {
  if (auditMode.value === 'reject' && !auditForm.remark.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  submitting.value = true
  try {
    if (auditMode.value === 'approve') {
      await approveRefund(current.value.id, { remark: auditForm.remark })
      ElMessage.success('退款已通过，订单状态已更新')
    } else {
      await rejectRefund(current.value.id, { remark: auditForm.remark })
      ElMessage.success('退款已拒绝，用户可重新申请')
    }
    auditVisible.value = false
    loadAll()
  } finally {
    submitting.value = false
  }
}

function statusText(status) {
  return { 0: '待审核', 1: '已通过', 2: '已拒绝' }[status] || '未知'
}

function statusType(status) {
  return { 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info'
}

function statusClass(status) {
  return { 0: 'pending', 1: 'approved', 2: 'rejected' }[status] || ''
}

onMounted(loadAll)
</script>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.summary-card {
  position: relative;
  overflow: hidden;
  background: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 17px 20px;
}

.summary-card .label {
  font-size: 12px;
  color: var(--ink-3);
}

.summary-card strong {
  display: block;
  font-size: 27px;
  letter-spacing: -0.8px;
  margin: 6px 0 4px;
  font-variant-numeric: tabular-nums;
}

.summary-card em {
  font-style: normal;
  font-size: 11px;
  color: var(--ink-3);
}

.summary-card.pending strong {
  color: #d97706;
}

.summary-card.approved strong {
  color: #059669;
}

.summary-card.rejected strong {
  color: #dc2626;
}

.summary-card::after {
  content: '';
  position: absolute;
  right: -28px;
  bottom: -40px;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.05;
}

.total-tip {
  font-size: 12px;
}

.mono {
  font-family: Consolas, "JetBrains Mono", monospace;
  font-size: 12px;
}

.order-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.order-cell .thumb {
  width: 40px;
  height: 40px;
}

.audit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: #f8fafc;
  border-radius: 12px;
  padding: 13px 15px;
  margin-bottom: 16px;
}

.audit-head strong {
  font-size: 13px;
}

.audit-head p {
  margin: 4px 0 0;
  font-size: 11px;
}

.audit-tip {
  display: flex;
  gap: 7px;
  font-size: 12px;
  color: var(--ink-3);
  background: #f8fafc;
  border-radius: 10px;
  padding: 10px 12px;
  line-height: 1.6;
}

.audit-tip .el-icon {
  color: #60a5fa;
  margin-top: 2px;
  flex-shrink: 0;
}

.detail-status {
  border-radius: 14px;
  padding: 18px 20px;
  color: #fff;
  margin-bottom: 18px;
  background: linear-gradient(135deg, #334155, #475569);
}

.detail-status.pending {
  background: linear-gradient(135deg, #b45309, #f59e0b);
}

.detail-status.approved {
  background: linear-gradient(135deg, #047857, #10b981);
}

.detail-status.rejected {
  background: linear-gradient(135deg, #991b1b, #ef4444);
}

.detail-status strong {
  display: block;
  font-size: 19px;
  margin-bottom: 6px;
}

.detail-status span {
  font-size: 12px;
  opacity: 0.72;
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

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

@media (max-width: 1000px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
