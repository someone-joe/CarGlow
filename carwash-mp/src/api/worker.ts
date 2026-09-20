import type { components } from '@/api/schema'
import { request } from '@/utils/request'

export type WorkerLoginVO = components['schemas']['WorkerLoginVO']
export type PickTaskVO = components['schemas']['PickTaskVO']
export type StationQueueVO = components['schemas']['StationQueueVO']

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

/** 契约：POST /api/v1/pick/orders/{orderNo}/take-key（KEY_IN → PICKING） */
export function takeKey(orderNo: string): Promise<void> {
  return request<void>({ url: `/api/v1/pick/orders/${orderNo}/take-key`, method: 'POST' })
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
