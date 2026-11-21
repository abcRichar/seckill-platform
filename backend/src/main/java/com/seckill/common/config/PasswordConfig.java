package com.seckill.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 单独放 PasswordEncoder，避免 WebConfig 同时依赖 AuthInterceptor、
 * 而 AuthInterceptor 又间接依赖 PasswordEncoder 造成循环依赖。
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
