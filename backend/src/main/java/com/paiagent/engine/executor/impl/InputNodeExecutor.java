package com.paiagent.engine.executor.impl;

import com.paiagent.engine.executor.NodeExecutor;
import com.paiagent.engine.model.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 输入节点执行器
 */
@Component
public class InputNodeExecutor implements NodeExecutor {
    
    @Override
    public Map<String, Object> execute(WorkflowNode node, Map<String, Object> input) {
        // 输入节点直接返回输入数据
        return new HashMap<>(input);
    }
    
    @Override
    public String getSupportedNodeType() {
        return "input";
    }
}
