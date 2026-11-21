<template>
  <article class="product-card" @click="goDetail">
    <div class="cover">
      <img :src="product.cover" :alt="product.name" loading="lazy" />
      <span class="cat">{{ product.categoryName }}</span>
      <span v-if="product.sales > 200" class="hot">热卖</span>
    </div>
    <div class="info">
      <h3 class="line-clamp-2">{{ product.name }}</h3>
      <p class="sub">{{ product.subtitle }}</p>
      <div class="bottom">
        <div class="price">
          <small>¥</small>{{ splitPrice(product.price).int }}<span class="decimal">.{{ splitPrice(product.price).dec }}</span>
        </div>
        <div class="sales">已售 {{ product.sales }}</div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { splitPrice } from '@/utils/format'

const props = defineProps({
  product: { type: Object, required: true }
})

const router = useRouter()

function goDetail() {
  router.push(`/product/${props.product.id}`)
}
</script>

<style scoped>
.product-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid transparent;
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s, border-color 0.25s;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-hover);
  border-color: #ffe1dd;
}

.cover {
  position: relative;
  aspect-ratio: 1 / 1;
  background: #f3f4f7;
  overflow: hidden;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s ease;
}

.product-card:hover .cover img {
  transform: scale(1.055);
}

.cat {
  position: absolute;
  left: 12px;
  top: 12px;
  font-size: 11px;
  color: #4b5563;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(6px);
  padding: 4px 9px;
  border-radius: 999px;
  font-weight: 600;
}

.hot {
  position: absolute;
  right: 12px;
  top: 12px;
  font-size: 11px;
  color: #fff;
  background: linear-gradient(135deg, #ff3b30, #ff7a45);
  padding: 4px 9px;
  border-radius: 999px;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.3);
}

.info {
  padding: 14px 15px 16px;
}

h3 {
  margin: 0;
  font-size: 15px;
  line-height: 1.45;
  font-weight: 600;
  min-height: 43px;
}

.sub {
  margin: 5px 0 12px;
  font-size: 12px;
  color: var(--ink-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
}

.price {
  font-size: 21px;
  line-height: 1;
}

.sales {
  font-size: 12px;
  color: var(--ink-3);
  white-space: nowrap;
}
</style>
