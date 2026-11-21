<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>商品管理</h1>
        <p>维护商品资料、库存与上下架状态</p>
      </div>
      <el-button v-if="canEdit" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增商品
      </el-button>
    </div>

    <section class="card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索商品名称"
          clearable
          style="width: 220px"
          prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 150px" @change="search">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="search">
          <el-option label="已上架" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>
        <el-button @click="search">查询</el-button>
        <el-button text @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted total-tip">共 {{ total }} 件商品</span>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" style="width: 100%">
          <el-table-column label="商品" min-width="280">
            <template #default="{ row }">
              <div class="product-cell">
                <img class="thumb" :src="row.cover" alt="" />
                <div>
                  <div class="cell-title">{{ row.name }}</div>
                  <div class="cell-sub">{{ row.subtitle }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="categoryName" label="分类" width="110" />
          <el-table-column label="价格" width="130">
            <template #default="{ row }">
              <span class="price">¥{{ formatPrice(row.price) }}</span>
              <div class="cell-sub" v-if="row.originalPrice">¥{{ formatPrice(row.originalPrice) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="stock" label="库存" width="90" />
          <el-table-column prop="sales" label="销量" width="80" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                :disabled="!canEdit"
                inline-prompt
                active-text="上架"
                inactive-text="下架"
                @change="(val) => toggleStatus(row, val)"
              />
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="165">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column v-if="canEdit" label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="商品名称" prop="name">
            <el-input v-model="form.name" maxlength="100" placeholder="请输入商品名称" />
          </el-form-item>
          <el-form-item label="所属分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="商品副标题">
          <el-input v-model="form.subtitle" maxlength="200" placeholder="一句话卖点" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="售价 ¥" prop="price">
            <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="划线价 ¥">
            <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="库存" prop="stock">
            <el-input-number v-model="form.stock" :min="0" :step="10" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">上架</el-radio>
              <el-radio :value="0">下架</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>

        <el-form-item label="商品封面">
          <el-upload
            class="cover-upload"
            :show-file-list="false"
            :http-request="uploadCover"
            accept=".jpg,.jpeg,.png,.gif,.webp,.svg"
          >
            <div class="cover-box">
              <img v-if="form.cover" :src="form.cover" alt="cover" />
              <div v-else class="cover-empty">
                <el-icon><Plus /></el-icon>
                <span>上传封面</span>
              </div>
            </div>
            <div class="upload-tip">支持 jpg/png/gif/webp/svg，最大 10MB</div>
          </el-upload>
        </el-form-item>

        <el-form-item label="商品详情">
          <el-input v-model="form.description" type="textarea" :rows="5" placeholder="商品详情描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createProduct,
  deleteProduct,
  getProducts,
  getPublicCategories,
  updateProduct,
  updateProductStatus,
  uploadImage
} from '@/api'
import { useAdminStore } from '@/stores/admin'
import { formatPrice, formatTime } from '@/utils/format'

const adminStore = useAdminStore()
const canEdit = computed(() => adminStore.hasPerm('product:edit'))

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const categories = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, keyword: '', categoryId: null, status: null })

const emptyForm = () => ({
  id: null,
  name: '',
  subtitle: '',
  categoryId: null,
  price: 0,
  originalPrice: 0,
  stock: 0,
  status: 1,
  cover: '',
  description: ''
})
const form = reactive(emptyForm())

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getProducts({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || undefined,
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
  Object.assign(query, { page: 1, keyword: '', categoryId: null, status: null })
  fetchList()
}

function openCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    subtitle: row.subtitle,
    categoryId: row.categoryId,
    price: Number(row.price),
    originalPrice: row.originalPrice ? Number(row.originalPrice) : 0,
    stock: row.stock,
    status: row.status,
    cover: row.cover,
    description: row.description
  })
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      name: form.name,
      subtitle: form.subtitle,
      categoryId: form.categoryId,
      price: form.price,
      originalPrice: form.originalPrice || null,
      stock: form.stock,
      status: form.status,
      cover: form.cover,
      images: form.cover,
      description: form.description
    }
    if (form.id) {
      await updateProduct(form.id, payload)
    } else {
      await createProduct(payload)
    }
    ElMessage.success(form.id ? '商品已更新' : '商品已创建')
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row, value) {
  const status = value ? 1 : 0
  try {
    await updateProductStatus(row.id, status)
    row.status = status
    ElMessage.success(value ? '已上架' : '已下架')
  } catch (e) {
    fetchList()
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？`, '删除商品', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await deleteProduct(row.id)
  ElMessage.success('已删除')
  fetchList()
}

async function uploadCover({ file }) {
  const data = await uploadImage(file)
  form.cover = data.url
  ElMessage.success('封面已上传')
}

onMounted(async () => {
  categories.value = (await getPublicCategories()) || []
  fetchList()
})
</script>

<style scoped>
.total-tip {
  font-size: 12px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.cover-upload :deep(.el-upload) {
  display: block;
}

.cover-box {
  width: 132px;
  height: 132px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
  display: grid;
  place-items: center;
  transition: border-color 0.2s;
}

.cover-box:hover {
  border-color: var(--brand);
}

.cover-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--ink-3);
  font-size: 12px;
}

.cover-empty .el-icon {
  font-size: 22px;
}

.upload-tip {
  font-size: 11px;
  color: var(--ink-3);
  margin-top: 8px;
}
</style>
