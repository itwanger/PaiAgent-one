package com.paiagent.controller;

import com.paiagent.common.Result;
import com.paiagent.dto.ExecutionRequest;
import com.paiagent.dto.ExecutionResponse;
import com.paiagent.engine.WorkflowEngine;
import com.paiagent.entity.Workflow;
import com.paiagent.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 工作流执行控制器
 */
@Tag(name = "工作流执行接口")
@RestController
@RequestMapping("/api/workflows")
public class ExecutionController {
    
    @Autowired
    private WorkflowService workflowService;
    
    @Autowired
    private WorkflowEngine workflowEngine;
    
    @Operation(summary = "执行工作流")
    @PostMapping("/{id}/execute")
    public Result<ExecutionResponse> executeWorkflow(@PathVariable Long id, @Valid @RequestBody ExecutionRequest request) {
        // 获取工作流
        Workflow workflow = workflowService.getById(id);
        if (workflow == null) {
            return Result.error("工作流不存在");
        }
        
        try {
            // 执行工作流
            ExecutionResponse response = workflowEngine.execute(workflow, request.getInputData());
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("工作流执行失败: " + e.getMessage());
        }
    }
}
