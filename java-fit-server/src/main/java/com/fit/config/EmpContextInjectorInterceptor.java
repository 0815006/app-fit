package com.fit.config;

import cn.dev33.satoken.stp.StpUtil;
import com.fit.common.EmpContext;
import com.fit.entity.User;
import com.fit.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * EmpContext 注入拦截器
 * 优先从 Sa-Token 当前登录用户推导 empNo，
 * 降级从 X-Emp-No 请求头读取。
 */
@Slf4j
@RequiredArgsConstructor
public class EmpContextInjectorInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        try {
            // 1. 优先从 Sa-Token 获取当前登录用户 ID，推导 empNo
            long userId = StpUtil.getLoginIdAsLong();
            User user = userService.getById(userId);
            if (user != null && user.getEmpNo() != null) {
                EmpContext.setEmpNo(user.getEmpNo());
                return true;
            }
        } catch (Exception e) {
            // Sa-Token 未登录，降级
            log.debug("Sa-Token 未登录，降级读取 X-Emp-No 头");
        }

        // 2. 降级：从 X-Emp-No 请求头读取
        String empNo = request.getHeader("X-Emp-No");
        EmpContext.setEmpNo(empNo);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        EmpContext.clear();
    }
}
