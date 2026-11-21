<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>操作日志</h1>
        <p>管理端增删改操作由 AOP 自动记录，异步落库</p>
      </div>
      <el-button @click="fetchList"><el-icon><Refresh /></el-icon> 刷新</el-button>
    </div>

    <section class="card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="操作人 / 动作"
          clearable
          style="width: 220px"
          prefix-icon="Search"
          @keyup.enter="search"
          @clear="search"
        />
        <el-select v-model="query.module" placeholder="全部模块" clearable style="width: 150px" @change="search">
          <el-option v-for="item in modules" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button @click="search">查询</el-button>
        <el-button text @click="reset">重置</el-button>
        <span class="spacer"></span>
        <span class="muted total-tip">共 {{ total }} 条日志</span>
      </div>

      <div class="table-wrap">
        <el-table v-loading="loading" :data="list" style="width: 100%">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="expand">
                <div class="expand-label">请求参数</div>
                <pre>{{ pretty(row.params) }}</pre>
                <template v-if="row.errorMsg">
                  <div class="expand-label error">错误信息</div>
                  <pre class="error-text">{{ row.errorMsg }}</pre>
                </template>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ formatTime(row.createTime, true) }}</template>
          </el-table-column>
          <el-table-column label="操作人" width="120">
            <template #default="{ row }">{{ row.username || '-' }}</template>
          </el-table-column>
          <el-table-column prop="module" label="模块" width="110" />
          <el-table-column prop="action" label="动作" min-width="150" />
          <el-table-column prop="ip" label="IP" width="130" />
          <el-table-column label="耗时" width="90">
            <template #default="{ row }">
              <span :class="{ slow: row.duration > 500 }">{{ row.duration }}ms</span>
            </template>
          </el-table-column>
          <el-table-column label="结果" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light" round size="small">
                {{ row.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="method" label="方法" min-width="220" show-overflow-tooltip />
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getLogs } from '@/api'
import { formatTime } from '@/utils/format'

const modules = ['商品管理', '分类管理', '秒杀活动', '订单管理', '用户管理', '权限管理']
const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({ page: 1, size: 10, keyword: '', module: '' })

async function fetchList() {
  loading.value = true
  try {
    const data = await getLogs({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      module: query.module || undefined
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
  Object.assign(query, { page: 1, keyword: '', module: '' })
  fetchList()
}

function pretty(value) {
  if (!value) return '无'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch (e) {
    return value
  }
}

onMounted(fetchList)
</script>

<style scoped>
.total-tip {
  font-size: 12px;
}

.expand {
  padding: 8px 20px 14px 48px;
}

.expand-label {
  font-size: 11px;
  color: var(--ink-3);
  margin: 6px 0;
  letter-spacing: 0.5px;
}

.expand-label.error {
  color: #ef4444;
}

pre {
  margin: 0;
  background: #0f172a;
  color: #cbd5e1;
  padding: 13px 15px;
  border-radius: 9px;
  font-size: 12px;
  line-height: 1.7;
  overflow: auto;
  max-height: 220px;
  font-family: "JetBrains Mono", Consolas, monospace;
}

.error-text {
  background: #fef2f2;
  color: #b91c1c;
}

.slow {
  color: #f59e0b;
  font-weight: 600;
}
</style>
