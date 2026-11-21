<template>
  <div class="home">
    <section class="hero">
      <div class="container hero-grid">
        <div class="banner" @click="goFeatured">
          <img :src="bannerUrl" alt="限时秒杀" />
          <div class="floating">
            <span class="dot"></span>
            <div>
              <strong>{{ liveCount }} 场秒杀进行中</strong>
              <em>Redis 预扣库存 · 抢完即止</em>
            </div>
          </div>
        </div>

        <aside class="feature-card">
          <div class="feature-head">
            <span>今日秒杀</span>
            <em>限时低价</em>
          </div>
          <template v-if="featured">
            <div class="feature-cover" @click="goSeckill(featured.id)">
              <img :src="featured.productCover" :alt="featured.title" />
              <span class="badge">{{ featured.state === 'UPCOMING' ? '即将开始' : '抢购中' }}</span>
            </div>
            <h3 class="line-clamp-2" @click="goSeckill(featured.id)">{{ featured.title }}</h3>
            <div class="feature-price">
              <span class="price"><small>¥</small>{{ splitPrice(featured.seckillPrice).int }}</span>
              <span class="original">¥{{ formatPrice(featured.originalPrice) }}</span>
            </div>
            <div class="feature-cd">
              <CountdownTimer :start="featured.startTime" :end="featured.endTime" compact />
            </div>
            <el-button type="primary" round class="feature-btn" @click="goSeckill(featured.id)">
              去看看 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </template>
          <div v-else class="feature-empty">下一场正在筹备中</div>
        </aside>
      </div>
    </section>

    <div class="container">
      <section id="seckill" class="block">
        <div class="section-head">
          <h2>秒杀专场 <span class="sub">时间一到，手慢无</span></h2>
          <router-link to="/products" class="more-link">查看全部商品 <el-icon><ArrowRight /></el-icon></router-link>
        </div>
        <div v-loading="loading" class="seckill-grid">
          <SeckillCard v-for="item in activities" :key="item.id" :activity="item" />
          <EmptyBlock v-if="!loading && activities.length === 0" text="暂时没有秒杀活动" icon="⏰" class="grid-empty" />
        </div>
      </section>

      <section class="block category-block">
        <div class="section-head">
          <h2>按分类逛</h2>
        </div>
        <div class="chips">
          <router-link to="/products" class="chip">全部好物</router-link>
          <router-link
            v-for="cat in categories"
            :key="cat.id"
            :to="{ path: '/products', query: { categoryId: cat.id } }"
            class="chip"
          >
            {{ cat.name }}
          </router-link>
        </div>
      </section>

      <section class="block">
        <div class="section-head">
          <h2>精选好物 <span class="sub">口碑与销量都在线</span></h2>
          <router-link to="/products" class="more-link">全部商品 <el-icon><ArrowRight /></el-icon></router-link>
        </div>
        <div v-loading="loading" class="product-grid">
          <ProductCard v-for="item in products" :key="item.id" :product="item" />
        </div>
      </section>

      <section class="services">
        <div class="service">
          <span class="ico">🛡️</span>
          <div><strong>正品保障</strong><em>品牌直供，假一赔十</em></div>
        </div>
        <div class="service">
          <span class="ico">⚡</span>
          <div><strong>极速发货</strong><em>当日下单，48 小时内发出</em></div>
        </div>
        <div class="service">
          <span class="ico">↩️</span>
          <div><strong>7 天无理由</strong><em>不满意可退换</em></div>
        </div>
        <div class="service">
          <span class="ico">🎧</span>
          <div><strong>专属客服</strong><em>9:00 - 21:00 在线</em></div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCategories, getProducts, getSeckillList } from '@/api'
import SeckillCard from '@/components/SeckillCard.vue'
import ProductCard from '@/components/ProductCard.vue'
import CountdownTimer from '@/components/CountdownTimer.vue'
import EmptyBlock from '@/components/EmptyBlock.vue'
import { formatPrice, splitPrice } from '@/utils/format'

const router = useRouter()
const bannerUrl = '/uploads/seed/banner.svg'
const loading = ref(true)
const activities = ref([])
const products = ref([])
const categories = ref([])

const liveCount = computed(() => activities.value.filter((a) => a.state === 'ONGOING').length)
const featured = computed(
  () =>
    activities.value.find((a) => a.state === 'ONGOING') ||
    activities.value.find((a) => a.state === 'UPCOMING') ||
    null
)

