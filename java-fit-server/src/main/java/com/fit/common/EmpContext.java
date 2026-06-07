package com.fit.common;

/**
 * ThreadLocal-based employee number context.
 * Reads from X-Emp-No request header, defaults to "0000000".
 */
public final class EmpContext {

    private static final String DEFAULT_EMP_NO = "0000000";
    private static final ThreadLocal<String> EMP_NO_HOLDER = new ThreadLocal<>();

    private EmpContext() {
    }

    public static void setEmpNo(String empNo) {
        EMP_NO_HOLDER.set(empNo != null && !empNo.isBlank() ? empNo : DEFAULT_EMP_NO);
    }

    public static String getEmpNo() {
        String empNo = EMP_NO_HOLDER.get();
        return empNo != null ? empNo : DEFAULT_EMP_NO;
    }

    public static void clear() {
        EMP_NO_HOLDER.remove();
    }
}
