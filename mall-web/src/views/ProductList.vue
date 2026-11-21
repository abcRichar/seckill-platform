<template>
  <div class="page">
    <div class="container">
      <div class="page-head">
        <div>
          <h1>{{ keyword ? `“${keyword}” 的搜索结果` : '全部商品' }}</h1>
          <p class="muted">共 {{ total }} 件好物 <span v-if="keyword">· 关键词：{{ keyword }}</span></p>
        </div>
        <div class="sort">
          <el-select v-model="sort" style="width: 150px" @change="reload">
            <el-option label="销量优先" value="sales" />
            <el-option label="价格从低到高" value="price_asc" />
            <el-option label="价格从高到低" value="price_desc" />
            <el-option label="最新上架" value="new" />
          </el-select>
        </div>
      </div>

      <div class="filters card">
        <button class="chip" :class="{ active: !categoryId }" @click="selectCategory(null)">全部</button>
        <button
          v-for="cat in categories"
          :key="cat.id"
          class="chip"
          :class="{ active: categoryId === cat.id }"
          @click="selectCategory(cat.id)"
        >
          {{ cat.name }}
        </button>
      </div>

      <div v-loading="loading" class="grid">
        <ProductCard v-for="item in products" :key="item.id" :product="item" />
      </div>
      <EmptyBlock v-if="!loading && products.length === 0" text="没有找到相关商品，换个关键词试试" icon="🔍">
        <el-button round @click="clearFilter">清除筛选</el-button>
      </EmptyBlock>

      <el-pagination
        v-if="total > size"
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="fetchList"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCategories, getProducts } from '@/api'
import ProductCard from '@/components/ProductCard.vue'
import EmptyBlock from '@/components/EmptyBlock.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const products = ref([])
const categories = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)
const sort = ref('sales')
const categoryId = ref(route.query.categoryId ? Number(route.query.categoryId) : null)
const keyword = ref(route.query.keyword || '')

async function fetchList() {
  loading.value = true
  try {
    const data = await getProducts({
      page: page.value,
      size: size.value,
      categoryId: categoryId.value || undefined,
      keyword: keyword.value || undefined,
      sort: sort.value
    })
    products.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  fetchList()
}

function selectCategory(id) {
  categoryId.value = id
  router.replace({ path: '/products', query: { ...route.query, categoryId: id || undefined } })
}

function clearFilter() {
  keyword.value = ''
  categoryId.value = null
  router.replace({ path: '/products' })
  reload()
}

watch(
  () => route.query.keyword,
  (value) => {
    keyword.value = value || ''
    reload()
  }
)

watch(
  () => route.query.categoryId,
  (value) => {
    categoryId.value = value ? Number(value) : null
    reload()
  }
)

onMounted(async () => {
  categories.value = (await getCategories()) || []
  fetchList()
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.page-head h1 {
  font-size: 26px;
  margin: 0 0 6px;
  letter-spacing: -0.5px;
}

.page-head p {
  margin: 0;
  font-size: 13px;
}

.filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 14px 16px;
  border-radius: 14px;
  margin-bottom: 20px;
  box-shadow: none;
  border: 1px solid var(--line);
}

.chip {
  border: none;
  background: #f4f5f8;
  color: var(--ink-2);
  padding: 8px 17px;
  border-radius: 999px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.chip:hover {
  background: #eceef2;
}

.chip.active {
  background: var(--ink);
  color: #fff;
  font-weight: 600;
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  min-height: 160px;
}

@media (max-width: 1000px) {
  .grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 700px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  .page-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
