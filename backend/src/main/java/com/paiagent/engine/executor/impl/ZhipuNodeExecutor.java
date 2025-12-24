package com.paiagent.engine.executor.impl;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;
import ai.z.openapi.service.model.Delta;
import com.paiagent.engine.executor.NodeExecutor;
import com.paiagent.engine.model.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ZhipuNodeExecutor implements NodeExecutor {

    private ZhipuAiClient client;
    private String lastApiKey;

    @Override
    public Map<String, Object> execute(WorkflowNode node, Map<String, Object> input) throws Exception {
        Map<String, Object> data = node.getData();

        // 获取配置
        String apiKey = (String) data.get("apiKey");
        String model = (String) data.get("model");
        Double temperature = data.get("temperature") != null ? ((Number) data.get("temperature")).doubleValue() : 0.7;
        String promptTemplate = (String) data.get("prompt");

        // 获取输入参数配置
        List<Map<String, Object>> inputParams = (List<Map<String, Object>>) data.get("inputParams");

        log.info("智谱 节点配置 - Model: {}, Temperature: {}", model, temperature);
        log.info("智谱 输入参数配置: {}", inputParams);
        log.info("智谱 输入数据: {}", input);

        // 构建参数值映射
        Map<String, String> paramValues = new HashMap<>();
        if (inputParams != null) {
            for (Map<String, Object> param : inputParams) {
                String paramName = (String) param.get("name");
                String paramType = (String) param.get("type");

                if ("input".equals(paramType)) {
                    paramValues.put(paramName, (String) param.get("value"));
                } else if ("reference".equals(paramType)) {
                    String reference = (String) param.get("referenceNode");
                    if (reference != null && reference.contains(".")) {
                        String[] parts = reference.split("\\.");
                        String refParamName = parts[parts.length - 1];

                        Object refValue = input.get(refParamName);
                        if (refValue == null && "user_input".equals(refParamName)) {
                            refValue = input.get("input");
                        }

                        if (refValue != null) {
                            paramValues.put(paramName, refValue.toString());
                        }
                    }
                }
            }
        }

        log.info("参数值映射: {}", paramValues);

        // 替换提示词模板中的参数
        String finalPrompt = promptTemplate;
        if (finalPrompt != null) {
            Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}");
            Matcher matcher = pattern.matcher(promptTemplate);

            while (matcher.find()) {
                String paramName = matcher.group(1).trim();
                String paramValue = paramValues.getOrDefault(paramName, "");
                finalPrompt = finalPrompt.replace("{{" + paramName + "}}", paramValue);
            }
        }

        log.info("最终提示词: {}", finalPrompt);

        // 获取 maxTokens 配置，默认 2048
        Integer maxTokens = data.get("maxTokens") != null
            ? ((Number) data.get("maxTokens")).intValue()
            : 2048;

        // 获取是否启用流式响应
        boolean enableStream = data.get("enableStream") != null
            ? (Boolean) data.get("enableStream")
            : false;

        try {
            // 复用客户端，只在 apiKey 变化时重新创建
            if (client == null || !apiKey.equals(lastApiKey)) {
                client = ZhipuAiClient.builder().ofZHIPU()
                    .apiKey(apiKey)
                    .build();
                lastApiKey = apiKey;
                log.info("创建新的智谱 AI 客户端");
            }

            // 构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(ChatMessage.builder()
                .role(ChatMessageRole.USER.value())
                .content(finalPrompt)
                .build());

            // 构建请求参数
            ChatCompletionCreateParams request;
            if (enableStream) {
                request = ChatCompletionCreateParams.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(temperature.floatValue())
                    .maxTokens(maxTokens)
                    .stream(true)
                    .build();
            } else {
                request = ChatCompletionCreateParams.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(temperature.floatValue())
                    .maxTokens(maxTokens)
                    .build();
            }

            // 发送请求
            log.info("调用智谱 AI API，模型: {}, 流式: {}", model, enableStream);

            if (enableStream) {
                // 流式响应
                return handleStreamResponse(client, request, data);
            } else {
                // 非流式响应
                return handleNormalResponse(client, request, data);
            }
        } catch (Exception e) {
            log.error("调用智谱 AI API 失败", e);
            throw new RuntimeException("调用智谱 AI API 失败: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> handleNormalResponse(ZhipuAiClient client,
                                                     ChatCompletionCreateParams request,
                                                     Map<String, Object> data) {
        ChatCompletionResponse response = client.chat().createChatCompletion(request);

        if (response.isSuccess()) {
            ChatMessage reply = response.getData().getChoices().get(0).getMessage();
            Object contentObj = reply.getContent();
            String content = contentObj != null ? String.valueOf(contentObj) : "";
            log.info("智谱 AI 响应内容: {}", content);

            // 构建输出
            Map<String, Object> output = new HashMap<>();

            // 根据输出参数配置返回结果
            List<Map<String, Object>> outputParams = (List<Map<String, Object>>) data.get("outputParams");
            if (outputParams != null && !outputParams.isEmpty()) {
                for (Map<String, Object> param : outputParams) {
                    String paramName = (String) param.get("name");
                    output.put(paramName, content);
                }
            } else {
                output.put("output", content);
            }

            // 获取 token 使用量
            if (response.getData().getUsage() != null) {
                output.put("tokens", response.getData().getUsage().getTotalTokens());
            } else {
                output.put("tokens", 0);
            }

            log.info("智谱 节点输出: {}", output);

            return output;
        } else {
            String errorMsg = response.getMsg();
            log.error("智谱 AI API 返回错误: {}", errorMsg);
            throw new RuntimeException("智谱 AI API 返回错误: " + errorMsg);
        }
    }

    private Map<String, Object> handleStreamResponse(ZhipuAiClient client,
                                                     ChatCompletionCreateParams request,
                                                     Map<String, Object> data) throws Exception {
        StringBuilder fullContent = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        final int[] totalTokens = {0};
        final Throwable[] error = {null};

        ChatCompletionResponse response = client.chat().createChatCompletion(request);

        if (response.isSuccess()) {
            response.getFlowable().subscribe(
                // 处理流式消息
                chunk -> {
                    if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                        Delta delta = chunk.getChoices().get(0).getDelta();
                        if (delta != null) {
                            Object content = delta.getContent();
                            if (content != null) {
                                String contentStr = String.valueOf(content);
                                fullContent.append(contentStr);
                                log.debug("收到流式内容: {}", contentStr);
                            }
                        }
                    }

                    // 获取 usage (最后一个 chunk 中)
                    if (chunk.getUsage() != null) {
                        totalTokens[0] = chunk.getUsage().getTotalTokens();
                    }
                },
                // 处理错误
                err -> {
                    log.error("流式响应错误", err);
                    error[0] = err;
                    latch.countDown();
                },
                // 完成时
                () -> {
                    log.info("智谱 AI 流式响应完成，完整内容: {}", fullContent);
                    latch.countDown();
                }
            );

            // 等待流式响应完成
            latch.await();

            if (error[0] != null) {
                throw new RuntimeException("智谱 AI 流式响应失败: " + error[0].getMessage(), error[0]);
            }
        } else {
            String errorMsg = response.getMsg();
            log.error("智谱 AI API 返回错误: {}", errorMsg);
            throw new RuntimeException("智谱 AI API 返回错误: " + errorMsg);
        }

        // 构建输出
        Map<String, Object> output = new HashMap<>();

        // 根据输出参数配置返回结果
        List<Map<String, Object>> outputParams = (List<Map<String, Object>>) data.get("outputParams");
        if (outputParams != null && !outputParams.isEmpty()) {
            for (Map<String, Object> param : outputParams) {
                String paramName = (String) param.get("name");
                output.put(paramName, fullContent.toString());
            }
        } else {
            output.put("output", fullContent.toString());
        }

        output.put("tokens", totalTokens[0] > 0 ? totalTokens[0] : fullContent.length() / 2);
        output.put("streamed", true);

        log.info("智谱 节点输出: {}", output);

        return output;
    }

    @Override
    public String getSupportedNodeType() {
        return "zhipu";
    }
}
