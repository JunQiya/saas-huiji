// CSV 导出工具（处理 BOM、特殊字符、UTF-8 编码，兼容 Excel 打开中文不乱码）

function escapeCell(v: any): string {
  if (v == null) return ''
  const s = String(v)
  // 引号、换行、逗号需要用双引号包裹，并把内部双引号转义为两个
  if (/[",\n\r]/.test(s)) {
    return '"' + s.replace(/"/g, '""') + '"'
  }
  return s
}

export interface CsvColumn<T = any> {
  key: keyof T | string
  header: string
  /** 自定义格式化（cell value => 字符串） */
  format?: (row: T) => any
}

/**
 * 导出为 CSV 文件
 * @param filename 文件名（不含 .csv，自动追加）
 * @param rows 数据行
 * @param columns 列定义 [{ key, header, format? }]
 */
export function exportCsv<T = any>(filename: string, rows: T[], columns: CsvColumn<T>[]) {
  if (!rows?.length && !columns.length) return
  const headers = columns.map(c => escapeCell(c.header))
  const body = rows.map(r => columns.map(c => {
    const raw = c.format ? c.format(r) : (r as any)[c.key]
    return escapeCell(raw)
  }))
  // UTF-8 BOM 让 Excel 正确识别中文
  const csv = '\uFEFF' + [headers, ...body].map(line => line.join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  triggerDownload(blob, ensureExt(filename, 'csv'))
}

/**
 * 简单表格导出（不依赖列定义）
 */
export function quickCsv(filename: string, headers: string[], rows: any[][]) {
  const data = rows.map(r => r.map(c => escapeCell(c)))
  const csv = '\uFEFF' + [headers.map(escapeCell), ...data].map(line => line.join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  triggerDownload(blob, ensureExt(filename, 'csv'))
}

function ensureExt(name: string, ext: string) {
  return name.endsWith('.' + ext) ? name : `${name}.${ext}`
}

function triggerDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.style.display = 'none'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  // 给浏览器一点时间后再回收，避免大文件下载中断
  setTimeout(() => URL.revokeObjectURL(url), 1500)
}
