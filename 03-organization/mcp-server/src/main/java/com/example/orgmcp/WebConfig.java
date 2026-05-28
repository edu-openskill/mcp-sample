package com.example.orgmcp;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthForwardInterceptor authForwardInterceptor;

    public WebConfig(AuthForwardInterceptor authForwardInterceptor) {
        this.authForwardInterceptor = authForwardInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authForwardInterceptor);  // 모든 path
    }
}
