package com.paiagent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 工作流创建/更新请求 DTO
 */
@Data
public class WorkflowRequest {
    
    @NotBlank(message = "工作流名称不能为空")
    private String name;
    
    private String description;
    
    @NotBlank(message = "工作流配置数据不能为空")
    private String flowData;
    
    private String engineType;
}
