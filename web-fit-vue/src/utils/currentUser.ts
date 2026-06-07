import { ref } from 'vue'

const EMP_NO_KEY = 'empNo'
const DEFAULT_EMP_NO = '0000000'

/** Reactive ref — components can watch this to react to user switches */
export const currentEmpNo = ref<string>(
  localStorage.getItem(EMP_NO_KEY) || DEFAULT_EMP_NO
)

export function getEmpNo(): string {
  return currentEmpNo.value
}

export function setEmpNo(empNo: string): void {
  currentEmpNo.value = empNo
  localStorage.setItem(EMP_NO_KEY, empNo)
}
