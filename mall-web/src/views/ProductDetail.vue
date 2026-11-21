<template>
  <div v-loading="loading" class="page">
    <div class="container">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/products' }">全部商品</el-breadcrumb-item>
        <el-breadcrumb-item>{{ product?.name || '商品详情' }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div v-if="product" class="detail card">
        <div class="gallery">
          <div class="main-img">
            <img :src="activeImage" :alt="product.name" />
          </div>
          <div v-if="imageList.length > 1" class="thumbs">
            <img
              v-for="img in imageList"
              :key="img"
              :src="img"
              :class="{ active: img === activeImage }"
              @mouseenter="activeImage = img"
            />
          </div>
        </div>

        <div class="info">
          <div class="tags">
            <span class="tag-soft">{{ product.categoryName }}</span>
            <span class="tag-stock" :class="{ low: product.stock < 20 }">
              {{ product.stock > 0 ? `库存 ${product.stock} 件` : '暂时缺货' }}
            </span>
          </div>
          <h1>{{ product.name }}</h1>
          <p class="subtitle">{{ product.subtitle }}</p>

          <div class="price-box">
            <span class="price"><small>¥</small>{{ splitPrice(product.price).int }}<span class="decimal">.{{ splitPrice(product.price).dec }}</span></span>
            <span v-if="product.originalPrice" class="original">¥{{ formatPrice(product.originalPrice) }}</span>
            <span class="sales">已售 {{ product.sales }} 件</span>
          </div>

          <div class="promo">
            <span class="promo-label">服务</span>
            <span>正品保障</span><span>极速发货</span><span>7 天无理由</span><span>包邮</span>
          </div>

          <div v-if="activity" class="seckill-box" @click="goSeckill">
            <div class="left">
              <span class="fire">⚡</span>
              <div>
                <strong>{{ activity.title }}</strong>
                <em>
                  秒杀价
                  <b>¥{{ formatPrice(activity.seckillPrice) }}</b>
                  <span v-if="activity.state === 'ONGOING'">· 抢购中</span>
                  <span v-else>· 即将开始</span>
                </em>
              </div>
            </div>
            <div class="right">
              <CountdownTimer :start="activity.startTime" :end="activity.endTime" compact />
              <el-button type="primary" round>去抢购</el-button>
            </div>
          </div>

          <div class="actions">
            <el-button type="primary" size="large" round @click="handleBuy">
              {{ activity && activity.state === 'ONGOING' ? '立即秒杀' : '立即购买' }}
            </el-button>
            <el-button size="large" round @click="goProducts">继续逛</el-button>
          </div>
        </div>
      </div>

      <div v-if="product" class="desc card">
        <h2>商品详情</h2>
        <div class="desc-body">{{ product.description }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProduct, getSeckillList } from '@/api'
import CountdownTimer from '@/components/CountdownTimer.vue'
import { formatPrice, splitPrice } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const product = ref(null)
const activity = ref(null)
const activeImage = ref('')

const imageList = computed(() => {
  if (!product.value) return []
  const list = (product.value.images || '').split(',').map((s) => s.trim()).filter(Boolean)
  return list.length ? list : [product.value.cover]
})

onMounted(async () => {
  try {
    const data = await getProduct(route.params.id)
    product.value = data
    activeImage.value = data.cover
    try {
      const list = await getSeckillList()
      activity.value = (list || []).find(
        (item) => item.productId === data.id && item.state !== 'ENDED'
      ) || null
    } catch (e) {
      activity.value = null
    }
  } catch (e) {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
})

function goSeckill() {
  if (activity.value) router.push(`/seckill/${activity.value.id}`)
}

function goProducts() {
  router.push('/products')
}

function handleBuy() {
  if (activity.value) {
    goSeckill()
    return
  }
  ElMessage.info('该商品暂无秒杀活动，可关注下一场')
}
</script>

<style scoped>
.el-breadcrumb {
  margin-bottom: 18px;
  font-size: 13px;
}

.detail {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
  gap: 40px;
  padding: 26px;
}

.gallery {
  min-width: 0;
}

.main-img {
  aspect-ratio: 1 / 1;
  border-radius: 16px;
  overflow: hidden;
  background: #f3f4f7;
}

.main-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbs {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.thumbs img {
  width: 68px;
  height: 68px;
  border-radius: 10px;
  object-fit: cover;
  cursor: pointer;
  border: 2px solid transparent;
  opacity: 0.7;
  transition: all 0.2s;
}

.thumbs img:hover,
.thumbs img.active {
  border-color: var(--brand);
  opacity: 1;
}

.info {
  padding-top: 6px;
  min-width: 0;
}

.tags {
  display: flex;
  gap: 10px;
  align-items: center;
}

.tag-stock {
  font-size: 12px;
  color: #0f766e;
  background: #ecfdf5;
  padding: 3px 9px;
  border-radius: 999px;
}

.tag-stock.low {
  color: #b45309;
  background: #fffbeb;
}

h1 {
  font-size: 29px;
  line-height: 1.35;
  margin: 16px 0 8px;
  letter-spacing: -0.6px;
}

.subtitle {
  color: var(--ink-3);
  font-size: 14px;
  margin: 0;
}

.price-box {
  display: flex;
  align-items: baseline;
  gap: 12px;
  background: linear-gradient(90deg, #fff4f2, #fffaf5);
  border-radius: 14px;
  padding: 18px 20px;
  margin: 22px 0;
}

.price-box .price {
  font-size: 36px;
}

.sales {
  margin-left: auto;
  font-size: 13px;
  color: var(--ink-3);
}

.promo {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: var(--ink-2);
  padding-bottom: 20px;
  border-bottom: 1px dashed var(--line);
}

.promo-label {
  color: var(--ink-3);
}

.seckill-box {
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 16px;
  background: #15181f;
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s;
}

.seckill-box:hover {
  transform: translateY(-2px);
}

.left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.fire {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #ff3b30, #ff9f1c);
  font-size: 20px;
  flex-shrink: 0;
}

.left strong {
  display: block;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.left em {
  font-style: normal;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.62);
  display: block;
  margin-top: 4px;
}

.left b {
  color: #ffb020;
  font-size: 15px;
}

.right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.actions {
  display: flex;
  gap: 12px;
  margin-top: 26px;
}

.desc {
  margin-top: 22px;
  padding: 28px 30px 34px;
}

.desc h2 {
  font-size: 19px;
  margin: 0 0 18px;
}

.desc-body {
  white-space: pre-line;
  line-height: 2;
  color: var(--ink-2);
  font-size: 14px;
}

@media (max-width: 900px) {
  .detail {
    grid-template-columns: 1fr;
    gap: 22px;
    padding: 18px;
  }
  .seckill-box {
    flex-direction: column;
    align-items: flex-start;
  }
  h1 {
    font-size: 23px;
  }
}
</style>
