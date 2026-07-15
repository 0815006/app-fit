package com.fit.common;

/**
 * ThreadLocal-based user context.
 * Stores userId from Sa-Token and empNo from user table / X-Emp-No header.
 */
public final class EmpContext {

    private static final String DEFAULT_EMP_NO = "0000000";
    private static final ThreadLocal<String> EMP_NO_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();

    private EmpContext() {
    }

    public static void setEmpNo(String empNo) {
        EMP_NO_HOLDER.set(empNo != null && !empNo.isBlank() ? empNo : DEFAULT_EMP_NO);
    }

    public static String getEmpNo() {
        String empNo = EMP_NO_HOLDER.get();
        return empNo != null ? empNo : DEFAULT_EMP_NO;
    }

    /** 设置当前用户主键 ID（来自 Sa-Token） */
    public static void setUserId(String userId) {
        USER_ID_HOLDER.set(userId);
    }

    /** 获取当前用户主键 ID */
    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void clear() {
        EMP_NO_HOLDER.remove();
        USER_ID_HOLDER.remove();
    }
}
