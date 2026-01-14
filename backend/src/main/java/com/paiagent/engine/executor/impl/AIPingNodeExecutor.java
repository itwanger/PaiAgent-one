package com.paiagent.engine.executor.impl;

import org.springframework.stereotype.Component;

/**
 * AI Ping 节点执行器
 * 基于Spring AI实现，使用OpenAI兼容接口
 */
@Component
public class AIPingNodeExecutor extends AbstractLLMNodeExecutor {
    
    @Override
    protected String getNodeType() {
        return "ai_ping";
    }
}
