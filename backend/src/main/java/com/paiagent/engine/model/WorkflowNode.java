package com.paiagent.engine.model;

import lombok.Data;
import java.util.Map;

/**
 * 工作流节点模型
 */
@Data
public class WorkflowNode {
    
    /**
     * 节点 ID
     */
    private String id;
    
    /**
     * 节点类型
     */
    private String type;
    
    /**
     * 节点位置
     */
    private Position position;
    
    /**
     * 节点配置数据
     */
    private Map<String, Object> data;
    
    @Data
    public static class Position {
        private Double x;
        private Double y;
    }
}
