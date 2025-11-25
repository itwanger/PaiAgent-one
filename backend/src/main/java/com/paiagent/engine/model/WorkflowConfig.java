package com.paiagent.engine.model;

import lombok.Data;
import java.util.List;

/**
 * 工作流配置模型
 */
@Data
public class WorkflowConfig {
    
    /**
     * 节点列表
     */
    private List<WorkflowNode> nodes;
    
    /**
     * 连线列表
     */
    private List<WorkflowEdge> edges;
}
