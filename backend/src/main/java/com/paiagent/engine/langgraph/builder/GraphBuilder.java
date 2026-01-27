package com.paiagent.engine.langgraph.builder;

import com.paiagent.dto.ExecutionEvent;
import com.paiagent.engine.langgraph.adapter.NodeAdapter;
import com.paiagent.engine.model.WorkflowConfig;
import com.paiagent.engine.model.WorkflowEdge;
import com.paiagent.engine.model.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 图构建器
 * 
 * 将工作流配置转换为 LangGraph StateGraph
 * 负责节点注册、边添加、入口出口设置
 */
@Slf4j
@Component
public class GraphBuilder {
    
    @Autowired
    private NodeAdapter nodeAdapter;
    
    /**
     * 构建 LangGraph StateGraph
     * 
     * @param config 工作流配置
     * @param eventCallback 事件回调（可选）
     * @return 编译后的 StateGraph
     * @throws Exception 图构建异常
     */
    public org.bsc.langgraph4j.CompiledGraph<AgentState> buildGraph(
            WorkflowConfig config, 
            Consumer<ExecutionEvent> eventCallback) throws Exception {
        
        log.info("开始构建 LangGraph: 节点数={}, 边数={}", 
            config.getNodes().size(), config.getEdges().size());
        
        // 创建 StateGraph
        StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);
        
        // 添加所有节点
        addNodes(graph, config.getNodes(), eventCallback);
        
        // 添加所有边
        addEdges(graph, config.getEdges());
        
        // 设置入口和出口
        setEntryAndExit(graph, config.getNodes(), config.getEdges());
        
        // 编译图
        var compiled = graph.compile();
        
        log.info("LangGraph 构建完成");
        return compiled;
    }
    
    /**
     * 添加节点到图中
     */
    private void addNodes(
            StateGraph<AgentState> graph, 
            List<WorkflowNode> nodes,
            Consumer<ExecutionEvent> eventCallback) throws Exception {
        
        for (WorkflowNode node : nodes) {
            log.debug("添加节点: id={}, type={}", node.getId(), node.getType());
            
            // 使用 NodeAdapter 将节点适配为 LangGraph NodeAction
            var nodeAction = nodeAdapter.adaptNode(node, eventCallback);
            
            graph.addNode(node.getId(), nodeAction);
        }
    }
    
    /**
     * 添加边到图中
     */
    private void addEdges(StateGraph<AgentState> graph, List<WorkflowEdge> edges) throws Exception {
        for (WorkflowEdge edge : edges) {
            log.debug("添加边: {} -> {}", edge.getSource(), edge.getTarget());
            graph.addEdge(edge.getSource(), edge.getTarget());
        }
    }
    
    /**
     * 设置图的入口和出口节点
     */
    private void setEntryAndExit(
            StateGraph<AgentState> graph,
            List<WorkflowNode> nodes,
            List<WorkflowEdge> edges) throws Exception {
        
        // 找到入口节点（没有前置节点的节点）
        WorkflowNode entryNode = findEntryNode(nodes, edges);
        if (entryNode != null) {
            log.info("设置入口节点: {}", entryNode.getId());
            graph.addEdge(StateGraph.START, entryNode.getId());
        } else {
            log.warn("未找到入口节点，使用第一个节点作为入口");
            if (!nodes.isEmpty()) {
                graph.addEdge(StateGraph.START, nodes.get(0).getId());
            }
        }
        
        // 找到出口节点（没有后继节点的节点）
        WorkflowNode exitNode = findExitNode(nodes, edges);
        if (exitNode != null) {
            log.info("设置出口节点: {}", exitNode.getId());
            graph.addEdge(exitNode.getId(), StateGraph.END);
        } else {
            log.warn("未找到出口节点，使用最后一个节点作为出口");
            if (!nodes.isEmpty()) {
                graph.addEdge(nodes.get(nodes.size() - 1).getId(), StateGraph.END);
            }
        }
    }
    
    /**
     * 查找入口节点（没有前置节点）
     */
    private WorkflowNode findEntryNode(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        for (WorkflowNode node : nodes) {
            boolean hasIncomingEdge = edges.stream()
                .anyMatch(edge -> edge.getTarget().equals(node.getId()));
            
            if (!hasIncomingEdge) {
                return node;
            }
        }
        return null;
    }
    
    /**
     * 查找出口节点（没有后继节点）
     */
    private WorkflowNode findExitNode(List<WorkflowNode> nodes, List<WorkflowEdge> edges) {
        for (WorkflowNode node : nodes) {
            boolean hasOutgoingEdge = edges.stream()
                .anyMatch(edge -> edge.getSource().equals(node.getId()));
            
            if (!hasOutgoingEdge) {
                return node;
            }
        }
        return null;
    }
}
