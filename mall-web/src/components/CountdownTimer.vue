<template>
  <div class="countdown" :class="[tone, { compact }]">
    <span v-if="!hideLabel" class="cd-label">{{ label }}</span>
    <div class="cd-nums">
      <template v-for="(part, index) in parts" :key="index">
        <span class="cd-num">{{ pad2(part) }}</span>
        <span v-if="index === 0 && parts.length === 4" class="cd-unit">天</span>
        <span v-if="index < parts.length - 1" class="cd-colon">:</span>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { pad2, parseTime } from '@/utils/format'

const props = defineProps({
  start: { type: String, default: '' },
  end: { type: String, default: '' },
  compact: { type: Boolean, default: false },
  hideLabel: { type: Boolean, default: false },
  tone: { type: String, default: 'light' }
})

const emit = defineEmits(['ended'])
const now = ref(Date.now())
let timer = null

onMounted(() => {
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})

const startAt = computed(() => parseTime(props.start)?.getTime() || 0)
const endAt = computed(() => parseTime(props.end)?.getTime() || 0)
const isBeforeStart = computed(() => startAt.value > 0 && now.value < startAt.value)
const target = computed(() => (isBeforeStart.value ? startAt.value : endAt.value))
const label = computed(() => (isBeforeStart.value ? '距开始' : '距结束'))

const diff = computed(() => Math.max(0, target.value - now.value))
const ended = computed(() => endAt.value > 0 && now.value >= endAt.value)

watch(ended, (value) => {
  if (value) emit('ended')
})

const parts = computed(() => {
  const total = Math.floor(diff.value / 1000)
  const days = Math.floor(total / 86400)
  const hours = Math.floor((total % 86400) / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const seconds = total % 60
  return days > 0 ? [days, hours, minutes, seconds] : [hours, minutes, seconds]
})
</script>

<style scoped>
.countdown {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-variant-numeric: tabular-nums;
}

.cd-label {
  font-size: 12px;
  color: var(--ink-3);
  white-space: nowrap;
}

.cd-nums {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.cd-num {
  min-width: 26px;
  height: 26px;
  padding: 0 5px;
  border-radius: 7px;
  background: #1c1f26;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.cd-colon {
  color: var(--ink-3);
  font-weight: 700;
  font-size: 12px;
}

.cd-unit {
  font-size: 11px;
  color: var(--ink-3);
  margin: 0 1px;
}

.dark .cd-unit {
  color: rgba(255, 255, 255, 0.55);
}

.compact .cd-num {
  min-width: 22px;
  height: 22px;
  font-size: 12px;
  border-radius: 6px;
}

.dark .cd-num {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  backdrop-filter: blur(4px);
}

.dark .cd-label,
.dark .cd-colon {
  color: rgba(255, 255, 255, 0.65);
}

.light .cd-num {
  background: #fff;
  color: var(--brand);
  box-shadow: 0 2px 8px rgba(255, 59, 48, 0.16);
}

.light .cd-label {
  color: var(--ink-3);
}
</style>
