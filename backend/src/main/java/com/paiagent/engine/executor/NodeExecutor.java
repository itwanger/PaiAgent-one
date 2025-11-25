package com.paiagent.engine.executor;

import com.paiagent.engine.model.WorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 节点执行器接口
 */
public interface NodeExecutor {
    
    /**
     * 执行节点
     * 
     * @param node 节点配置
     * @param input 输入数据
     * @return 输出数据
     */
    Map<String, Object> execute(WorkflowNode node, Map<String, Object> input) throws Exception;
    
    /**
     * 获取支持的节点类型
     */
    String getSupportedNodeType();
}
