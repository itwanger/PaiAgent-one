package com.paiagent.engine.model;

import lombok.Data;

/**
 * 工作流连线模型
 */
@Data
public class WorkflowEdge {
    
    /**
     * 连线 ID
     */
    private String id;
    
    /**
     * 源节点 ID
     */
    private String source;
    
    /**
     * 目标节点 ID
     */
    private String target;
    
    /**
     * 源节点输出端口
     */
    private String sourceHandle;
    
    /**
     * 目标节点输入端口
     */
    private String targetHandle;
}
