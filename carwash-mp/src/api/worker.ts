import type { components } from '@/api/schema'
import { request } from '@/utils/request'

export type WorkerLoginVO = components['schemas']['WorkerLoginVO']
export type PickTaskVO = components['schemas']['PickTaskVO']
export type StationQueueVO = components['schemas']['StationQueueVO']
export type OpenCodeVO = components['schemas']['OpenCodeVO']
export type WorkerProfileVO = components['schemas']['WorkerProfileVO']
export type WorkerQualificationVO = components['schemas']['WorkerQualificationVO']
export type WorkerWorkStatus = 'OFFLINE' | 'ONLINE' | 'BUSY'

/**
 * 师傅端接口。契约：openapi.yaml /api/v1/worker|pick|station/**。
 * token 由 utils/request 自动按 URL 选取师傅 token（isWorkerApi），这里不用关心。
 */

/** 契约：POST /api/v1/worker/login（工号登录，与 C 端微信登录完全隔离） */
export function workerLogin(workerNo: string, password: string): Promise<WorkerLoginVO> {
  return request<WorkerLoginVO>({
    url: '/api/v1/worker/login',
    method: 'POST',
    data: { workerNo, password },
    auth: false,
  })
}

/** 任务池 stage：PICKUP 待取车 / RETURN 待送回 / ALL 全部 */
export type PickStage = 'PICKUP' | 'RETURN' | 'ALL'

/** 契约：GET /api/v1/pick/tasks */
export function fetchPickTasks(stage: PickStage = 'ALL'): Promise<PickTaskVO[]> {
  return request<PickTaskVO[]>({
    url: '/api/v1/pick/tasks',
    method: 'GET',
    data: { stage },
  })
}

/**
 * 契约：POST /api/v1/pick/orders/{orderNo}/open-code（仅 KEY_IN 可调用）
 * slotNo 为扫柜机格口二维码所得：传了由服务端校验格口匹配（一物一码，防开错柜）；
 * 不传则只取备用开箱码（扫码失败时的兜底）。
 */
export function fetchWorkerOpenCode(orderNo: string, slotNo?: string): Promise<OpenCodeVO> {
  return request<OpenCodeVO>({
    url: `/api/v1/pick/orders/${orderNo}/open-code`,
    method: 'POST',
    data: { slotNo },
  })
}

/** 契约：POST /api/v1/pick/orders/{orderNo}/take-key（必带开箱码，一次有效；KEY_IN → PICKING） */
export function takeKey(orderNo: string, code: string): Promise<void> {
  return request<void>({
    url: `/api/v1/pick/orders/${orderNo}/take-key`,
    method: 'POST',
    data: { code },
  })
}

/** 契约：POST /api/v1/pick/orders/{orderNo}/pick-car-done（≥6 张照片） */
export function pickCarDone(orderNo: string, fileIds: string[], note?: string): Promise<void> {
  return request<void>({
    url: `/api/v1/pick/orders/${orderNo}/pick-car-done`,
    method: 'POST',
    data: { fileIds, note },
  })
}

/** 契约：POST /api/v1/pick/orders/{orderNo}/return-done（停放照 + 车位号） */
export function returnDone(orderNo: string, fileIds: string[], parkingNo: string): Promise<void> {
  return request<void>({
    url: `/api/v1/pick/orders/${orderNo}/return-done`,
    method: 'POST',
    data: { fileIds, parkingNo },
  })
}

/** 契约：GET /api/v1/worker/profile（档案 + 打卡 / 资质 / 在班 / 负载聚合） */
export function fetchWorkerProfile(): Promise<WorkerProfileVO> {
  return request<WorkerProfileVO>({ url: '/api/v1/worker/profile', method: 'GET' })
}

/** 契约：POST /api/v1/worker/work-status（打卡上下班，本人操作） */
export function setWorkerWorkStatus(workStatus: WorkerWorkStatus): Promise<{ workStatus: WorkerWorkStatus }> {
  return request<{ workStatus: WorkerWorkStatus }>({
    url: '/api/v1/worker/work-status',
    method: 'POST',
    data: { workStatus },
  })
}

/** 契约：GET /api/v1/worker/qualification（我的资质，未提交返回空态） */
export function fetchMyQualification(): Promise<WorkerQualificationVO> {
  return request<WorkerQualificationVO>({ url: '/api/v1/worker/qualification', method: 'GET' })
}

/** 契约：POST /api/v1/worker/qualification（提交 / 重新提交资质） */
export function submitQualification(payload: {
  idName: string
  idNo: string
  idFrontFile: string
  idBackFile: string
  licenseFile?: string
}): Promise<void> {
  return request<void>({ url: '/api/v1/worker/qualification', method: 'POST', data: payload })
}

/** 契约：GET /api/v1/pick/pool（待派单池，可抢单） */
export function fetchPickPool(stage: PickStage = 'ALL'): Promise<PickTaskVO[]> {
  return request<PickTaskVO[]>({
    url: '/api/v1/pick/pool',
    method: 'GET',
    data: { stage },
  })
}

/** 契约：POST /api/v1/pick/orders/{orderNo}/claim（抢单，并发下失败返 B1002） */
export function claimOrder(orderNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/pick/orders/${orderNo}/claim`, method: 'POST' })
}

/** 契约：GET /api/v1/station/queue（单个看板对象，师傅只属一个站点） */
export function fetchStationQueue(): Promise<StationQueueVO> {
  return request<StationQueueVO>({ url: '/api/v1/station/queue', method: 'GET' })
}

/** 契约：POST /api/v1/station/orders/{orderNo}/arrive（TO_STATION → WASHING） */
export function arriveStation(orderNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/station/orders/${orderNo}/arrive`, method: 'POST' })
}

/** 契约：POST /api/v1/station/orders/{orderNo}/sop-done（WASHING → QC） */
export function sopDone(orderNo: string, steps?: string[]): Promise<void> {
  return request<void>({
    url: `/api/v1/station/orders/${orderNo}/sop-done`,
    method: 'POST',
    data: { steps: steps ?? [] },
  })
}

/** 契约：POST /api/v1/station/orders/{orderNo}/qc-pass（QC → WAIT_RETURN，必带洗后照） */
export function qcPass(orderNo: string, fileIds: string[]): Promise<void> {
  return request<void>({
    url: `/api/v1/station/orders/${orderNo}/qc-pass`,
    method: 'POST',
    data: { fileIds },
  })
}

/** 契约：POST /api/v1/station/orders/{orderNo}/qc-fail（QC → WASHING，原因必填） */
export function qcFail(orderNo: string, reason: string): Promise<void> {
  return request<void>({
    url: `/api/v1/station/orders/${orderNo}/qc-fail`,
    method: 'POST',
    data: { reason },
  })
}

/** 契约：POST /api/v1/station/orders/{orderNo}/leave（WAIT_RETURN → RETURNING） */
export function leaveStation(orderNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/station/orders/${orderNo}/leave`, method: 'POST' })
}
