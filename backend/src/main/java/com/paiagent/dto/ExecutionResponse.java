package com.paiagent.dto;

import lombok.Data;
import java.util.List;

/**
 * 工作流执行响应 DTO
 */
@Data
public class ExecutionResponse {
    
    private Long executionId;
    private String status;
    private List<NodeResult> nodeResults;
    private String outputData;
    private Integer duration;
    
    @Data
    public static class NodeResult {
        private String nodeId;
        private String nodeName;
        private String status;
        private String input;
        private String output;
        private Integer duration;
        private String error;
    }
}
