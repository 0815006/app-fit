import request from '@/utils/request'
import type { ApiResult } from './loginRecord'

// ============================================================
// 健身打卡 API
// ============================================================

export interface DashboardVO {
  readyMuscleNames: string[]
  muscleGroups: MuscleGroupStatusVO[]
  timeoutRecord: TimeoutRecordVO | null
}

export interface SubMuscleStatus {
  muscleCode: string
  muscleName: string
  trainedThisWeek: boolean
}

export interface MuscleGroupStatusVO {
  muscleGroup: string
  muscleGroupName: string
  weeklyCount: number
  status: 'READY' | 'RECOVERING'
  remainingSeconds: number
  subMuscles: SubMuscleStatus[]
}

export interface TimeoutRecordVO {
  recordId: string
  actionName: string
  startTime: string
  startTimeLabel: string
}

/** 本周训练摘要记录 */
export interface WeeklyWorkoutVO {
  actionName: string
  muscleGroup: string
  muscleGroupName: string
  startTime: string
  exhaustionScore: number
  dayOfWeek: number
}

/** POST /api/gym-workout/start */
export function startWorkout(actionId: string): Promise<ApiResult<string>> {
  return request.post('/gym-workout/start', { actionId })
}

/** PUT /api/gym-workout/{id}/end */
export function endWorkout(recordId: string, exhaustionScore: number): Promise<ApiResult<null>> {
  return request.put(`/gym-workout/${recordId}/end`, { exhaustionScore })
}

/** PUT /api/gym-workout/{id}/correct */
export function correctTimeout(
  recordId: string,
  actualMinutes: number,
  exhaustionScore: number
): Promise<ApiResult<null>> {
  return request.put(`/gym-workout/${recordId}/correct`, { actualMinutes, exhaustionScore })
}

/** GET /api/gym-workout/dashboard */
export function getDashboard(): Promise<ApiResult<DashboardVO>> {
  return request.get('/gym-workout/dashboard')
}

/** POST /api/gym-workout/makeup */
export function makeupWorkout(
  actionId: string,
  startTime: string,
  exhaustionScore: number
): Promise<ApiResult<null>> {
  return request.post('/gym-workout/makeup', { actionId, startTime, exhaustionScore })
}

/** GET /api/gym-workout/timeout-check */
export function checkTimeout(): Promise<ApiResult<TimeoutRecordVO | null>> {
  return request.get('/gym-workout/timeout-check')
}

/** GET /api/gym-workout/weekly-summary */
export function getWeeklySummary(): Promise<ApiResult<WeeklyWorkoutVO[]>> {
  return request.get('/gym-workout/weekly-summary')
}
