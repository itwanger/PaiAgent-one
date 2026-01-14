package com.paiagent.engine.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

/**
 * ChatClient动态工厂
 * 根据节点配置在运行时创建不同类型的ChatClient实例
 */
@Slf4j
@Component
public class ChatClientFactory {
    
    /**
     * 根据节点类型和配置创建ChatClient
     *
     * @param nodeType    节点类型 (openai/deepseek/qwen)
     * @param apiUrl      API端点URL
     * @param apiKey      API密钥
     * @param model       模型名称
     * @param temperature 温度参数
     * @return ChatClient实例
     */
    public ChatClient createClient(String nodeType, String apiUrl, String apiKey, 
                                   String model, Double temperature) {
        log.info("创建ChatClient - 类型: {}, URL: {}, 模型: {}, 温度: {}", 
                nodeType, apiUrl, model, temperature);
        
        ChatModel chatModel = switch (nodeType) {
            case "openai", "deepseek", "qwen" -> createOpenAICompatibleModel(apiUrl, apiKey, model, temperature);
            default -> throw new IllegalArgumentException("不支持的节点类型: " + nodeType);
        };
        
        return ChatClient.builder(chatModel).build();
    }
    
    /**
     * 创建OpenAI兼容的ChatModel
     * 支持OpenAI、DeepSeek和通义千问（通过OpenAI兼容接口）
     */
    private ChatModel createOpenAICompatibleModel(String apiUrl, String apiKey, 
                                                   String model, Double temperature) {
        // 使用构造函数创建OpenAiApi（支持自定义baseUrl）
        OpenAiApi openAiApi = new OpenAiApi(apiUrl, apiKey);
        
        // 创建ChatModel并配置选项
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();
        
        return new OpenAiChatModel(openAiApi, options);
    }
}
