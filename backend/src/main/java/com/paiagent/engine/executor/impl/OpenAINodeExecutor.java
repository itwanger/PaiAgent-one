package com.paiagent.engine.executor.impl;

import com.paiagent.engine.executor.NodeExecutor;
import com.paiagent.engine.model.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI 节点执行器(示例实现)
 * 注意:这是一个示例实现,实际使用需要配置真实的 API Key 和调用 OpenAI API
 */
@Slf4j
@Component
public class OpenAINodeExecutor implements NodeExecutor {
    
    @Override
    public Map<String, Object> execute(WorkflowNode node, Map<String, Object> input) throws Exception {
        Map<String, Object> data = node.getData();
        String prompt = (String) data.get("prompt");
        String inputText = (String) input.get("input");
        
        log.info("OpenAI 节点执行 - 提示词: {}, 输入: {}", prompt, inputText);
        
        // TODO: 实际调用 OpenAI API
        // 这里使用模拟数据
        String simulatedResponse = "这是 OpenAI 对 \"" + inputText + "\" 的模拟回复。\n" +
                "提示词: " + prompt + "\n" +
                "生成的播客脚本内容...";
        
        Map<String, Object> output = new HashMap<>();
        output.put("output", simulatedResponse);
        output.put("tokens", 100);
        
        return output;
    }
    
    @Override
    public String getSupportedNodeType() {
        return "openai";
    }
}
