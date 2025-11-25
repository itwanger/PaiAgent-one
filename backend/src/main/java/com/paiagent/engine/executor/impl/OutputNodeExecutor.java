package com.paiagent.engine.executor.impl;

import com.paiagent.engine.executor.NodeExecutor;
import com.paiagent.engine.model.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 输出节点执行器
 */
@Component
public class OutputNodeExecutor implements NodeExecutor {
    
    @Override
    public Map<String, Object> execute(WorkflowNode node, Map<String, Object> input) {
        // 输出节点直接返回输入数据
        Map<String, Object> output = new HashMap<>();
        output.put("output", input.get("output") != null ? input.get("output") : input.get("input"));
        return output;
    }
    
    @Override
    public String getSupportedNodeType() {
        return "output";
    }
}
