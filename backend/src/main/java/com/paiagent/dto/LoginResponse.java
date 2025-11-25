package com.paiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * 访问令牌
     */
    private String token;
    
    /**
     * 用户信息
     */
    private UserInfo user;
    
    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private String username;
    }
}
