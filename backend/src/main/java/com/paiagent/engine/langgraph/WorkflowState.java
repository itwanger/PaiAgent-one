package com.paiagent.engine.langgraph;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * LangGraph 工作流状态模型
 * 
 * 用于在 LangGraph 节点之间传递状态信息
 * 包含节点输出、执行上下文、状态跟踪等信息
 */
@Data
public class WorkflowState {
    
    /**
     * 当前正在执行的节点 ID
     */
    private String currentNodeId;
    
    /**
     * 全局上下文数据
     * 存储整个工作流共享的数据
     */
    private Map<String, Object> globalContext = new HashMap<>();
    
    /**
     * 节点输出映射
     * Key: 节点ID
     * Value: 节点输出数据
     */
    private Map<String, NodeOutput> nodeOutputs = new HashMap<>();
    
    /**
     * 工作流执行状态
     */
    private String status = "RUNNING";
    
    /**
     * 错误信息（如果执行失败）
     */
    private String errorMessage;
    
    /**
     * 工作流开始时间（毫秒时间戳）
     */
    private Long startTime;
    
    /**
     * 原始输入数据
     */
    private String inputData;
    
    /**
     * 更新节点输出
     * 
     * @param nodeId 节点ID
     * @param output 输出数据
     * @param status 执行状态（SUCCESS/FAILED）
     */
    public void updateNodeOutput(String nodeId, Map<String, Object> output, String status) {
        NodeOutput nodeOutput = new NodeOutput();
        nodeOutput.setNodeId(nodeId);
        nodeOutput.setOutput(output);
        nodeOutput.setStatus(status);
        nodeOutput.setTimestamp(System.currentTimeMillis());
        
        this.nodeOutputs.put(nodeId, nodeOutput);
        this.currentNodeId = nodeId;
    }
    
    /**
     * 获取指定节点的输出
     * 
     * @param nodeId 节点ID
     * @return 节点输出数据，如果节点未执行则返回 null
     */
    public Map<String, Object> getNodeOutput(String nodeId) {
        NodeOutput nodeOutput = nodeOutputs.get(nodeId);
        return nodeOutput != null ? nodeOutput.getOutput() : null;
    }
    
    /**
     * 获取前一个节点的输出
     * 
     * @return 前一个节点的输出数据，如果没有则返回空 Map
     */
    public Map<String, Object> getPreviousNodeOutput() {
        if (currentNodeId == null || nodeOutputs.isEmpty()) {
            return new HashMap<>();
        }
        
        NodeOutput current = nodeOutputs.get(currentNodeId);
        if (current != null && current.getOutput() != null) {
            return current.getOutput();
        }
        
        return new HashMap<>();
    }
    
    /**
     * 节点输出信息
     */
    @Data
    public static class NodeOutput {
        /**
         * 节点ID
         */
        private String nodeId;
        
        /**
         * 输出数据
         */
        private Map<String, Object> output;
        
        /**
         * 执行状态（SUCCESS/FAILED）
         */
        private String status;
        
        /**
         * 执行时间戳
         */
        private Long timestamp;
    }
}
