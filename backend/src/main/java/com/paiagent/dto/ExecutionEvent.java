package com.paiagent.dto;

import lombok.Data;

@Data
public class ExecutionEvent {
    private String eventType;
    private String nodeId;
    private String nodeName;
    private String status;
    private String message;
    private Object data;
    private Long timestamp;
    
    public static ExecutionEvent workflowStart(Long executionId) {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventType("WORKFLOW_START");
        event.setMessage("工作流开始执行");
        event.setTimestamp(System.currentTimeMillis());
        event.setData(executionId);
        return event;
    }
    
    public static ExecutionEvent nodeStart(String nodeId, String nodeName) {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventType("NODE_START");
        event.setNodeId(nodeId);
        event.setNodeName(nodeName);
        event.setStatus("RUNNING");
        event.setMessage("节点开始执行");
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }
    
    public static ExecutionEvent nodeSuccess(String nodeId, String nodeName, Object output, int duration) {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventType("NODE_SUCCESS");
        event.setNodeId(nodeId);
        event.setNodeName(nodeName);
        event.setStatus("SUCCESS");
        event.setMessage("节点执行成功,耗时 " + duration + "ms");
        event.setData(output);
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }
    
    public static ExecutionEvent nodeError(String nodeId, String nodeName, String error) {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventType("NODE_ERROR");
        event.setNodeId(nodeId);
        event.setNodeName(nodeName);
        event.setStatus("FAILED");
        event.setMessage(error);
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }
    
    public static ExecutionEvent workflowComplete(String status, Object output, int duration) {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventType("WORKFLOW_COMPLETE");
        event.setStatus(status);
        event.setMessage("工作流执行完成,总耗时 " + duration + "ms");
        event.setData(output);
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }
    
    public static ExecutionEvent nodeProgress(String nodeId, String nodeName, String message, Object data) {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventType("NODE_PROGRESS");
        event.setNodeId(nodeId);
        event.setNodeName(nodeName);
        event.setStatus("RUNNING");
        event.setMessage(message);
        event.setData(data);
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }
}