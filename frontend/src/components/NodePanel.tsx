import { useEffect, useState } from 'react';
import { Collapse, message } from 'antd';
import { getNodeTypes, NodeDefinition } from '../api/workflow';

interface NodePanelProps {
  onDragStart: (event: React.DragEvent, nodeType: string, displayName: string) => void;
}

/**
 * 左侧节点面板组件
 */
const NodePanel = ({ onDragStart }: NodePanelProps) => {
  const [nodeTypes, setNodeTypes] = useState<NodeDefinition[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadNodeTypes();
  }, []);

  const loadNodeTypes = async () => {
    setLoading(true);
    try {
      const result = await getNodeTypes();
      if (result.code === 200) {
        setNodeTypes(result.data);
      } else {
        message.error('加载节点类型失败');
      }
    } catch {
      message.error('加载节点类型失败');
    } finally {
      setLoading(false);
    }
  };

  // 按分类分组节点
  const llmNodes = nodeTypes.filter((node) => node.category === 'LLM' && node.nodeType !== 'openai');
  const toolNodes = nodeTypes.filter((node) => node.category === 'TOOL');

  const renderNodeItem = (node: NodeDefinition) => (
    <div
      key={node.nodeType}
      draggable
      onDragStart={(e) => onDragStart(e, node.nodeType, node.displayName)}
      className="p-3 mb-3 bg-cyber-gray border border-cyber-purple/50 rounded-lg cursor-move hover:border-cyber-cyan hover:shadow-neon-cyan hover:bg-cyber-purple/5 transition-all"
    >
      <div className="flex items-center gap-2">
        <span className="text-2xl">{node.icon}</span>
        <span className="font-medium text-cyber-pink text-sm">{node.displayName}</span>
      </div>
    </div>
  );

  const items = [
    {
      key: 'llm',
      label: <span className="font-semibold text-cyber-cyan text-glow text-sm">🤖 大模型节点</span>,
      children: (
        <div className="pt-2">
          {llmNodes.length > 0 ? (
            llmNodes.map(renderNodeItem)
          ) : (
            <div className="text-cyber-pink/60 text-center py-6 text-sm">暂无节点</div>
          )}
        </div>
      ),
    },
    {
      key: 'tool',
      label: <span className="font-semibold text-cyber-cyan text-glow text-sm">🔧 工具节点</span>,
      children: (
        <div className="pt-2">
          {toolNodes.length > 0 ? (
            toolNodes.map(renderNodeItem)
          ) : (
            <div className="text-cyber-pink/60 text-center py-6 text-sm">暂无节点</div>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="h-full flex flex-col">
      {/* 标题 */}
      <div className="px-4 py-3 border-b border-cyber-purple/30 bg-cyber-dark/50">
        <h3 className="font-bold text-cyber-cyan text-neon text-base">节点库</h3>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-3">
        {loading ? (
          <div className="text-center py-12 text-cyber-pink animate-pulse">加载中...</div>
        ) : (
          <>
            <Collapse
              defaultActiveKey={['llm', 'tool']}
              ghost
              items={items}
              bordered={false}
              className="bg-transparent"
            />
            <div className="mt-3 px-3 py-2 bg-cyber-purple/10 rounded-lg text-xs text-cyber-cyan border border-cyber-purple/30 text-center">
              💡 拖拽节点到画布中使用
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default NodePanel;
