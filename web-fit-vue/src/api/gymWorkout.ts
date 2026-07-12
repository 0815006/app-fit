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
  weight: number | null
  reps: number | null
  setCount: number | null
  rmEstimate: number | null
  isPr: boolean
  exhaustionScore: number
  dayOfWeek: number
}

/** POST /api/gym-workout/start */
export function startWorkout(actionId: string): Promise<ApiResult<string>> {
  return request.post('/gym-workout/start', { actionId })
}

/** 结束训练请求参数 */
export interface EndWorkoutParams {
  weight?: number | null
  reps?: number | null
  setCount?: number | null
  exhaustionScore: number
}

/** PUT /api/gym-workout/{id}/end */
export function endWorkout(recordId: string, params: EndWorkoutParams): Promise<ApiResult<null>> {
  return request.put(`/gym-workout/${recordId}/end`, params)
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

/** 补打卡请求参数 */
export interface MakeupWorkoutParams {
  actionId: string
  startTime: string
  exhaustionScore: number
  weight?: number | null
  reps?: number | null
  setCount?: number | null
}

/** POST /api/gym-workout/makeup */
export function makeupWorkout(
  actionId: string,
  startTime: string,
  exhaustionScore: number,
  weight?: number | null,
  reps?: number | null,
  setCount?: number | null
): Promise<ApiResult<null>> {
  return request.post('/gym-workout/makeup', { actionId, startTime, exhaustionScore, weight, reps, setCount })
}

/** GET /api/gym-workout/timeout-check */
export function checkTimeout(): Promise<ApiResult<TimeoutRecordVO | null>> {
  return request.get('/gym-workout/timeout-check')
}

/** GET /api/gym-workout/weekly-summary */
export function getWeeklySummary(): Promise<ApiResult<WeeklyWorkoutVO[]>> {
  return request.get('/gym-workout/weekly-summary')
}

/** GET /api/gym-workout/checkin-dates — 半年打卡日期列表 YYYY-MM-DD */
export function getCheckinDates(): Promise<ApiResult<string[]>> {
  return request.get('/gym-workout/checkin-dates')
}
