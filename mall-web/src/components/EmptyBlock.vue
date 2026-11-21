<template>
  <div class="empty">
    <div class="icon">{{ icon }}</div>
    <p>{{ text }}</p>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  text: { type: String, default: '暂无数据' },
  icon: { type: String, default: '🌵' }
})
</script>

<style scoped>
.empty {
  padding: 60px 20px;
  text-align: center;
  background: #fff;
  border-radius: var(--radius);
}

.icon {
  font-size: 44px;
  margin-bottom: 12px;
}

p {
  margin: 0 0 18px;
  color: var(--ink-3);
  font-size: 14px;
}
</style>
