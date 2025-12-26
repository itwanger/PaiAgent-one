package com.paiagent.engine.executor.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.paiagent.engine.executor.NodeExecutor;
import com.paiagent.engine.model.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class QwenNodeExecutor implements NodeExecutor {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Override
    public Map<String, Object> execute(WorkflowNode node, Map<String, Object> input) throws Exception {
        Map<String, Object> data = node.getData();
        
        // 获取配置并 trim 去除空格
        String apiUrl = data.get("apiUrl") != null ? ((String) data.get("apiUrl")).trim() : null;
        String apiKey = data.get("apiKey") != null ? ((String) data.get("apiKey")).trim() : null;
        String model = data.get("model") != null ? ((String) data.get("model")).trim() : null;
        Double temperature = data.get("temperature") != null ? ((Number) data.get("temperature")).doubleValue() : 0.7;
        String promptTemplate = (String) data.get("prompt");
        
        // 获取输入参数配置
        List<Map<String, Object>> inputParams = (List<Map<String, Object>>) data.get("inputParams");
        
        log.info("Qwen 节点配置 - API: {}, Model: {}, Temperature: {}", apiUrl, model, temperature);
        log.info("Qwen 输入参数配置: {}", inputParams);
        log.info("Qwen 输入数据: {}", input);
        
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
        
        // 调用 Qwen API
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", finalPrompt);
        messages.add(message);
        requestBody.put("messages", messages);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);
        
        log.info("请求 Qwen API: {}", apiUrl);
        log.info("请求体: {}", requestBody.toJSONString());
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            apiUrl,
            entity,
            String.class
        );
        
        log.info("API 响应状态码: {}", response.getStatusCode());
        log.info("API 响应内容: {}", response.getBody());
        
        JSONObject responseJson = JSON.parseObject(response.getBody());
        JSONArray choices = responseJson.getJSONArray("choices");
        String apiResponse;
        if (choices != null && !choices.isEmpty()) {
            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject messageObj = firstChoice.getJSONObject("message");
            apiResponse = messageObj.getString("content");
        } else {
            throw new RuntimeException("API 返回格式异常: " + response.getBody());
        }
        
        Map<String, Object> output = new HashMap<>();
        
        List<Map<String, Object>> outputParams = (List<Map<String, Object>>) data.get("outputParams");
        if (outputParams != null && !outputParams.isEmpty()) {
            for (Map<String, Object> param : outputParams) {
                String paramName = (String) param.get("name");
                output.put(paramName, apiResponse);
            }
        } else {
            output.put("output", apiResponse);
        }
        
        output.put("tokens", 120);
        
        log.info("Qwen 节点输出: {}", output);
        
        return output;
    }
    
    @Override
    public String getSupportedNodeType() {
        return "qwen";
    }
}