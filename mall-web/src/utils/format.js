/** 价格拆成整数和小数，方便排版 */
export function splitPrice(value) {
  const num = Number(value || 0)
  const fixed = num.toFixed(2)
  const [int, dec] = fixed.split('.')
  return { int, dec }
}

export function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}

/** Safari 对 yyyy-MM-dd HH:mm:ss 解析不友好，统一替换 */
export function parseTime(value) {
  if (!value) return null
  if (value instanceof Date) return value
  return new Date(String(value).replace(/-/g, '/'))
}

export function formatTime(value) {
  const date = parseTime(value)
  if (!date || Number.isNaN(date.getTime())) return '-'
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}`
}

export function pad2(n) {
  return String(n).padStart(2, '0')
}
