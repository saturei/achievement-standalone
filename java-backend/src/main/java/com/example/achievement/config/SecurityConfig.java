package com.example.achievement.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private Environment env;

    private boolean isDev() {
        return Arrays.asList(env.getActiveProfiles()).contains("sqlite")
                || Arrays.asList(env.getActiveProfiles()).contains("dev");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .csrf().disable()
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        if (isDev()) {
            // 开发/测试环境：允许所有 API 访问（兼容旧的 X-Current-User 模式）
            http.authorizeRequests()
                    .antMatchers("/**").permitAll()
                    .and()
                    .headers().frameOptions().disable();
        } else {
            // 生产环境：需要 JWT 认证
            http.authorizeRequests()
                    .antMatchers("/api/auth/**").permitAll()
                    .antMatchers("/api/**").authenticated()
                    .antMatchers("/h2-console/**").permitAll()
                    .antMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                    .anyRequest().authenticated()
                    .and()
                    .headers().frameOptions().disable();
        }
    }
}
