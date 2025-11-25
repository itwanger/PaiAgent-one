package com.paiagent.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工作流响应 DTO
 */
@Data
public class WorkflowResponse {
    
    private Long id;
    private String name;
    private String description;
    private String flowData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
