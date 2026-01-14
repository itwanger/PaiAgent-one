package com.paiagent.engine.executor.impl;

import org.springframework.stereotype.Component;

/**
 * OpenAI 节点执行器
 * 基于Spring AI实现，支持流式输出
 */
@Component
public class OpenAINodeExecutor extends AbstractLLMNodeExecutor {
    
    @Override
    protected String getNodeType() {
        return "openai";
    }
}
