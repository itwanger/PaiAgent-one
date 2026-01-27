package com.paiagent.engine.langgraph.state;

import com.paiagent.engine.langgraph.WorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 状态管理器
 * 
 * 负责 WorkflowState 的初始化、更新和提取
 * 提供与 LangGraph 状态模型的转换接口
 */
@Slf4j
@Component
public class StateManager {
    
    /**
     * 初始化工作流状态
     * 
     * @param inputData 原始输入数据
     * @return LangGraph 状态 Map
     */
    public Map<String, Object> initializeState(String inputData) {
        Map<String, Object> state = new HashMap<>();
        
        // 设置原始输入
        state.put("inputData", inputData);
        
        // 初始化当前输入（用于节点间传递）
        Map<String, Object> currentInput = new HashMap<>();
        currentInput.put("input", inputData);
        state.put("currentInput", currentInput);
        
        // 初始化节点输出映射
        Map<String, Map<String, Object>> nodeOutputs = new HashMap<>();
        state.put("nodeOutputs", nodeOutputs);
        
        // 初始化状态
        state.put("status", "RUNNING");
        state.put("startTime", System.currentTimeMillis());
        
        log.info("初始化工作流状态: inputData={}", inputData);
        return state;
    }
    
    /**
     * 从 LangGraph 状态提取 WorkflowState
     * 
     * @param langGraphState LangGraph 状态 Map
     * @return WorkflowState 对象
     */
    public WorkflowState extractWorkflowState(Map<String, Object> langGraphState) {
        WorkflowState workflowState = new WorkflowState();
        
        // 提取基本字段
        workflowState.setInputData((String) langGraphState.get("inputData"));
        workflowState.setStatus((String) langGraphState.getOrDefault("status", "RUNNING"));
        workflowState.setErrorMessage((String) langGraphState.get("errorMessage"));
        workflowState.setStartTime((Long) langGraphState.get("startTime"));
        workflowState.setCurrentNodeId((String) langGraphState.get("currentNodeId"));
        
        // 提取节点输出
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> nodeOutputs = 
            (Map<String, Map<String, Object>>) langGraphState.getOrDefault("nodeOutputs", new HashMap<>());
        
        for (Map.Entry<String, Map<String, Object>> entry : nodeOutputs.entrySet()) {
            workflowState.updateNodeOutput(entry.getKey(), entry.getValue(), "SUCCESS");
        }
        
        // 提取全局上下文
        @SuppressWarnings("unchecked")
        Map<String, Object> globalContext = 
            (Map<String, Object>) langGraphState.getOrDefault("globalContext", new HashMap<>());
        workflowState.setGlobalContext(globalContext);
        
        return workflowState;
    }
    
    /**
     * 获取最终输出数据
     * 
     * @param state LangGraph 状态
     * @return 最终输出数据（JSON 字符串）
     */
    public Map<String, Object> getFinalOutput(Map<String, Object> state) {
        // 获取当前输入（最后一个节点的输出）
        @SuppressWarnings("unchecked")
        Map<String, Object> currentInput = 
            (Map<String, Object>) state.getOrDefault("currentInput", new HashMap<>());
        
        return currentInput;
    }
    
    /**
     * 检查工作流是否执行成功
     * 
     * @param state LangGraph 状态
     * @return true 表示成功
     */
    public boolean isSuccessful(Map<String, Object> state) {
        String status = (String) state.getOrDefault("status", "RUNNING");
        return "SUCCESS".equals(status) || "RUNNING".equals(status);
    }
    
    /**
     * 获取错误信息
     * 
     * @param state LangGraph 状态
     * @return 错误信息，如果没有则返回 null
     */
    public String getErrorMessage(Map<String, Object> state) {
        return (String) state.get("errorMessage");
    }
    
    /**
     * 从状态中提取节点执行结果列表
     * 
     * @param state LangGraph 状态
     * @param config 工作流配置（用于获取节点名称）
     * @return 节点执行结果列表
     */
    @SuppressWarnings("unchecked")
    public java.util.List<Map<String, Object>> extractNodeResults(
            Map<String, Object> state, 
            com.paiagent.engine.model.WorkflowConfig config) {
        
        java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();
        
        // 获取 nodeOutputs
        Map<String, Map<String, Object>> nodeOutputs = 
            (Map<String, Map<String, Object>>) state.getOrDefault("nodeOutputs", new HashMap<>());
        
        if (nodeOutputs.isEmpty()) {
            return results;
        }
        
        // 创建节点 ID 到名称的映射
        Map<String, String> nodeIdToName = new HashMap<>();
        for (var node : config.getNodes()) {
            nodeIdToName.put(node.getId(), node.getType());
        }
        
        // 转换为 NodeResult 格式
        for (Map.Entry<String, Map<String, Object>> entry : nodeOutputs.entrySet()) {
            String nodeId = entry.getKey();
            Map<String, Object> output = entry.getValue();
            
            Map<String, Object> nodeResult = new HashMap<>();
            nodeResult.put("nodeId", nodeId);
            nodeResult.put("nodeName", nodeIdToName.getOrDefault(nodeId, nodeId));
            nodeResult.put("status", "SUCCESS");
            nodeResult.put("output", com.alibaba.fastjson2.JSON.toJSONString(output));
            
            results.add(nodeResult);
        }
        
        return results;
    }
}
