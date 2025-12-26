package com.paiagent.engine.executor;

import com.paiagent.dto.ExecutionEvent;
import com.paiagent.engine.model.WorkflowNode;

import java.util.Map;
import java.util.function.Consumer;

public interface NodeExecutor {
    
    Map<String, Object> execute(WorkflowNode node, Map<String, Object> input) throws Exception;
    
    default Map<String, Object> execute(WorkflowNode node, Map<String, Object> input, Consumer<ExecutionEvent> progressCallback) throws Exception {
        return execute(node, input);
    }
    
    String getSupportedNodeType();
}