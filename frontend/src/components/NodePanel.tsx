import { useEffect, useState } from 'react';
import { Card, Collapse, message } from 'antd';
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
  const llmNodes = nodeTypes.filter((node) => node.category === 'LLM');
  const toolNodes = nodeTypes.filter((node) => node.category === 'TOOL');

  const renderNodeItem = (node: NodeDefinition) => (
    <div
      key={node.nodeType}
      draggable
      onDragStart={(e) => onDragStart(e, node.nodeType, node.displayName)}
      className="p-3 mb-2 bg-white border border-gray-200 rounded cursor-move hover:border-blue-400 hover:shadow-md transition-all"
    >
      <div className="flex items-center">
        <span className="text-2xl mr-2">{node.icon}</span>
        <span className="font-medium text-gray-700">{node.displayName}</span>
      </div>
    </div>
  );

  const items = [
    {
      key: 'llm',
      label: <span className="font-semibold">🤖 大模型节点</span>,
      children: (
        <div>
          {llmNodes.length > 0 ? (
            llmNodes.map(renderNodeItem)
          ) : (
            <div className="text-gray-400 text-center py-4">暂无节点</div>
          )}
        </div>
      ),
    },
    {
      key: 'tool',
      label: <span className="font-semibold">🔧 工具节点</span>,
      children: (
        <div>
          {toolNodes.length > 0 ? (
            toolNodes.map(renderNodeItem)
          ) : (
            <div className="text-gray-400 text-center py-4">暂无节点</div>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="h-full bg-gray-50 border-r border-gray-200 overflow-y-auto">
      <Card
        title={<span className="font-bold">节点库</span>}
        bordered={false}
        loading={loading}
        className="h-full"
      >
        <Collapse
          defaultActiveKey={['llm', 'tool']}
          ghost
          items={items}
        />
        <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded text-sm text-gray-600">
          💡 提示: 拖拽节点到画布中使用
        </div>
      </Card>
    </div>
  );
};

export default NodePanel;
