package com.paiagent.common;

import lombok.Data;

/**
 * 统一响应结果类
 */
@Data
public class Result<T> {
    
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 成功响应码
     */
    public static final Integer SUCCESS_CODE = 200;
    
    /**
     * 失败响应码
     */
    public static final Integer ERROR_CODE = 500;
    
    /**
     * 未认证响应码
     */
    public static final Integer UNAUTHORIZED_CODE = 401;
    
    /**
     * 成功
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(SUCCESS_CODE);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }
    
    /**
     * 成功
     */
    public static <T> Result<T> success() {
        return success(null);
    }
    
    /**
     * 失败
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(ERROR_CODE);
        result.setMessage(message);
        return result;
    }
    
    /**
     * 未认证
     */
    public static <T> Result<T> unauthorized(String message) {
        Result<T> result = new Result<>();
        result.setCode(UNAUTHORIZED_CODE);
        result.setMessage(message);
        return result;
    }
}
