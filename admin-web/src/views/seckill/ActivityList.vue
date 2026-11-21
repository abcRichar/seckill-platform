<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>秒杀活动</h1>
        <p>上架后自动生成隐藏链接并把 DB 库存预热到 Redis</p>
      </div>
      <el-button v-if="canEdit" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增活动
      </el-button>
    </div>

    <section class="card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索活动标题"
          clearable
          style="width: 220px"
          prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="search">
          <el-option label="草稿" :value="0" />
          <el-option label="已上架" :value="1" />
          <el-option label="已结束" :value="2" />
        </el-select>
        <el-button @click="search">查询</el-button>
        <el-button text @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted total-tip">共 {{ total }} 场活动</span>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" style="width: 100%">
          <el-table-column label="活动 / 商品" min-width="260">
            <template #default="{ row }">
              <div class="activity-cell">
                <img class="thumb" :src="row.productCover" alt="" />
                <div>
                  <div class="cell-title">{{ row.title }}</div>
                  <div class="cell-sub">{{ row.productName }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="价格" width="130">
            <template #default="{ row }">
              <span class="price">¥{{ formatPrice(row.seckillPrice) }}</span>
              <div class="cell-sub">日常 ¥{{ formatPrice(row.originalPrice) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="库存" width="120">
            <template #default="{ row }">
              <div>{{ row.stock }} / {{ row.initialStock }}</div>
              <el-progress
                :percentage="stockPercent(row)"
                :stroke-width="5"
                :show-text="false"
                style="margin-top: 6px; width: 80px"
              />
            </template>
          </el-table-column>
          <el-table-column label="活动时间" width="200">
            <template #default="{ row }">
              <div class="time-line">{{ formatTime(row.startTime) }}</div>
              <div class="time-line muted">至 {{ formatTime(row.endTime) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" effect="light" round size="small">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="canEdit" label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button v-if="canPublish(row)" link type="primary" @click="publish(row)">上架</el-button>
              <el-button v-else-if="row.status === 1" link type="warning" @click="stop(row)">下架</el-button>
              <el-button link type="primary" :disabled="row.status === 1" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" :disabled="row.status === 1" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑活动' : '新增活动'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="选择商品" prop="productId">
          <el-select v-model="form.productId" filterable placeholder="请选择上架商品" style="width: 100%">
            <el-option
              v-for="product in products"
              :key="product.id"
              :label="`${product.name}（库存 ${product.stock}）`"
              :value="product.id"
            >
              <div class="product-option">
                <img :src="product.cover" alt="" />
                <span>{{ product.name }}</span>
                <em>¥{{ formatPrice(product.price) }}</em>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="form.title" maxlength="100" placeholder="如：旗舰手机限时秒杀" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="秒杀价 ¥" prop="seckillPrice">
            <el-input-number v-model="form.seckillPrice" :min="0.01" :precision="2" :step="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="秒杀库存" prop="stock">
            <el-input-number v-model="form.stock" :min="1" :step="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="每人限购">
            <el-input-number v-model="form.limitPerUser" :min="1" :max="1" style="width: 100%" disabled />
            <div class="tip">当前秒杀规则为一人一单（DB 唯一索引兜底）</div>
          </el-form-item>
        </div>
        <el-form-item label="活动时间" prop="timeRange">
          <el-date-picker
            v-model="form.timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <div class="notice">
          <el-icon><InfoFilled /></el-icon>
          活动保存后为草稿状态，点击「上架」才会初始化 Redis 库存并开放抢购。
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createActivity,
  deleteActivity,
  getActivities,
  getProducts,
  publishActivity,
  stopActivity,
  updateActivity
} from '@/api'
import { useAdminStore } from '@/stores/admin'
import { formatPrice, formatTime } from '@/utils/format'

const adminStore = useAdminStore()
const canEdit = computed(() => adminStore.hasPerm('seckill:edit'))

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const products = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, keyword: '', status: null })

const emptyForm = () => ({
  id: null,
  productId: null,
  title: '',
  seckillPrice: 0,
  stock: 1,
  limitPerUser: 1,
  timeRange: []
})
const form = reactive(emptyForm())

const rules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  seckillPrice: [{ required: true, message: '请输入秒杀价', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  timeRange: [
    {
      validator: (rule, value, callback) =>
        value && value.length === 2 ? callback() : callback(new Error('请选择活动时间')),
      trigger: 'change'
    }
  ]
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getActivities({
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

function search() {
  query.page = 1
  fetchList()
}

function reset() {
  Object.assign(query, { page: 1, keyword: '', status: null })
  fetchList()
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row) {
  if (row.status === 1) {
    ElMessage.warning('活动进行中，请先下架再编辑')
    return
  }
  Object.assign(form, {
    id: row.id,
    productId: row.productId,
    title: row.title,
    seckillPrice: Number(row.seckillPrice),
    stock: row.stock,
    limitPerUser: row.limitPerUser || 1,
    timeRange: [row.startTime, row.endTime]
  })
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      productId: form.productId,
      title: form.title,
      seckillPrice: form.seckillPrice,
      stock: form.stock,
      limitPerUser: 1,
      startTime: form.timeRange[0],
      endTime: form.timeRange[1]
    }
    if (form.id) {
      await updateActivity(form.id, payload)
    } else {
      await createActivity(payload)
    }
    ElMessage.success('活动已保存为草稿')
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

async function publish(row) {
  try {
    await ElMessageBox.confirm(
      `确认上架「${row.title}」？上架后将以剩余库存初始化 Redis，并生成隐藏秒杀链接。`,
      '上架活动',
      { confirmButtonText: '确认上架', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  await publishActivity(row.id)
  ElMessage.success('已上架，Redis 库存已初始化')
  fetchList()
}

async function stop(row) {
  try {
    await ElMessageBox.confirm('下架后库存会从 Redis 清除，已下单用户不受影响。', '下架活动', {
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await stopActivity(row.id)
  ElMessage.success('活动已下架')
  fetchList()
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」吗？`, '删除活动', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await deleteActivity(row.id)
  ElMessage.success('已删除')
  fetchList()
}

function stockPercent(row) {
  if (!row.initialStock) return 0
  return Math.min(100, Math.round(((row.initialStock - row.stock) / row.initialStock) * 100))
}

function canPublish(row) {
  if (row.status === 1) return false
  const end = new Date(String(row.endTime || '').replace(/-/g, '/'))
  return !Number.isNaN(end.getTime()) && end > new Date()
}

function statusText(status) {
  return { 0: '草稿', 1: '已上架', 2: '已结束' }[status] || '未知'
}

function statusType(status) {
  return { 0: 'info', 1: 'success', 2: 'warning' }[status] || 'info'
}

onMounted(async () => {
  fetchList()
  const data = await getProducts({ page: 1, size: 200, status: 1 })
  products.value = data?.records || []
})
</script>

<style scoped>
.total-tip {
  font-size: 12px;
}

.activity-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.time-line {
  font-size: 12px;
  line-height: 1.7;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 0 14px;
}

.tip {
  font-size: 11px;
  color: var(--ink-3);
  margin-top: 4px;
  line-height: 1.5;
}

.product-option {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-option img {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  object-fit: cover;
}

.product-option em {
  margin-left: auto;
  font-style: normal;
  color: var(--ink-3);
  font-size: 12px;
}

.notice {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  font-size: 12px;
  color: #64748b;
  background: #f8fafc;
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 11px 13px;
  line-height: 1.6;
}

.notice .el-icon {
  color: #60a5fa;
  margin-top: 2px;
}
</style>
