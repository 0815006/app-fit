package com.fit.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that reads the X-Emp-No header and stores it in EmpContext.
 */
@Slf4j
@Component
public class EmpInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String empNo = request.getHeader("X-Emp-No");
        EmpContext.setEmpNo(empNo);
        log.debug("EmpInterceptor: set empNo = {}", EmpContext.getEmpNo());
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
