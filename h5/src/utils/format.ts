// 金额：分 → 元（保留 2 位）
export function fenToYuan(fen: number | string | undefined | null): string {
  if (fen === undefined || fen === null || fen === '') return '0.00'
  const num = Number(fen)
  if (Number.isNaN(num)) return '0.00'
  return (num / 100).toFixed(2)
}

// 金额：元 → 分（整数）
export function yuanToFen(yuan: number | string): number {
  const num = Number(yuan)
  if (Number.isNaN(num)) return 0
  return Math.round(num * 100)
}

// 日期格式化：ISO → YYYY-MM-DD HH:mm
export function formatDateTime(iso?: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 日期格式化：ISO → YYYY-MM-DD
export function formatDate(iso?: string): string {
  if (!iso) return '-'
  return formatDateTime(iso).split(' ')[0]
}

// 手机号脱敏：138****8888
export function maskPhone(phone?: string): string {
  if (!phone || phone.length < 11) return phone || ''
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}
