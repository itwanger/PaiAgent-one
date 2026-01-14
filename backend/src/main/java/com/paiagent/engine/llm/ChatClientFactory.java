package com.paiagent.engine.llm;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
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
     * @param nodeType    节点类型 (openai/deepseek/qwen/zhipu/ai_ping)
     * @param apiUrl      API端点URL (DashScope模式下忽略)
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
            case "openai", "deepseek", "zhipu", "ai_ping" -> createOpenAICompatibleModel(apiUrl, apiKey, model, temperature);
            case "qwen" -> createDashScopeModel(apiKey, model, temperature);
            default -> throw new IllegalArgumentException("不支持的节点类型: " + nodeType);
        };
        
        return ChatClient.builder(chatModel).build();
    }
    
    /**
     * 创建OpenAI兼容的ChatModel
     * 支持OpenAI、DeepSeek、ZhiPu、AIPing等OpenAI兼容接口
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
    
    /**
     * 创建DashScope ChatModel（用于通义千问）
     * 使用阿里云 DashScope 原生 API，通过 Spring AI Alibaba 框架调用
     * 
     * @param apiKey      阿里云 DashScope API Key
     * @param model       模型名称，如 qwen-turbo, qwen-plus, qwen-max 等
     * @param temperature 温度参数，控制输出随机性 (0.0-2.0)
     * @return DashScopeChatModel 实例
     * 
     * @apiNote 已知问题: spring-ai-alibaba 1.0.0.2 版本存在 requestOptions bug
     *          可能在某些场景下抛出 "requestOptions cannot be null" 异常
     *          建议后续升级到更高版本以获得完整修复
     * @see <a href="https://github.com/alibaba/spring-ai-alibaba/issues/3300">GitHub Issue #3300</a>
     */
    private ChatModel createDashScopeModel(String apiKey, String model, Double temperature) {
        log.info("【DashScope模式】创建ChatModel - 模型: {}, 温度: {}", model, temperature);
        
        // 创建 DashScope API 客户端
        DashScopeApi dashScopeApi = new DashScopeApi(apiKey);
        log.debug("DashScopeApi 实例创建成功");
        
        // 配置模型选项
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature)
                .build();
        log.debug("DashScopeChatOptions 配置完成 - model: {}, temperature: {}", model, temperature);
        
        // 创建并返回 DashScopeChatModel
        DashScopeChatModel chatModel = new DashScopeChatModel(dashScopeApi, options);
        log.info("【DashScope模式】DashScopeChatModel 创建成功");
        
        return chatModel;
    }
}