package com.paiagent.engine.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt模板处理服务
 * 负责处理模板变量替换和参数映射
 */
@Slf4j
@Service
public class PromptTemplateService {
    
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{(.*?)\\}\\}");
    
    /**
     * 处理prompt模板，替换其中的变量
     *
     * @param promptTemplate 原始模板，包含{{variable}}格式的变量
     * @param inputParams    输入参数配置列表
     * @param runtimeInput   运行时输入数据（来自上游节点输出）
     * @return 替换后的最终prompt
     */
    public String processTemplate(String promptTemplate, 
                                  List<Map<String, Object>> inputParams, 
                                  Map<String, Object> runtimeInput) {
        if (promptTemplate == null) {
            return "";
        }
        
        // 构建参数值映射
        Map<String, String> paramValues = buildParamValues(inputParams, runtimeInput);
        log.debug("参数值映射: {}", paramValues);
        
        // 替换模板变量
        return replaceTemplateVariables(promptTemplate, paramValues);
    }
    
    /**
     * 构建参数值映射
     * 支持两种参数类型：
     * - input: 静态值，直接从配置中获取
     * - reference: 引用值，从上游节点输出中获取
     */
    private Map<String, String> buildParamValues(List<Map<String, Object>> inputParams, 
                                                  Map<String, Object> runtimeInput) {
        Map<String, String> paramValues = new HashMap<>();
        
        if (inputParams == null) {
            return paramValues;
        }
        
        for (Map<String, Object> param : inputParams) {
            String paramName = (String) param.get("name");
            String paramType = (String) param.get("type");
            
            if ("input".equals(paramType)) {
                // 静态输入值
                Object value = param.get("value");
                if (value != null) {
                    paramValues.put(paramName, value.toString());
                }
            } else if ("reference".equals(paramType)) {
                // 引用上游节点输出
                String reference = (String) param.get("referenceNode");
                if (reference != null && reference.contains(".")) {
                    String[] parts = reference.split("\\.");
                    String refParamName = parts[parts.length - 1];
                    
                    Object refValue = runtimeInput.get(refParamName);
                    // 兼容处理：user_input 可能存储为 input
                    if (refValue == null && "user_input".equals(refParamName)) {
                        refValue = runtimeInput.get("input");
                    }
                    
                    if (refValue != null) {
                        paramValues.put(paramName, refValue.toString());
                    }
                }
            }
        }
        
        return paramValues;
    }
    
    /**
     * 替换模板中的{{variable}}变量
     */
    private String replaceTemplateVariables(String template, Map<String, String> paramValues) {
        String result = template;
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String paramName = matcher.group(1).trim();
            String paramValue = paramValues.getOrDefault(paramName, "");
            result = result.replace("{{" + paramName + "}}", paramValue);
        }
        
        return result;
    }
}