onMounted(async () => {
  try {
    const [activityList, productPage, categoryList] = await Promise.all([
      getSeckillList(),
      getProducts({ page: 1, size: 8, sort: 'sales' }),
      getCategories()
    ])
    activities.value = activityList || []
    products.value = productPage?.records || []
    categories.value = categoryList || []
  } catch (e) {
    // 错误提示由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
})

function goSeckill(id) {
  router.push(`/seckill/${id}`)
}

function goFeatured() {
  if (featured.value) goSeckill(featured.value.id)
  else router.push('/products')
}
</script>

<style scoped>
.hero {
  padding: 26px 0 6px;
  background:
    radial-gradient(900px 320px at 12% -10%, rgba(255, 59, 48, 0.09), transparent 60%),
    radial-gradient(700px 300px at 90% 0%, rgba(255, 159, 28, 0.1), transparent 60%);
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 2.15fr) minmax(280px, 1fr);
  gap: 20px;
}

.banner {
  position: relative;
  border-radius: 22px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--shadow);
  aspect-ratio: 12 / 7;
  background: #111827;
}

.banner img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s;
}

.banner:hover img {
  transform: scale(1.02);
}

.floating {
  position: absolute;
  left: 22px;
  bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px 10px 12px;
  border-radius: 14px;
  background: rgba(17, 21, 28, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(10px);
  color: #fff;
}

.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 0 5px rgba(74, 222, 128, 0.16);
  animation: pulse 1.6s infinite;
}

@keyframes pulse {
  50% {
    box-shadow: 0 0 0 9px rgba(74, 222, 128, 0.04);
  }
}

.floating strong {
  display: block;
  font-size: 13px;
}

.floating em {
  display: block;
  font-style: normal;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.62);
  margin-top: 2px;
}

.feature-card {
  background: #fff;
  border-radius: 22px;
  padding: 18px;
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
}

.feature-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 13px;
}

.feature-head span {
  font-size: 17px;
  font-weight: 700;
}

.feature-head em {
  font-style: normal;
  font-size: 11px;
  color: var(--brand);
  background: var(--brand-soft);
  padding: 3px 8px;
  border-radius: 999px;
  font-weight: 600;
}

.feature-cover {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  aspect-ratio: 16 / 10;
  cursor: pointer;
  background: #f3f4f7;
}

.feature-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.badge {
  position: absolute;
  left: 10px;
  top: 10px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ff3b30, #ff7a45);
  padding: 4px 9px;
  border-radius: 999px;
}

.feature-card h3 {
  font-size: 15px;
  line-height: 1.45;
  margin: 12px 0 8px;
  cursor: pointer;
  min-height: 43px;
}

.feature-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.feature-price .price {
  font-size: 24px;
}

.feature-cd {
  margin: 10px 0 14px;
}

.feature-btn {
  margin-top: auto;
  width: 100%;
}

.feature-empty {
  flex: 1;
  display: grid;
  place-items: center;
  color: var(--ink-3);
  font-size: 13px;
}

.block {
  margin-top: 8px;
}

.seckill-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  min-height: 120px;
}

.grid-empty {
  grid-column: 1 / -1;
}

.category-block {
  margin-top: 34px;
}

.chips {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.chip {
  padding: 9px 18px;
  background: #fff;
  border-radius: 999px;
  font-size: 14px;
  color: var(--ink-2);
  border: 1px solid transparent;
  transition: all 0.2s;
  box-shadow: 0 2px 10px rgba(16, 24, 40, 0.04);
}

.chip:hover {
  color: var(--brand);
  border-color: #ffd4cf;
  transform: translateY(-1px);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  min-height: 120px;
}

.services {
  margin-top: 46px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  background: #fff;
  border-radius: 20px;
  padding: 26px 24px;
  box-shadow: var(--shadow);
}

.service {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ico {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: #f6f7f9;
  display: grid;
  place-items: center;
  font-size: 21px;
  flex-shrink: 0;
}

.service strong {
  display: block;
  font-size: 14px;
}

.service em {
  display: block;
  font-style: normal;
  font-size: 12px;
  color: var(--ink-3);
  margin-top: 3px;
}

@media (max-width: 1000px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }
  .feature-card {
    max-width: 420px;
  }
  .seckill-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .services {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .seckill-grid,
  .product-grid,
  .services {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
  .services {
    grid-template-columns: 1fr;
  }
  .hero {
    padding-top: 14px;
  }
}
</style>
