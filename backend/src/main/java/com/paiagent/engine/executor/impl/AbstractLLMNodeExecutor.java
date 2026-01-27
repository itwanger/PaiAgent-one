package com.paiagent.engine.executor.impl;

import com.paiagent.dto.ExecutionEvent;
import com.paiagent.engine.executor.NodeExecutor;
import com.paiagent.engine.llm.ChatClientFactory;
import com.paiagent.engine.llm.LLMNodeConfig;
import com.paiagent.engine.llm.PromptTemplateService;
import com.paiagent.engine.model.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * LLM节点执行器抽象基类
 * 统一处理配置提取、模板替换、API调用和输出构建
 */
@Slf4j
public abstract class AbstractLLMNodeExecutor implements NodeExecutor {
    
    @Autowired
    protected ChatClientFactory chatClientFactory;
    
    @Autowired
    protected PromptTemplateService promptTemplateService;
    
    /**
     * 获取节点类型标识
     */
    protected abstract String getNodeType();
    
    @Override
    public Map<String, Object> execute(WorkflowNode node, Map<String, Object> input) throws Exception {
        return execute(node, input, null);
    }
    
    @Override
    public Map<String, Object> execute(WorkflowNode node, Map<String, Object> input, 
                                       Consumer<ExecutionEvent> progressCallback) throws Exception {
        // 1. 提取节点配置
        LLMNodeConfig config = extractConfig(node);
        
        log.info("{} 节点配置 - API: {}, Model: {}, Temperature: {}", 
                getNodeType().toUpperCase(), config.getApiUrl(), config.getModel(), config.getTemperature());
        log.info("{} 输入参数配置: {}", getNodeType().toUpperCase(), config.getInputParams());
        log.info("{} 输入数据: {}", getNodeType().toUpperCase(), input);
        
        // 2. 处理prompt模板
        String finalPrompt = promptTemplateService.processTemplate(
                config.getPromptTemplate(), 
                config.getInputParams(), 
                input
        );
        log.info("最终提示词: {}", finalPrompt);
        
        // 3. 创建ChatClient
        ChatClient chatClient = chatClientFactory.createClient(
                getNodeType(),
                config.getApiUrl(),
                config.getApiKey(),
                config.getModel(),
                config.getTemperature()
        );
        
        // 4. 调用LLM（支持流式和非流式）
        LLMResponse llmResponse;
        if (config.isStreaming() && progressCallback != null) {
            llmResponse = executeStreaming(chatClient, finalPrompt, node, progressCallback);
        } else {
            llmResponse = executeNormal(chatClient, finalPrompt);
        }
        
        log.info("{} API响应: {}", getNodeType().toUpperCase(), llmResponse.getContent());
        log.info("{} Token统计: 输入={}, 输出={}, 总计={}", 
                getNodeType().toUpperCase(), 
                llmResponse.getInputTokens(),
                llmResponse.getOutputTokens(),
                llmResponse.getTotalTokens());
        
        // 5. 构建输出
        Map<String, Object> output = buildOutput(llmResponse, config.getOutputParams());
        log.info("{} 节点输出: {}", getNodeType().toUpperCase(), output);
        
        return output;
    }
    
    /**
     * LLM响应包装类
     */
    private static class LLMResponse {
        private final String content;
        private final Integer inputTokens;
        private final Integer outputTokens;
        private final Integer totalTokens;
        
        public LLMResponse(String content, Integer inputTokens, Integer outputTokens, Integer totalTokens) {
            this.content = content;
            this.inputTokens = inputTokens != null ? inputTokens : 0;
            this.outputTokens = outputTokens != null ? outputTokens : 0;
            this.totalTokens = totalTokens != null ? totalTokens : (this.inputTokens + this.outputTokens);
        }
        
        public String getContent() { return content; }
        public Integer getInputTokens() { return inputTokens; }
        public Integer getOutputTokens() { return outputTokens; }
        public Integer getTotalTokens() { return totalTokens; }
    }
    
    /**
     * 普通（非流式）调用
     */
    private LLMResponse executeNormal(ChatClient chatClient, String prompt) {
        var chatResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();
        
        String content = chatResponse.getResult().getOutput().getContent();
        
        // 提取token统计
        var metadata = chatResponse.getMetadata();
        Integer inputTokens = null;
        Integer outputTokens = null;
        Integer totalTokens = null;
        
        if (metadata != null && metadata.getUsage() != null) {
            var usage = metadata.getUsage();
            inputTokens = usage.getPromptTokens() != null ? usage.getPromptTokens().intValue() : null;
            outputTokens = usage.getGenerationTokens() != null ? usage.getGenerationTokens().intValue() : null;
            totalTokens = usage.getTotalTokens() != null ? usage.getTotalTokens().intValue() : null;
        }
        
        return new LLMResponse(content, inputTokens, outputTokens, totalTokens);
    }
    
    /**
     * 流式调用
     */
    private LLMResponse executeStreaming(ChatClient chatClient, String prompt, 
                                    WorkflowNode node, Consumer<ExecutionEvent> progressCallback) {
        StringBuilder accumulated = new StringBuilder();
        
        // 注意：流式调用时无法获取token统计，因为metadata在流式模式下不可用
        chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .doOnNext(chunk -> {
                    accumulated.append(chunk);
                    if (progressCallback != null) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("chunk", chunk);
                        data.put("accumulated", accumulated.toString());
                        progressCallback.accept(
                                ExecutionEvent.nodeProgress(node.getId(), node.getType(),
                                        "生成中...", data)
                        );
                    }
                })
                .blockLast();
        
        // 流式调用无token统计
        return new LLMResponse(accumulated.toString(), null, null, null);
    }
    
    /**
     * 从节点数据中提取配置
     */
    @SuppressWarnings("unchecked")
    protected LLMNodeConfig extractConfig(WorkflowNode node) {
        Map<String, Object> data = node.getData();
        
        LLMNodeConfig config = new LLMNodeConfig();
        config.setApiUrl(trimString(data.get("apiUrl")));
        config.setApiKey(trimString(data.get("apiKey")));
        config.setModel(trimString(data.get("model")));
        config.setTemperature(data.get("temperature") != null 
                ? ((Number) data.get("temperature")).doubleValue() 
                : 0.7);
        config.setPromptTemplate((String) data.get("prompt"));
        config.setInputParams((List<Map<String, Object>>) data.get("inputParams"));
        config.setOutputParams((List<Map<String, Object>>) data.get("outputParams"));
        config.setStreaming(Boolean.TRUE.equals(data.get("streaming")));
        
        return config;
    }
    
    /**
     * 构建输出结果
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> buildOutput(LLMResponse llmResponse, List<Map<String, Object>> outputParams) {
        Map<String, Object> output = new HashMap<>();
        
        String content = llmResponse.getContent();
        
        if (outputParams != null && !outputParams.isEmpty()) {
            for (Map<String, Object> param : outputParams) {
                String paramName = (String) param.get("name");
                output.put(paramName, content);
            }
        } else {
            output.put("output", content);
        }
        
        // 添加token统计
        output.put("inputTokens", llmResponse.getInputTokens());
        output.put("outputTokens", llmResponse.getOutputTokens());
        output.put("totalTokens", llmResponse.getTotalTokens());
        output.put("tokens", llmResponse.getTotalTokens()); // 保持向后兼容
        
        return output;
    }
    
    /**
     * 去除字符串两端空格
     */
    private String trimString(Object value) {
        return value != null ? value.toString().trim() : null;
    }
    
    @Override
    public String getSupportedNodeType() {
        return getNodeType();
    }
}
