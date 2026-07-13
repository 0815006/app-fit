package com.fit.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.fit.common.EmpContext;
import com.fit.entity.User;
import com.fit.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置 —— Sa-Token 拦截器 + EmpContext 融合
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UserService userService;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // 1. Sa-Token 路由拦截器：校验登录态，放行登录接口
        registry.addInterceptor(new SaInterceptor(handle -> {
                    SaRouter.match("/api/**")
                            .notMatch("/api/auth/web-login", "/api/auth/wx-login")
                            .check(r -> StpUtil.checkLogin());
                }))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/web-login", "/api/auth/wx-login");

        // 2. EmpContext 注入拦截器：从 Sa-Token 或 Header 获取 empNo
        registry.addInterceptor(new EmpContextInjectorInterceptor(userService))
                .addPathPatterns("/api/**")
                // 排除登录接口（登录时还没有session）
                .excludePathPatterns("/api/auth/web-login", "/api/auth/wx-login");
    }
}
