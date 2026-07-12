import request from '@/utils/request'
import type { ApiResult } from './loginRecord'

/** 榜单统一 VO */
export interface RankingItemVO {
  rank: number
  userId: string
  empName: string
  empNo: string
  avatarUrl: string
  value: number
  auxiliaryValue?: number
  auxiliaryValue2?: number
  trend?: string
}

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
export function compareStrength(userId2: string): Promise<ApiResult<Record<string, any>>> {
  return request.get('/training-stats/compare', { params: { userId2 } })
}

// ═══════════════════════════════════════════════════════════
// V2 榜单（基于 gym_workout_record，返回 RankingItemVO）
// ═══════════════════════════════════════════════════════════

/** 坚持榜 V2：周期内累计打卡天数排名 */
export function getConsistencyRankingV2(days: number = 30): Promise<ApiResult<RankingItemVO[]>> {
  return request.get('/training-stats/ranking/consistency-v2', { params: { days } })
}

/** 容量榜：周期内训练总容量排名 */
export function getVolumeRanking(days: number = 30): Promise<ApiResult<RankingItemVO[]>> {
  return request.get('/training-stats/ranking/volume', { params: { days } })
}

/** 1RM巅峰榜：单次最大 1RM 排行（深蹲/卧推/硬拉/三大项之和） */
export function getPeak1RMRanking(
  days: number = 30,
  lift: string = 'bench'
): Promise<ApiResult<RankingItemVO[]>> {
  return request.get('/training-stats/ranking/peak-1rm', { params: { days, lift } })
}

/** 进步榜 V2：单次最大 1RM 增长率排名（深蹲/卧推/硬拉/三大项之和） */
export function getProgressRankingV2(
  days: number = 30,
  lift: string = 'bench'
): Promise<ApiResult<RankingItemVO[]>> {
  return request.get('/training-stats/ranking/progress-v2', { params: { days, lift } })
}

/** 容量榜：单次最大容量排行（深蹲/卧推/硬拉/三大项之和） */
export function getMaxSingleVolumeRanking(
  days: number = 30,
  lift: string = 'bench'
): Promise<ApiResult<RankingItemVO[]>> {
  return request.get('/training-stats/ranking/max-single-volume', { params: { days, lift } })
}
