package com.paiagent.engine.langgraph.adapter;

import com.paiagent.dto.ExecutionEvent;
import com.paiagent.engine.executor.NodeExecutor;
import com.paiagent.engine.executor.NodeExecutorFactory;
import com.paiagent.engine.langgraph.WorkflowState;
import com.paiagent.engine.model.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 节点适配器
 * 
 * 将现有的 NodeExecutor 适配为 LangGraph NodeAction
 * 实现节点执行逻辑与 LangGraph 状态模型的桥接
 */
@Slf4j
@Component
public class NodeAdapter {
    
    @Autowired
    private NodeExecutorFactory executorFactory;
    
    /**
     * 将 WorkflowNode 适配为 LangGraph AsyncNodeAction
     * 
     * @param node 工作流节点定义
     * @param eventCallback 事件回调函数（可选）
     * @return LangGraph AsyncNodeAction
     */
    public AsyncNodeAction<AgentState> adaptNode(WorkflowNode node, Consumer<ExecutionEvent> eventCallback) {
        
        return (AgentState state) -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 触发节点开始事件
                if (eventCallback != null) {
                    eventCallback.accept(ExecutionEvent.nodeStart(node.getId(), node.getType()));
                }
                
                // 获取节点执行器
                NodeExecutor executor = executorFactory.getExecutor(node.getType());
                
                // 从状态中提取当前输入
                Map<String, Object> stateData = state.data();
                @SuppressWarnings("unchecked")
                Map<String, Object> currentInput = (Map<String, Object>) stateData.getOrDefault("currentInput", new HashMap<>());
                
                // 为 Output 节点注入 nodeOutputs 以便访问所有节点的输出
                if ("output".equals(node.getType())) {
                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, Object>> nodeOutputs = 
                        (Map<String, Map<String, Object>>) stateData.getOrDefault("nodeOutputs", new HashMap<>());
                    currentInput = new HashMap<>(currentInput);
                    currentInput.put("__nodeOutputs__", nodeOutputs);
                }
                
                // 执行节点
                Map<String, Object> output = executor.execute(node, currentInput, eventCallback);
                
                // 更新状态
                Map<String, Object> newStateData = new HashMap<>(stateData);
                
                // 保存节点输出
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> nodeOutputs = 
                    (Map<String, Map<String, Object>>) newStateData.getOrDefault("nodeOutputs", new HashMap<>());
                nodeOutputs.put(node.getId(), output);
                newStateData.put("nodeOutputs", nodeOutputs);
                
                // 更新当前输入为本节点输出（传递给下一个节点）
                newStateData.put("currentInput", output);
                newStateData.put("currentNodeId", node.getId());
                
                // 触发节点成功事件
                if (eventCallback != null) {
                    long duration = System.currentTimeMillis() - startTime;
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("input", currentInput);
                    eventData.put("output", output);
                    eventData.put("duration", duration);
                    eventCallback.accept(ExecutionEvent.nodeSuccess(node.getId(), node.getType(), eventData, (int) duration));
                }
                
                return CompletableFuture.completedFuture(newStateData);
                
            } catch (Exception e) {
                log.error("节点执行失败: {}", node.getId(), e);
                
                // 触发节点错误事件
                if (eventCallback != null) {
                    eventCallback.accept(ExecutionEvent.nodeError(node.getId(), node.getType(), e.getMessage()));
                }
                
                // 更新状态为失败
                Map<String, Object> errorState = new HashMap<>(state.data());
                errorState.put("status", "FAILED");
                errorState.put("errorMessage", "节点 " + node.getId() + " 执行失败: " + e.getMessage());
                
                return CompletableFuture.completedFuture(errorState);
            }
        };
    }
    
    /**
     * 批量适配多个节点
     * 
     * @param nodes 节点列表
     * @param eventCallback 事件回调
     * @return 节点ID到AsyncNodeAction的映射
     */
    public Map<String, AsyncNodeAction<AgentState>> adaptNodes(
            java.util.List<WorkflowNode> nodes, 
            Consumer<ExecutionEvent> eventCallback) {
        
        Map<String, AsyncNodeAction<AgentState>> adaptedNodes = new HashMap<>();
        
        for (WorkflowNode node : nodes) {
            adaptedNodes.put(node.getId(), adaptNode(node, eventCallback));
        }
        
        return adaptedNodes;
    }
}
