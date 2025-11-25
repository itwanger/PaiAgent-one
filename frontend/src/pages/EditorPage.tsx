import { useState } from 'react';
import { Button, Input, Form, Card, message } from 'antd';
import { SaveOutlined, FolderOpenOutlined, BugOutlined, LogoutOutlined } from '@ant-design/icons';
import { Node } from '@xyflow/react';
import NodePanel from '../components/NodePanel';
import FlowCanvas from '../components/FlowCanvas';
import DebugDrawer from '../components/DebugDrawer';
import { useWorkflowStore } from '../store/workflowStore';
import { useAuthStore } from '../store/authStore';
import { createWorkflow, updateWorkflow, executeWorkflow } from '../api/workflow';
import { useNavigate } from 'react-router-dom';

/**
 * 工作流编辑器页面
 */
const EditorPage = () => {
  const navigate = useNavigate();
  const { username, clearAuth } = useAuthStore();
  const { nodes, edges, currentWorkflowId, setCurrentWorkflowId, selectedNode } = useWorkflowStore();
  const [workflowName, setWorkflowName] = useState('未命名工作流');
  const [saving, setSaving] = useState(false);
  const [debugDrawerOpen, setDebugDrawerOpen] = useState(false);

  // 处理节点拖拽开始
  const handleDragStart = (event: React.DragEvent, nodeType: string, displayName: string) => {
    event.dataTransfer.setData('application/reactflow-type', nodeType);
    event.dataTransfer.setData('application/reactflow-label', displayName);
    event.dataTransfer.effectAllowed = 'move';
  };

  // 处理节点点击
  const handleNodeClick = (node: Node) => {
    console.log('Node clicked:', node);
    // TODO: 显示节点配置面板
  };

  // 保存工作流
  const handleSave = async () => {
    if (nodes.length === 0) {
      message.warning('工作流为空,无法保存');
      return;
    }

    const flowData = JSON.stringify({
      nodes: nodes.map((node) => ({
        id: node.id,
        type: node.data?.type || node.type,
        position: node.position,
        data: node.data,
      })),
      edges: edges.map((edge) => ({
        id: edge.id,
        source: edge.source,
        target: edge.target,
        sourceHandle: edge.sourceHandle,
        targetHandle: edge.targetHandle,
      })),
    });

    setSaving(true);
    try {
      if (currentWorkflowId) {
        // 更新
        await updateWorkflow(currentWorkflowId, {
          name: workflowName,
          flowData,
        });
        message.success('工作流保存成功');
      } else {
        // 创建
        const result = await createWorkflow({
          name: workflowName,
          description: '通过编辑器创建',
          flowData,
        });
        if (result.code === 200) {
          setCurrentWorkflowId(result.data.id);
          message.success('工作流创建成功');
        }
      }
    } catch {
      message.error('保存失败');
    } finally {
      setSaving(false);
    }
  };

  // 执行工作流(从调试抽屉调用)
  const handleExecute = async (inputData: string) => {
    if (!currentWorkflowId) {
      throw new Error('请先保存工作流');
    }

    const result = await executeWorkflow(currentWorkflowId, inputData);
    if (result.code === 200) {
      return result.data;
    } else {
      throw new Error(result.message || '执行失败');
    }
  };

  // 打开调试抽屉
  const handleOpenDebug = () => {
    if (!currentWorkflowId) {
      message.warning('请先保存工作流');
      return;
    }
    setDebugDrawerOpen(true);
  };

  // 登出
  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  return (
    <div className="h-screen flex flex-col bg-gray-100">
      {/* 顶部工具栏 */}
      <div className="bg-white shadow-sm px-4 py-3 flex items-center justify-between border-b border-gray-200">
        <div className="flex items-center gap-4">
          <h1 className="text-xl font-bold text-gray-800">PaiAgent</h1>
          <Input
            value={workflowName}
            onChange={(e) => setWorkflowName(e.target.value)}
            className="w-64"
            placeholder="工作流名称"
          />
        </div>
        
        <div className="flex items-center gap-3">
          <Button
            icon={<FolderOpenOutlined />}
            onClick={() => message.info('加载功能开发中')}
          >
            加载
          </Button>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            onClick={handleSave}
            loading={saving}
          >
            保存
          </Button>
          <Button
            type="primary"
            icon={<BugOutlined />}
            onClick={handleOpenDebug}
            disabled={!currentWorkflowId}
          >
            调试
          </Button>
          <div className="ml-4 flex items-center gap-2">
            <span className="text-gray-600">👤 {username}</span>
            <Button
              icon={<LogoutOutlined />}
              onClick={handleLogout}
            >
              登出
            </Button>
          </div>
        </div>
      </div>

      {/* 主要内容区域 */}
      <div className="flex-1 flex overflow-hidden">
        {/* 左侧节点面板 */}
        <div className="w-64 flex-shrink-0">
          <NodePanel onDragStart={handleDragStart} />
        </div>

        {/* 中间画布 */}
        <div className="flex-1">
          <FlowCanvas onNodeClick={handleNodeClick} />
        </div>

        {/* 右侧配置面板 */}
        <div className="w-80 flex-shrink-0 bg-white border-l border-gray-200 overflow-y-auto">
          <Card title="节点配置" className="m-4">
            {selectedNode ? (
              <div>
                <p className="text-gray-600">节点 ID: {selectedNode.id}</p>
                <p className="text-gray-600">节点类型: {String(selectedNode.data?.type || '')}</p>
                <Form className="mt-4">
                  <Form.Item label="提示词">
                    <Input.TextArea rows={4} placeholder="输入提示词..." />
                  </Form.Item>
                  <Form.Item label="温度">
                    <Input type="number" step="0.1" defaultValue="0.7" />
                  </Form.Item>
                  <Button type="primary" block>
                    保存配置
                  </Button>
                </Form>
              </div>
            ) : (
              <p className="text-gray-400 text-center py-8">请选择一个节点</p>
            )}
          </Card>
        </div>
      </div>

      {/* 调试抽屉 */}
      <DebugDrawer
        open={debugDrawerOpen}
        onClose={() => setDebugDrawerOpen(false)}
        onExecute={handleExecute}
      />
    </div>
  );
};

export default EditorPage;
