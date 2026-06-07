const EMP_NO_KEY = 'empNo'
const DEFAULT_EMP_NO = '0000000'

export function getEmpNo(): string {
  return localStorage.getItem(EMP_NO_KEY) || DEFAULT_EMP_NO
}

export function setEmpNo(empNo: string): void {
  localStorage.setItem(EMP_NO_KEY, empNo)
}
