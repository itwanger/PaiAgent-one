package com.paiagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 工作流执行请求 DTO
 */
@Data
public class ExecutionRequest {
    
    @NotBlank(message = "输入数据不能为空")
    private String inputData;
}
