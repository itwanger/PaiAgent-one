package com.paiagent.engine;

import com.alibaba.fastjson2.JSON;
import com.paiagent.dto.ExecutionMessage;
import com.paiagent.dto.ExecutionResponse;
import com.paiagent.engine.dag.DAGParser;
import com.paiagent.engine.executor.NodeExecutor;
import com.paiagent.engine.executor.NodeExecutorFactory;
import com.paiagent.engine.model.WorkflowConfig;
import com.paiagent.engine.model.WorkflowNode;
import com.paiagent.entity.ExecutionRecord;
import com.paiagent.entity.Workflow;
import com.paiagent.mapper.ExecutionRecordMapper;
import com.paiagent.websocket.ExecutionWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行引擎
 */
@Slf4j
@Service
public class WorkflowEngine {

    @Autowired
    private DAGParser dagParser;

    @Autowired
    private NodeExecutorFactory executorFactory;

    @Autowired
    private ExecutionRecordMapper executionRecordMapper;

    @Autowired(required = false)
    private ExecutionWebSocketHandler webSocketHandler;

    /**
     * 执行工作流
     */
    public ExecutionResponse execute(Workflow workflow, String inputData) {
        long startTime = System.currentTimeMillis();

        // 发送开始消息
        if (webSocketHandler != null) {
            webSocketHandler.broadcast(ExecutionMessage.start());
        }

        // 解析工作流配置
        WorkflowConfig config = JSON.parseObject(workflow.getFlowData(), WorkflowConfig.class);

        // DAG 解析和拓扑排序
        List<WorkflowNode> sortedNodes = dagParser.parse(config);

        // 执行节点
        List<ExecutionResponse.NodeResult> nodeResults = new ArrayList<>();
        Map<String, Map<String, Object>> nodeOutputs = new HashMap<>();

        // 初始输入数据
        Map<String, Object> currentInput = new HashMap<>();
        currentInput.put("input", inputData);

        String status = "SUCCESS";
        String errorMessage = null;
        String outputData = null;

        try {
            int completedNodes = 0;
            for (WorkflowNode node : sortedNodes) {
                long nodeStartTime = System.currentTimeMillis();

                ExecutionResponse.NodeResult nodeResult = new ExecutionResponse.NodeResult();
                nodeResult.setNodeId(node.getId());
                nodeResult.setNodeName(node.getType());
                nodeResult.setInput(JSON.toJSONString(currentInput));

                // 发送节点开始消息
                if (webSocketHandler != null) {
                    webSocketHandler.broadcast(ExecutionMessage.nodeStart(node.getId(), node.getType(), currentInput));
                }

                try {
                    // 获取节点执行器
                    NodeExecutor executor = executorFactory.getExecutor(node.getType());

                    // 执行节点
                    Map<String, Object> output = executor.execute(node, currentInput);

                    // 记录输出
                    nodeOutputs.put(node.getId(), output);
                    currentInput = output; // 下一个节点的输入

                    nodeResult.setStatus("SUCCESS");
                    nodeResult.setOutput(JSON.toJSONString(output));

                    // 发送节点完成消息
                    long nodeDuration = System.currentTimeMillis() - nodeStartTime;
                    if (webSocketHandler != null) {
                        webSocketHandler.broadcast(ExecutionMessage.nodeComplete(node.getId(), node.getType(), output, nodeDuration));
                    }

                    // 发送进度消息
                    completedNodes++;
                    if (webSocketHandler != null) {
                        webSocketHandler.broadcast(ExecutionMessage.progress(completedNodes, sortedNodes.size()));
                    }

                } catch (Exception e) {
                    log.error("节点执行失败: {}", node.getId(), e);
                    nodeResult.setStatus("FAILED");
                    nodeResult.setError(e.getMessage());
                    status = "FAILED";
                    errorMessage = "节点 " + node.getId() + " 执行失败: " + e.getMessage();

                    // 发送节点错误消息
                    if (webSocketHandler != null) {
                        webSocketHandler.broadcast(ExecutionMessage.nodeError(node.getId(), node.getType(), e.getMessage()));
                    }

                    throw e;
                } finally {
                    long nodeEndTime = System.currentTimeMillis();
                    nodeResult.setDuration((int) (nodeEndTime - nodeStartTime));
                    nodeResults.add(nodeResult);
                }
            }

            // 最终输出
            outputData = JSON.toJSONString(currentInput);

        } catch (Exception e) {
            status = "FAILED";
            if (errorMessage == null) {
                errorMessage = e.getMessage();
            }
        }

        long endTime = System.currentTimeMillis();
        int duration = (int) (endTime - startTime);

        // 保存执行记录
        ExecutionRecord record = new ExecutionRecord();
        record.setFlowId(workflow.getId());
        // 将 inputData 包装成 JSON 对象
        Map<String, Object> inputDataMap = new HashMap<>();
        inputDataMap.put("input", inputData);
        String inputDataJson = JSON.toJSONString(inputDataMap);
        log.info("保存执行记录 - inputData: {}", inputDataJson);
        log.info("保存执行记录 - outputData: {}", outputData);
        record.setInputData(inputDataJson);
        record.setOutputData(outputData);
        record.setStatus(status);
        record.setNodeResults(JSON.toJSONString(nodeResults));
        record.setErrorMessage(errorMessage);
        record.setDuration(duration);
        executionRecordMapper.insert(record);

        // 发送完成消息
        if (webSocketHandler != null) {
            webSocketHandler.broadcast(ExecutionMessage.complete(status, currentInput, duration));
        }

        // 构建响应
        ExecutionResponse response = new ExecutionResponse();
        response.setExecutionId(record.getId());
        response.setStatus(status);
        response.setNodeResults(nodeResults);
        response.setOutputData(outputData);
        response.setDuration(duration);

        return response;
    }
}
