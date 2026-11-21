export function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}

export function parseTime(value) {
  if (!value) return null
  return new Date(String(value).replace(/-/g, '/'))
}

export function formatTime(value, withSeconds = false) {
  const date = parseTime(value)
  if (!date || Number.isNaN(date.getTime())) return '-'
  const pad = (n) => String(n).padStart(2, '0')
  const base = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}`
  return withSeconds ? `${base}:${pad(date.getSeconds())}` : base
}
