package com.paiagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.paiagent.dto.WorkflowRequest;
import com.paiagent.dto.WorkflowResponse;
import com.paiagent.entity.Workflow;
import com.paiagent.mapper.WorkflowMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流服务
 */
@Service
public class WorkflowService extends ServiceImpl<WorkflowMapper, Workflow> {
    
    /**
     * 创建工作流
     */
    public WorkflowResponse createWorkflow(WorkflowRequest request) {
        Workflow workflow = new Workflow();
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setFlowData(request.getFlowData());
        workflow.setEngineType(request.getEngineType());
        
        this.save(workflow);
        
        return toResponse(workflow);
    }
    
    /**
     * 更新工作流
     */
    public WorkflowResponse updateWorkflow(Long id, WorkflowRequest request) {
        Workflow workflow = this.getById(id);
        if (workflow == null) {
            throw new RuntimeException("工作流不存在");
        }
        
        workflow.setName(request.getName());
        workflow.setDescription(request.getDescription());
        workflow.setFlowData(request.getFlowData());
        workflow.setEngineType(request.getEngineType());
        
        this.updateById(workflow);
        
        return toResponse(workflow);
    }
    
    /**
     * 删除工作流
     */
    public void deleteWorkflow(Long id) {
        this.removeById(id);
    }
    
    /**
     * 获取工作流详情
     */
    public WorkflowResponse getWorkflowById(Long id) {
        Workflow workflow = this.getById(id);
        if (workflow == null) {
            throw new RuntimeException("工作流不存在");
        }
        return toResponse(workflow);
    }
    
    /**
     * 查询工作流列表
     */
    public List<WorkflowResponse> listWorkflows() {
        LambdaQueryWrapper<Workflow> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Workflow::getUpdatedAt);
        
        List<Workflow> workflows = this.list(wrapper);
        return workflows.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 转换为响应 DTO
     */
    private WorkflowResponse toResponse(Workflow workflow) {
        WorkflowResponse response = new WorkflowResponse();
        BeanUtils.copyProperties(workflow, response);
        return response;
    }
}
