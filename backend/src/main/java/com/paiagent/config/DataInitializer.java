package com.paiagent.config;

import com.paiagent.entity.NodeDefinition;
import com.paiagent.service.NodeDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 数据初始化器
 * 在应用启动时自动添加默认的节点定义
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private NodeDefinitionService nodeDefinitionService;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化节点定义数据...");

        // 定义所有需要初始化的人工智能节点
        List<NodeDefinition> aiPingNodes = Arrays.asList(
            createNodeDefinition("ai_ping", "AI Ping", "LLM", "🏓",
                "{\"type\": \"object\", \"properties\": {\"input\": {\"type\": \"string\"}}}",
                "{\"type\": \"object\", \"properties\": {\"output\": {\"type\": \"string\"}, \"tokens\": {\"type\": \"number\"}}}",
                "{\"type\": \"object\", \"properties\": {\"apiUrl\": {\"type\": \"string\", \"default\": \"https://api.aiping.com/v1\"}, \"apiKey\": {\"type\": \"string\"}, \"model\": {\"type\": \"string\", \"default\": \"ai-ping-v1\"}, \"prompt\": {\"type\": \"string\"}, \"temperature\": {\"type\": \"number\", \"default\": 0.7, \"minimum\": 0, \"maximum\": 1}, \"maxTokens\": {\"type\": \"number\", \"default\": 1000}}}")
        );

        // 检查并初始化每个节点
        for (NodeDefinition node : aiPingNodes) {
            NodeDefinition existing = nodeDefinitionService.getByNodeType(node.getNodeType());
            if (existing == null) {
                log.info("添加新节点: {} ({})", node.getDisplayName(), node.getNodeType());
                nodeDefinitionService.save(node);
            } else {
                log.info("节点已存在: {} ({})", node.getDisplayName(), node.getNodeType());
            }
        }

        log.info("节点定义初始化完成！");
    }

    private NodeDefinition createNodeDefinition(String nodeType, String displayName, String category,
                                               String icon, String inputSchema, String outputSchema, String configSchema) {
        NodeDefinition node = new NodeDefinition();
        node.setNodeType(nodeType);
        node.setDisplayName(displayName);
        node.setCategory(category);
        node.setIcon(icon);
        node.setInputSchema(inputSchema);
        node.setOutputSchema(outputSchema);
        node.setConfigSchema(configSchema);
        node.setDeleted(0);
        return node;
    }
}
