-- 为 workflow 表添加 engine_type 列
-- 执行日期: 2026-01-27
-- 用途: 支持 LangGraph4j 引擎选择

USE paiagent;

-- 1. 添加 engine_type 列，默认值为 'dag' 确保向后兼容
ALTER TABLE workflow 
ADD COLUMN engine_type VARCHAR(50) DEFAULT 'dag' COMMENT '工作流引擎类型(dag/langgraph)' 
AFTER flow_data;

-- 2. 更新现有数据（可选，因为已有 DEFAULT 值）
UPDATE workflow SET engine_type = 'dag' WHERE engine_type IS NULL;

-- 3. 验证
SELECT id, name, engine_type FROM workflow LIMIT 10;
