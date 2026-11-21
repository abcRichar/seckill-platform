<template>
  <article class="seckill-card" :class="{ ended: activity.state === 'ENDED' }" @click="goDetail">
    <div class="cover">
      <img :src="activity.productCover" :alt="activity.title" loading="lazy" />
      <div class="state" :class="stateClass">{{ stateText }}</div>
    </div>
    <div class="info">
      <div class="title-row">
        <h3 class="line-clamp-2">{{ activity.title }}</h3>
      </div>
      <div class="price-row">
        <span class="price">
          <small>¥</small>{{ splitPrice(activity.seckillPrice).int
          }}<span class="decimal">.{{ splitPrice(activity.seckillPrice).dec }}</span>
        </span>
        <span class="original">¥{{ formatPrice(activity.originalPrice) }}</span>
      </div>
      <div class="stock-row">
        <div class="bar"><i :style="{ width: activity.soldPercent + '%' }"></i></div>
        <span>已抢 {{ activity.soldPercent }}%</span>
      </div>
      <div class="foot">
        <CountdownTimer
          v-if="activity.state !== 'ENDED'"
          :start="activity.startTime"
          :end="activity.endTime"
          compact
          hide-label
        />
        <span v-else class="ended-text">本场已结束</span>
        <el-button
          :type="activity.state === 'ONGOING' ? 'primary' : 'default'"
          size="small"
          round
          :disabled="activity.state === 'ENDED' || activity.stock <= 0"
        >
          {{ buttonText }}
        </el-button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import CountdownTimer from './CountdownTimer.vue'
import { formatPrice, splitPrice } from '@/utils/format'

const props = defineProps({
  activity: { type: Object, required: true }
})

const router = useRouter()

const stateText = computed(() => {
  const map = { ONGOING: '抢购中', UPCOMING: '即将开始', ENDED: '已结束' }
  return map[props.activity.state] || '待开始'
})

const stateClass = computed(() => (props.activity.state || '').toLowerCase())

const buttonText = computed(() => {
  if (props.activity.state === 'ENDED') return '已结束'
  if (props.activity.state === 'UPCOMING') return '先看看'
  if (props.activity.stock <= 0) return '已抢光'
  return '立即抢'
})

function goDetail() {
  router.push(`/seckill/${props.activity.id}`)
}
</script>

<style scoped>
.seckill-card {
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid transparent;
  transition: transform 0.25s, box-shadow 0.25s;
}

.seckill-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-hover);
}

.seckill-card.ended {
  opacity: 0.72;
}

.cover {
  position: relative;
  aspect-ratio: 16 / 10;
  background: #f3f4f7;
  overflow: hidden;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s;
}

.seckill-card:hover .cover img {
  transform: scale(1.05);
}

.state {
  position: absolute;
  left: 12px;
  top: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  backdrop-filter: blur(6px);
}

.state.ongoing {
  background: linear-gradient(135deg, #ff3b30, #ff7a45);
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.35);
}

.state.upcoming {
  background: rgba(17, 21, 28, 0.72);
}

.state.ended {
  background: rgba(120, 128, 140, 0.8);
}

.info {
  padding: 14px 15px 15px;
}

h3 {
  margin: 0;
  font-size: 15px;
  line-height: 1.45;
  min-height: 43px;
  font-weight: 600;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 8px;
}

.price {
  font-size: 22px;
}

.stock-row {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 10px 0 12px;
  font-size: 11px;
  color: var(--ink-3);
}

.bar {
  flex: 1;
  height: 6px;
  background: #f1f2f5;
  border-radius: 999px;
  overflow: hidden;
}

.bar i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #ff9f1c, #ff3b30);
  transition: width 0.6s;
}

.foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.ended-text {
  font-size: 12px;
  color: var(--ink-3);
}
</style>
