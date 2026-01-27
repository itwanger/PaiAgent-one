package com.paiagent.engine;

import com.paiagent.dto.ExecutionEvent;
import com.paiagent.dto.ExecutionResponse;
import com.paiagent.entity.Workflow;

import java.util.function.Consumer;

/**
 * 工作流执行器接口
 * 
 * 定义统一的工作流执行接口，支持多种执行引擎实现
 * 包括旧的 DAG 引擎和新的 LangGraph 引擎
 */
public interface WorkflowExecutor {
    
    /**
     * 执行工作流（无回调）
     * 
     * @param workflow 工作流定义
     * @param inputData 输入数据
     * @return 执行结果
     */
    ExecutionResponse execute(Workflow workflow, String inputData);
    
    /**
     * 执行工作流（带事件回调）
     * 
     * @param workflow 工作流定义
     * @param inputData 输入数据
     * @param eventCallback 事件回调函数（用于 SSE 推送）
     * @return 执行结果
     */
    ExecutionResponse executeWithCallback(
        Workflow workflow, 
        String inputData, 
        Consumer<ExecutionEvent> eventCallback
    );
    
    /**
     * 获取引擎类型
     * 
     * @return 引擎类型标识（如 "dag", "langgraph"）
     */
    default String getEngineType() {
        return "unknown";
    }
}
