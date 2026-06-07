package com.fit.config;

import com.fit.common.EmpInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final EmpInterceptor empInterceptor;

    public WebConfig(EmpInterceptor empInterceptor) {
        this.empInterceptor = empInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(empInterceptor)
                .addPathPatterns("/api/**");
    }
}
