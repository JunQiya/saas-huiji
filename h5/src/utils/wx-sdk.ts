import { wxApi } from '@/api/h5'

// 微信 JS-SDK 配置参数
interface WxConfigOptions {
  debug: boolean
  appId: string
  timestamp: string
  nonceStr: string
  signature: string
  jsApiList: string[]
}

// 分享给朋友所需数据
interface WxShareData {
  title: string
  desc: string
  link: string
  imgUrl: string
}

// 分享到朋友圈所需数据
interface WxTimelineShareData {
  title: string
  link: string
  imgUrl: string
}

// 扫一扫返回
interface WxScanQRCodeResult {
  resultStr: string
}

// 选择图片返回
interface WxChooseImageResult {
  localIds: string[]
}

// 微信 JS-SDK 全局对象类型
interface WxSdk {
  config(options: WxConfigOptions): void
  ready(callback: () => void): void
  error(callback: (res: { errMsg: string }) => void): void
  updateAppMessageShareData(data: WxShareData, cb?: () => void): void
  updateTimelineShareData(data: WxTimelineShareData, cb?: () => void): void
  scanQRCode(options: {
    needResult: number
    scanType: string[]
    success: (res: WxScanQRCodeResult) => void
  }): void
  chooseImage(options: {
    count: number
    sizeType: string[]
    sourceType: string[]
    success: (res: WxChooseImageResult) => void
  }): void
  previewImage(options: { current: string; urls: string[] }): void
  chooseWXPay(options: {
    timestamp: string
    nonceStr: string
    pkg: string
    signType: string
    paySign: string
    success: (res: { errMsg: string }) => void
    cancel: (res: { errMsg: string }) => void
    fail: (res: { errMsg: string }) => void
  }): void
}

declare global {
  interface Window {
    wx?: WxSdk
  }
}

let wxLoaded = false

// 动态加载微信 JS-SDK 脚本
function loadWxScript(): Promise<void> {
  if (wxLoaded && window.wx) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = 'https://res.wx.qq.com/open/js/jweixin-1.6.0.js'
    script.onload = () => {
      wxLoaded = true
      resolve()
    }
    script.onerror = () => reject(new Error('加载微信 JS-SDK 脚本失败'))
    document.head.appendChild(script)
  })
}

// 初始化 Promise（全局只执行一次）
let initPromise: Promise<void> | null = null

// 初始化微信 JS-SDK：加载脚本 → 获取签名 → wx.config → wx.ready
export async function initWxSdk(): Promise<void> {
  if (initPromise) return initPromise
  initPromise = (async () => {
    await loadWxScript()
    // iOS 下签名 URL 用首页地址，Android 用当前地址；hash 路由下取 # 之前部分即可
    const url = window.location.href.split('#')[0]
    const res = await wxApi.jssdk(url)
    if (!res || !res.appId) return // 未配置微信公众号，静默跳过
    window.wx!.config({
      debug: false,
      appId: res.appId,
      timestamp: res.timestamp,
      nonceStr: res.nonceStr,
      signature: res.signature,
      jsApiList: [
        'updateAppMessageShareData',
        'updateTimelineShareData',
        'scanQRCode',
        'chooseImage',
        'previewImage',
        'chooseWXPay'
      ]
    })
    await new Promise<void>((resolve, reject) => {
      window.wx!.ready(resolve)
      window.wx!.error((res) => reject(new Error(res.errMsg)))
    })
  })().catch((err) => {
    // 失败后清空缓存，允许后续重试
    initPromise = null
    throw err
  })
  return initPromise
}

// 设置分享内容（朋友 + 朋友圈）
export function wxShare(title: string, desc: string, link: string, imgUrl: string): void {
  if (!window.wx) return
  window.wx.updateAppMessageShareData({ title, desc, link, imgUrl })
  window.wx.updateTimelineShareData({ title, link, imgUrl })
}

// 微信 JSAPI 支付唤起参数
export interface WxPayParams {
  appId?: string
  timeStamp: string
  nonceStr: string
  pkg?: string
  package?: string
  signType: string
  paySign: string
}

// 唤起微信支付, 返回支付结果(errMsg)
export function wxChooseWXPay(params: WxPayParams): Promise<string> {
  return new Promise((resolve, reject) => {
    if (!window.wx) {
      reject(new Error('请在微信中打开完成支付'))
      return
    }
    window.wx.chooseWXPay({
      timestamp: params.timeStamp,
      nonceStr: params.nonceStr,
      pkg: params.pkg || params.package || '',
      signType: params.signType,
      paySign: params.paySign,
      success: (res) => resolve(res.errMsg),
      cancel: () => reject(new Error('支付已取消')),
      fail: (res) => reject(new Error(res.errMsg || '支付失败'))
    })
  })
}
