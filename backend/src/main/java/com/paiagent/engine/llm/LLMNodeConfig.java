package com.paiagent.engine.llm;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * LLM节点配置DTO
 */
@Data
public class LLMNodeConfig {
    
    /**
     * API端点URL
     */
    private String apiUrl;
    
    /**
     * API密钥
     */
    private String apiKey;
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 温度参数
     */
    private Double temperature;
    
    /**
     * 提示词模板
     */
    private String promptTemplate;
    
    /**
     * 输入参数配置
     */
    private List<Map<String, Object>> inputParams;
    
    /**
     * 输出参数配置
     */
    private List<Map<String, Object>> outputParams;
    
    /**
     * 是否启用流式输出
     */
    private boolean streaming;
}
