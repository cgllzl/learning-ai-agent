package com.enterprise.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// 项目使用自定义 Bearer 会话，不启用 Spring Boot 自动生成的默认用户和随机密码。
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class EnterpriseAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseAgentApplication.class, args);
    }
}
