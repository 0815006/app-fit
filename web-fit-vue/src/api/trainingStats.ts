import request from '@/utils/request'
import type { ApiResult } from './loginRecord'

// 计算1RM
export function calculate1RM(weight: number, reps: number): Promise<ApiResult<number>> {
  return request.get('/training-stats/1rm', { params: { weight, reps } })
}

// 获取指定动作的历史最佳1RM
export function getBest1RM(actionId: string): Promise<ApiResult<number>> {
  return request.get(`/training-stats/best-1rm/${actionId}`)
}

// 获取指定动作的最近一次训练数据
export function getLastSession(actionId: string): Promise<ApiResult<Record<string, any>>> {
  return request.get(`/training-stats/last-session/${actionId}`)
}

// 获取肌群疲劳度
export function getMuscleFatigue(): Promise<ApiResult<Record<string, number>>> {
  return request.get('/training-stats/fatigue')
}

// 获取训练总容量趋势
export function getVolumeTrend(params: {
  groupBy?: string
  startDate?: string
  endDate?: string
}): Promise<ApiResult<Array<Record<string, any>>>> {
  return request.get('/training-stats/volume-trend', { params })
}

// 获取贡献墙数据
export function getContributionWall(year?: number): Promise<ApiResult<Array<Record<string, any>>>> {
  return request.get('/training-stats/contribution', { params: { year } })
}

// 坚持榜
export function getConsistencyRanking(days: number = 30): Promise<ApiResult<Array<Record<string, any>>>> {
  return request.get('/training-stats/ranking/consistency', { params: { days } })
}

// 进步榜
export function getProgressRanking(days: number = 30): Promise<ApiResult<Array<Record<string, any>>>> {
  return request.get('/training-stats/ranking/progress', { params: { days } })
}

// 平台期检测
export function detectPlateau(weeks: number = 6): Promise<ApiResult<Array<Record<string, any>>>> {
  return request.get('/training-stats/plateau', { params: { weeks } })
}

// 检查是否PR
export function checkPr(actionId: string, weight: number, reps: number): Promise<ApiResult<boolean>> {
  return request.get('/training-stats/check-pr', { params: { actionId, weight, reps } })
}

// 力量对比
export function compareStrength(empNo2: string): Promise<ApiResult<Record<string, any>>> {
  return request.get('/training-stats/compare', { params: { empNo2 } })
}
