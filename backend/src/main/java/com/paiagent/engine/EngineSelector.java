package com.paiagent.engine;

import com.paiagent.entity.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工作流引擎选择器
 * 
 * 根据工作流配置动态选择合适的执行引擎
 * 支持 DAG 引擎和 LangGraph 引擎
 */
@Slf4j
@Component
public class EngineSelector {
    
    @Autowired
    private List<WorkflowExecutor> executors;
    
    /**
     * 选择合适的执行引擎
     * 
     * @param workflow 工作流定义
     * @return 执行引擎实例
     */
    public WorkflowExecutor selectEngine(Workflow workflow) {
        // 获取工作流引擎类型（默认为 "dag"）
        String engineType = getEngineType(workflow);
        
        log.info("为工作流 [{}] 选择引擎: {}", workflow.getName(), engineType);
        
        // 根据引擎类型查找对应的执行器
        for (WorkflowExecutor executor : executors) {
            if (executor.getEngineType().equals(engineType)) {
                log.info("找到匹配的引擎: {}", executor.getClass().getSimpleName());
                return executor;
            }
        }
        
        // 默认返回 DAG 引擎（向后兼容）
        log.warn("未找到引擎类型 [{}]，使用默认 DAG 引擎", engineType);
        return executors.stream()
                .filter(e -> "dag".equals(e.getEngineType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("未找到可用的工作流引擎"));
    }
    
    /**
     * 从工作流中获取引擎类型
     * 
     * @param workflow 工作流定义
     * @return 引擎类型（"dag" 或 "langgraph"）
     */
    private String getEngineType(Workflow workflow) {
        String engineType = workflow.getEngineType();
        
        // 如果未设置或为空，默认使用 DAG 引擎（向后兼容）
        if (engineType == null || engineType.trim().isEmpty()) {
            log.debug("工作流 [{}] 未指定引擎类型，使用默认 DAG 引擎", workflow.getName());
            return "dag";
        }
        
        return engineType.toLowerCase().trim();
    }
}
