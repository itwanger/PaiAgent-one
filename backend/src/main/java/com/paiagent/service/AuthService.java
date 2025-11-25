package com.paiagent.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务
 */
@Service
public class AuthService {
    
    /**
     * 默认用户名
     */
    private static final String DEFAULT_USERNAME = "admin";
    
    /**
     * 默认密码
     */
    private static final String DEFAULT_PASSWORD = "123";
    
    /**
     * Token 存储(Token -> Username)
     */
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();
    
    /**
     * 用户登录
     */
    public String login(String username, String password) {
        if (DEFAULT_USERNAME.equals(username) && DEFAULT_PASSWORD.equals(password)) {
            String token = UUID.randomUUID().toString().replace("-", "");
            tokenStore.put(token, username);
            return token;
        }
        return null;
    }
    
    /**
     * 用户登出
     */
    public void logout(String token) {
        tokenStore.remove(token);
    }
    
    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        return tokenStore.containsKey(token);
    }
    
    /**
     * 获取 Token 对应的用户名
     */
    public String getUsernameByToken(String token) {
        return tokenStore.get(token);
    }
}
