// 金额与日期格式化工具

// 分 -> 元（保留 2 位）
export function fenToYuan(fen: number | undefined | null): string {
  if (fen === null || fen === undefined || isNaN(fen as number)) return '0.00'
  return (fen / 100).toFixed(2)
}

// 元 -> 分（整数）
export function yuanToFen(yuan: number | string): number {
  const n = typeof yuan === 'string' ? parseFloat(yuan) : yuan
  if (isNaN(n)) return 0
  return Math.round(n * 100)
}

// 千分位金额展示
export function formatMoney(fen: number | undefined | null): string {
  const yuan = fenToYuan(fen)
  const [intPart, decPart] = yuan.split('.')
  const grouped = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return `${grouped}.${decPart}`
}

// 日期格式化 YYYY-MM-DD HH:mm
export function formatDateTime(iso?: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function formatDate(iso?: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// 环比百分比展示（正负带箭头）
export function formatDelta(delta: number | undefined | null): { text: string; up: boolean } {
  if (delta === null || delta === undefined || isNaN(delta as number)) {
    return { text: '0%', up: false }
  }
  const up = delta >= 0
  const sign = up ? '+' : ''
  return { text: `${sign}${delta.toFixed(1)}%`, up }
}
