package com.paiagent.controller;

import com.paiagent.common.Result;
import com.paiagent.dto.WorkflowRequest;
import com.paiagent.dto.WorkflowResponse;
import com.paiagent.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流管理控制器
 */
@Tag(name = "工作流管理接口")
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    
    @Autowired
    private WorkflowService workflowService;
    
    @Operation(summary = "创建工作流")
    @PostMapping
    public Result<WorkflowResponse> createWorkflow(@Valid @RequestBody WorkflowRequest request) {
        WorkflowResponse response = workflowService.createWorkflow(request);
        return Result.success(response);
    }
    
    @Operation(summary = "更新工作流")
    @PutMapping("/{id}")
    public Result<WorkflowResponse> updateWorkflow(@PathVariable Long id, @Valid @RequestBody WorkflowRequest request) {
        WorkflowResponse response = workflowService.updateWorkflow(id, request);
        return Result.success(response);
    }
    
    @Operation(summary = "删除工作流")
    @DeleteMapping("/{id}")
    public Result<Void> deleteWorkflow(@PathVariable Long id) {
        workflowService.deleteWorkflow(id);
        return Result.success();
    }
    
    @Operation(summary = "获取工作流详情")
    @GetMapping("/{id}")
    public Result<WorkflowResponse> getWorkflow(@PathVariable Long id) {
        WorkflowResponse response = workflowService.getWorkflowById(id);
        return Result.success(response);
    }
    
    @Operation(summary = "查询工作流列表")
    @GetMapping
    public Result<List<WorkflowResponse>> listWorkflows() {
        List<WorkflowResponse> list = workflowService.listWorkflows();
        return Result.success(list);
    }
}
