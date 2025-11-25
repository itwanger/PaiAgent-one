package com.paiagent.controller;

import com.paiagent.common.Result;
import com.paiagent.dto.LoginRequest;
import com.paiagent.dto.LoginResponse;
import com.paiagent.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());
        if (token != null) {
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(request.getUsername());
            LoginResponse response = new LoginResponse(token, userInfo);
            return Result.success(response);
        }
        return Result.error("用户名或密码错误");
    }
    
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            authService.logout(token);
        }
        return Result.success();
    }
    
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/current")
    public Result<LoginResponse.UserInfo> getCurrentUser(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            String username = authService.getUsernameByToken(token);
            if (username != null) {
                return Result.success(new LoginResponse.UserInfo(username));
            }
        }
        return Result.unauthorized("未认证");
    }
}
