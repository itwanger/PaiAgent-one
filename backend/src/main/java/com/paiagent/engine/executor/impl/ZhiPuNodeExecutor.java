package com.paiagent.engine.executor.impl;

import org.springframework.stereotype.Component;

/**
 * 智谱(ZhiPu) 节点执行器
 * 基于Spring AI实现，使用OpenAI兼容接口
 */
@Component
public class ZhiPuNodeExecutor extends AbstractLLMNodeExecutor {
    
    @Override
    protected String getNodeType() {
        return "zhipu";
    }
}
