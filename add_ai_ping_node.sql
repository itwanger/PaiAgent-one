-- 更新 PaiAgent 数据库，添加 AI Ping 节点
-- 使用方法: 在 MySQL 中执行此脚本

USE paiagent;

-- 检查 AI Ping 节点是否已存在
SELECT CASE
  WHEN EXISTS (SELECT 1 FROM node_definition WHERE node_type = 'ai_ping')
  THEN 'AI Ping 节点已存在，跳过插入'
  ELSE 'AI Ping 节点不存在，准备插入'
END AS status;

-- 插入 AI Ping 节点定义
INSERT INTO node_definition (node_type, display_name, category, icon, input_schema, output_schema, config_schema)
VALUES ('ai_ping', 'AI Ping', 'LLM', '🏓',
 '{"type": "object", "properties": {"input": {"type": "string"}}}',
 '{"type": "object", "properties": {"output": {"type": "string"}, "tokens": {"type": "number"}}}',
 '{"type": "object", "properties": {"baseUrl": {"type": "string", "default": "https://api.aiping.com/v1"}, "apiKey": {"type": "string"}, "model": {"type": "string", "default": "ai-ping-v1"}, "prompt": {"type": "string"}, "temperature": {"type": "number", "default": 0.7}, "maxTokens": {"type": "number", "default": 1000}}}')
ON DUPLICATE KEY UPDATE
  display_name = 'AI Ping',
  category = 'LLM',
  icon = '🏓',
  input_schema = '{"type": "object", "properties": {"input": {"type": "string"}}}',
  output_schema = '{"type": "object", "properties": {"output": {"type": "string"}, "tokens": {"type": "number"}}}',
  config_schema = '{"type": "object", "properties": {"baseUrl": {"type": "string", "default": "https://api.aiping.com/v1"}, "apiKey": {"type": "string"}, "model": {"type": "string", "default": "ai-ping-v1"}, "prompt": {"type": "string"}, "temperature": {"type": "number", "default": 0.7}, "maxTokens": {"type": "number", "default": 1000}}}';

-- 验证插入结果
SELECT node_type, display_name, category, icon, created_at
FROM node_definition
WHERE node_type = 'ai_ping';