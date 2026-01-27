package com.paiagent.engine.langgraph;

import com.alibaba.fastjson2.JSON;
import com.paiagent.dto.ExecutionEvent;
import com.paiagent.dto.ExecutionResponse;
import com.paiagent.engine.WorkflowExecutor;
import com.paiagent.engine.langgraph.builder.GraphBuilder;
import com.paiagent.engine.langgraph.state.StateManager;
import com.paiagent.engine.model.WorkflowConfig;
import com.paiagent.entity.ExecutionRecord;
import com.paiagent.entity.Workflow;
import com.paiagent.mapper.ExecutionRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * LangGraph 工作流引擎
 * 
 * 基于 LangGraph4j 实现的新一代工作流执行引擎
 * 支持状态图、条件分支、循环等高级特性
 */
@Slf4j
@Service
public class LangGraphWorkflowEngine implements WorkflowExecutor {
    
    @Autowired
    private GraphBuilder graphBuilder;
    
    @Autowired
    private StateManager stateManager;
    
    @Autowired
    private ExecutionRecordMapper executionRecordMapper;
    
    @Override
    public ExecutionResponse execute(Workflow workflow, String inputData) {
        return executeWithCallback(workflow, inputData, null);
    }
    
    @Override
    public ExecutionResponse executeWithCallback(
            Workflow workflow, 
            String inputData, 
            Consumer<ExecutionEvent> eventCallback) {
        
        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMessage = null;
        
        log.info("开始执行工作流 [{}] - 使用 LangGraph 引擎", workflow.getName());
        
        try {
            // 触发工作流开始事件
            if (eventCallback != null) {
                eventCallback.accept(ExecutionEvent.workflowStart(null));
            }
            
            // 1. 解析工作流配置
            WorkflowConfig config = JSON.parseObject(workflow.getFlowData(), WorkflowConfig.class);
            log.info("工作流配置解析完成: 节点数={}, 边数={}", 
                config.getNodes().size(), config.getEdges().size());
            
            // 2. 构建 LangGraph
            CompiledGraph<AgentState> compiledGraph = graphBuilder.buildGraph(config, eventCallback);
            
            // 3. 初始化状态（使用 AgentState）
            Map<String, Object> initialStateData = stateManager.initializeState(inputData);
            
            // 4. 执行图
            log.info("开始执行 LangGraph");
            var result = compiledGraph.invoke(initialStateData);
            
            // 提取最终状态
            Map<String, Object> finalState;
            if (result.isPresent()) {
                finalState = result.get().data();
            } else {
                throw new RuntimeException("LangGraph 执行返回空结果");
            }
            
            log.info("LangGraph 执行完成");
            
            // 5. 检查执行状态
            if (!stateManager.isSuccessful(finalState)) {
                status = "FAILED";
                errorMessage = stateManager.getErrorMessage(finalState);
            }
            
            // 6. 提取输出数据
            Map<String, Object> outputData = stateManager.getFinalOutput(finalState);
            String outputDataJson = JSON.toJSONString(outputData);
            
            // 7. 提取节点执行结果
            var nodeResultsList = stateManager.extractNodeResults(finalState, config);
            
            // 8. 保存执行记录
            long endTime = System.currentTimeMillis();
            int duration = (int) (endTime - startTime);
            
            ExecutionRecord record = new ExecutionRecord();
            record.setFlowId(workflow.getId());
            
            Map<String, Object> inputDataMap = new HashMap<>();
            inputDataMap.put("input", inputData);
            record.setInputData(JSON.toJSONString(inputDataMap));
            
            record.setOutputData(outputDataJson);
            record.setStatus(status);
            record.setNodeResults(JSON.toJSONString(nodeResultsList));
            record.setErrorMessage(errorMessage);
            record.setDuration(duration);
            
            executionRecordMapper.insert(record);
            
            // 9. 触发工作流完成事件
            if (eventCallback != null) {
                eventCallback.accept(ExecutionEvent.workflowComplete(status, outputData, duration));
            }
            
            // 10. 构建响应
            ExecutionResponse response = new ExecutionResponse();
            response.setExecutionId(record.getId());
            response.setStatus(status);
            
            // 转换为 ExecutionResponse.NodeResult 格式
            java.util.List<ExecutionResponse.NodeResult> nodeResults = new ArrayList<>();
            for (Map<String, Object> nodeResultMap : nodeResultsList) {
                ExecutionResponse.NodeResult nodeResult = new ExecutionResponse.NodeResult();
                nodeResult.setNodeId((String) nodeResultMap.get("nodeId"));
                nodeResult.setNodeName((String) nodeResultMap.get("nodeName"));
                nodeResult.setStatus((String) nodeResultMap.get("status"));
                nodeResult.setOutput((String) nodeResultMap.get("output"));
                nodeResults.add(nodeResult);
            }
            response.setNodeResults(nodeResults);
            
            response.setOutputData(outputDataJson);
            response.setDuration(duration);
            
            log.info("工作流执行完成: status={}, duration={}ms", status, duration);
            return response;
            
        } catch (Exception e) {
            status = "FAILED";
            errorMessage = e.getMessage();
            
            log.error("工作流执行失败", e);
            
            // 触发错误事件
            if (eventCallback != null) {
                eventCallback.accept(ExecutionEvent.workflowComplete("FAILED", errorMessage, 
                    (int) (System.currentTimeMillis() - startTime)));
            }
            
            // 保存失败记录
            long endTime = System.currentTimeMillis();
            int duration = (int) (endTime - startTime);
            
            ExecutionRecord record = new ExecutionRecord();
            record.setFlowId(workflow.getId());
            record.setInputData(JSON.toJSONString(Map.of("input", inputData)));
            record.setStatus("FAILED");
            record.setErrorMessage(errorMessage);
            record.setDuration(duration);
            executionRecordMapper.insert(record);
            
            // 返回错误响应
            ExecutionResponse response = new ExecutionResponse();
            response.setExecutionId(record.getId());
            response.setStatus("FAILED");
            response.setNodeResults(new ArrayList<>());
            response.setOutputData(null);
            response.setDuration(duration);
            
            return response;
        }
    }
    
    @Override
    public String getEngineType() {
        return "langgraph";
    }
}
