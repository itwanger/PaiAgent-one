package com.paiagent.engine.executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行器工厂
 */
@Component
public class NodeExecutorFactory {
    
    private final Map<String, NodeExecutor> executors = new HashMap<>();
    
    @Autowired
    public NodeExecutorFactory(List<NodeExecutor> executorList) {
        for (NodeExecutor executor : executorList) {
            executors.put(executor.getSupportedNodeType(), executor);
        }
    }
    
    /**
     * 根据节点类型获取执行器
     */
    public NodeExecutor getExecutor(String nodeType) {
        NodeExecutor executor = executors.get(nodeType);
        if (executor == null) {
            throw new RuntimeException("不支持的节点类型: " + nodeType);
        }
        return executor;
    }
}
