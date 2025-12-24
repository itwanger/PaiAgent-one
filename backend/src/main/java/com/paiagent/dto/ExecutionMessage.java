package com.paiagent.dto;

import lombok.Data;

/**
 * 执行状态消息（WebSocket 推送）
 */
@Data
public class ExecutionMessage {
    private String type; // 消息类型: START, NODE_START, NODE_COMPLETE, PROGRESS, COMPLETE, ERROR
    private String nodeId;
    private String nodeName;
    private String status;
    private Object input;
    private Object output;
    private String error;
    private Long duration;
    private Integer progress; // 进度百分比
    private String message; // 额外的消息文本
    private Long timestamp;

    public static ExecutionMessage start() {
        ExecutionMessage msg = new ExecutionMessage();
        msg.setType("START");
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static ExecutionMessage nodeStart(String nodeId, String nodeName, Object input) {
        ExecutionMessage msg = new ExecutionMessage();
        msg.setType("NODE_START");
        msg.setNodeId(nodeId);
        msg.setNodeName(nodeName);
        msg.setInput(input);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static ExecutionMessage nodeComplete(String nodeId, String nodeName, Object output, long duration) {
        ExecutionMessage msg = new ExecutionMessage();
        msg.setType("NODE_COMPLETE");
        msg.setNodeId(nodeId);
        msg.setNodeName(nodeName);
        msg.setOutput(output);
        msg.setStatus("SUCCESS");
        msg.setDuration(duration);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static ExecutionMessage nodeError(String nodeId, String nodeName, String error) {
        ExecutionMessage msg = new ExecutionMessage();
        msg.setType("NODE_COMPLETE");
        msg.setNodeId(nodeId);
        msg.setNodeName(nodeName);
        msg.setStatus("FAILED");
        msg.setError(error);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static ExecutionMessage progress(int completed, int total) {
        ExecutionMessage msg = new ExecutionMessage();
        msg.setType("PROGRESS");
        msg.setProgress(completed * 100 / total);
        msg.setMessage(String.format("已完成 %d/%d 个节点", completed, total));
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static ExecutionMessage complete(String status, Object outputData, long duration) {
        ExecutionMessage msg = new ExecutionMessage();
        msg.setType("COMPLETE");
        msg.setStatus(status);
        msg.setOutput(outputData);
        msg.setDuration(duration);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }

    public static ExecutionMessage error(String errorMessage) {
        ExecutionMessage msg = new ExecutionMessage();
        msg.setType("ERROR");
        msg.setError(errorMessage);
        msg.setTimestamp(System.currentTimeMillis());
        return msg;
    }
}
