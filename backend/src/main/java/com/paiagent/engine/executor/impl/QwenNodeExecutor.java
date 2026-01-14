package com.paiagent.engine.executor.impl;

import org.springframework.stereotype.Component;

/**
 * 通义千问(Qwen) 节点执行器
 * 基于Spring AI Alibaba实现
 */
@Component
public class QwenNodeExecutor extends AbstractLLMNodeExecutor {
    
    @Override
    protected String getNodeType() {
        return "qwen";
    }
}
