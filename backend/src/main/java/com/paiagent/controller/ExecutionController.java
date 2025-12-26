package com.paiagent.controller;

import com.paiagent.common.Result;
import com.paiagent.dto.ExecutionEvent;
import com.paiagent.dto.ExecutionRequest;
import com.paiagent.dto.ExecutionResponse;
import com.paiagent.engine.WorkflowEngine;
import com.paiagent.entity.Workflow;
import com.paiagent.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Tag(name = "工作流执行接口")
@RestController
@RequestMapping("/api/workflows")
public class ExecutionController {
    
    @Autowired
    private WorkflowService workflowService;
    
    @Autowired
    private WorkflowEngine workflowEngine;
    
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    @Operation(summary = "执行工作流")
    @PostMapping("/{id}/execute")
    public Result<ExecutionResponse> executeWorkflow(@PathVariable Long id, @Valid @RequestBody ExecutionRequest request) {
        Workflow workflow = workflowService.getById(id);
        if (workflow == null) {
            return Result.error("工作流不存在");
        }
        
        try {
            ExecutionResponse response = workflowEngine.execute(workflow, request.getInputData());
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("工作流执行失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "实时执行工作流(SSE)")
    @GetMapping(value = "/{id}/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeWorkflowStream(@PathVariable Long id, @RequestParam String inputData) {
        SseEmitter emitter = new SseEmitter(300000L);
        String emitterId = id + "_" + System.currentTimeMillis();
        emitters.put(emitterId, emitter);
        
        emitter.onCompletion(() -> emitters.remove(emitterId));
        emitter.onTimeout(() -> emitters.remove(emitterId));
        emitter.onError((e) -> emitters.remove(emitterId));
        
        Consumer<ExecutionEvent> eventCallback = event -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getEventType())
                        .data(event));
            } catch (IOException e) {
                log.error("发送 SSE 事件失败", e);
                emitters.remove(emitterId);
            }
        };
        
        new Thread(() -> {
            try {
                Workflow workflow = workflowService.getById(id);
                if (workflow == null) {
                    emitter.send(SseEmitter.event()
                            .name("ERROR")
                            .data(ExecutionEvent.workflowComplete("FAILED", "工作流不存在", 0)));
                    emitter.complete();
                    return;
                }
                
                workflowEngine.executeWithCallback(workflow, inputData, eventCallback);
                emitter.complete();
            } catch (Exception e) {
                log.error("工作流执行失败", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("ERROR")
                            .data(ExecutionEvent.workflowComplete("FAILED", e.getMessage(), 0)));
                } catch (IOException ex) {
                    log.error("发送错误事件失败", ex);
                }
                emitter.complete();
            }
        }).start();
        
        return emitter;
    }
}