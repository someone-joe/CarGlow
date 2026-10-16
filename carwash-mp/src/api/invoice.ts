import { request } from '@/utils/request'

/** 契约：openapi.yaml /api/v1/invoices** */
export interface InvoiceVO {
  invoiceId: number
  memberId: number
  orderNo: string
  amount: number
  titleType: string
  title: string
  taxNo?: string
  email: string
  status: 'APPLYING' | 'ISSUED' | 'REJECTED'
  statusLabel: string
  invoiceNo?: string
  rejectReason?: string
  remark?: string
  createTime: string
}

export interface ApplyInvoiceRequest {
  orderNo: string
  titleType?: string
  title: string
  taxNo?: string
  email: string
  remark?: string
}

export function applyInvoice(data: ApplyInvoiceRequest): Promise<number> {
  return request<number>({ url: '/api/v1/invoices', method: 'POST', data })
}

export function fetchInvoices(): Promise<InvoiceVO[]> {
  return request<InvoiceVO[]>({ url: '/api/v1/invoices', method: 'GET' })
}

export function fetchInvoice(invoiceId: number): Promise<InvoiceVO> {
  return request<InvoiceVO>({ url: `/api/v1/invoices/${invoiceId}`, method: 'GET' })
}
