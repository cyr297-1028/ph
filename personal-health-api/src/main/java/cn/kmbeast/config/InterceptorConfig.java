package cn.kmbeast.config;

import cn.kmbeast.Interceptor.JwtInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * 拦截器配置
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Resource
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**") // 拦截所有请求
                // ↓↓↓↓↓↓ 重点：在这里添加放行路径 ↓↓↓↓↓↓
                .excludePathPatterns(
                        "/user/login",       // 账号登录
                        "/user/register",    // 用户注册
                        "/user/faceLogin",   // 刷脸登录 (必须放行，因为还没Token)
                        "/user/addFace",     // 人脸录入 (必须放行，注册时还没Token)
                        "/file/**",          // 文件访问
                        "/doc.html",         // Swagger文档
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/news/**",
                        "/category/**"
                );
    }
}