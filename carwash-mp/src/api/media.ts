import type { components } from '@/api/schema'
import { ApiError, BASE_URL, getToken } from '@/utils/request'

export type MediaVO = components['schemas']['MediaVO']
export type MediaBizType = 'PARK' | 'PICK' | 'WASHED' | 'RETURN' | 'COMPARE' | 'VIDEO'

/**
 * 上传大小上限（MB）：与后端 wash.media.max-size-mb 保持一致。
 * 选图时就地预检，避免整张传完才被后端拒（14MB 图的事故：接口 200 但 body 是错误 JSON，
 * 用户侧看起来像"没反应"）。
 */
export const MAX_UPLOAD_MB = 10

/**
 * 影像上传。契约：openapi.yaml POST /api/v1/media/upload（multipart/form-data）。
 * 二进制走 wx.uploadFile 而非统一 request，但同样拆 {code,msg,data} 信封，失败统一抛 ApiError。
 */
export function uploadMedia(filePath: string, bizType: MediaBizType, orderNo?: string): Promise<MediaVO> {
  return new Promise<MediaVO>((resolve, reject) => {
    const token = getToken()
    uni.uploadFile({
      url: BASE_URL + '/api/v1/media/upload',
      filePath,
      name: 'file',
      formData: { bizType, ...(orderNo ? { orderNo } : {}) },
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new ApiError(res.statusCode, '上传失败'))
          return
        }
        try {
          const body = JSON.parse(res.data) as { code: number; msg: string; data?: MediaVO }
          if (body.code === 0 && body.data) {
            resolve(body.data)
          } else {
            reject(new ApiError(body.code, body.msg || '上传失败'))
          }
        } catch {
          reject(new ApiError(-1, '上传响应解析失败'))
        }
      },
      fail: (err) => reject(new ApiError(-1, (err as { errMsg?: string }).errMsg || '上传失败')),
    })
  })
}
