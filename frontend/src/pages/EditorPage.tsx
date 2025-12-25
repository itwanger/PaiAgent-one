import { useState, useEffect, useRef } from 'react';
import { Button, Input, Form, message, Checkbox, Select, Space, Modal, List } from 'antd';
import { SaveOutlined, FolderOpenOutlined, BugOutlined, LogoutOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { Node } from '@xyflow/react';
import NodePanel from '../components/NodePanel';
import FlowCanvas from '../components/FlowCanvas';
import DebugDrawer from '../components/DebugDrawer';
import { useWorkflowStore } from '../store/workflowStore';
import { useAuthStore } from '../store/authStore';
import { createWorkflow, updateWorkflow, executeWorkflow, getWorkflows, getWorkflow, Workflow } from '../api/workflow';
import { useNavigate, useParams } from 'react-router-dom';

interface OutputParam {
  name: string;
  type: 'input' | 'reference';
  value: string;
  referenceNode?: string;
}

interface LlmInputParam {
  name: string;
  type: 'input' | 'reference';
  value: string;
  referenceNode?: string;
}

interface LlmOutputParam {
  name: string;
  type: string;
  description?: string;
}

interface TtsInputParam {
  name: string;
  type: 'input' | 'reference';
  value: string;
  referenceNode?: string;
}

interface TtsOutputParam {
  name: string;
  value: string;
}

/**
 * 工作流编辑器页面
 */
const EditorPage = () => {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const { username, clearAuth } = useAuthStore();
  const { nodes, edges, currentWorkflowId, setCurrentWorkflowId, selectedNode, setNodes, setEdges } = useWorkflowStore();
  const [workflowName, setWorkflowName] = useState('未命名工作流');
  const [saving, setSaving] = useState(false);
  const [debugDrawerOpen, setDebugDrawerOpen] = useState(false);
  const [outputParams, setOutputParams] = useState<OutputParam[]>([]);
  const [responseContent, setResponseContent] = useState('');
  const [loadModalOpen, setLoadModalOpen] = useState(false);
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [loadingWorkflows, setLoadingWorkflows] = useState(false);
  const hasLoadedRef = useRef<number | null>(null);
  
  // LLM 节点配置状态
  const [llmConfig, setLlmConfig] = useState({
    apiUrl: '',
    apiKey: '',
    model: '',
    temperature: 0.7,
    prompt: '',
    maxTokens: 2048,
    enableStream: false
  });
  const [llmInputParams, setLlmInputParams] = useState<LlmInputParam[]>([]);
  const [llmOutputParams, setLlmOutputParams] = useState<LlmOutputParam[]>([]);

  // TTS 节点配置状态
  const [ttsConfig, setTtsConfig] = useState({
    apiKey: '',
    model: 'qwen3-tts-flash',
    voice: 'Cherry',
    languageType: 'Auto'
  });
  const [ttsInputParams, setTtsInputParams] = useState<TtsInputParam[]>([]);
  const [ttsOutputParams, setTtsOutputParams] = useState<TtsOutputParam[]>([]);

  // 处理节点拖拽开始
  const handleDragStart = (event: React.DragEvent, nodeType: string, displayName: string) => {
    event.dataTransfer.setData('application/reactflow-type', nodeType);
    event.dataTransfer.setData('application/reactflow-label', displayName);
    event.dataTransfer.effectAllowed = 'move';
  };

  // 处理节点点击
  const handleNodeClick = (node: Node) => {
    console.log('Node clicked:', node);
    useWorkflowStore.getState().setSelectedNode(node);
    
    // 加载节点配置
    if (node.data?.type === 'output') {
      setOutputParams((node.data?.outputParams as OutputParam[]) || []);
      setResponseContent((node.data?.responseContent as string) || '');
    } else if (node.data?.type === 'openai' || node.data?.type === 'deepseek' || node.data?.type === 'qwen' || node.data?.type === 'zhipu') {
      // 加载 LLM 节点配置
      setLlmConfig({
        apiUrl: (node.data?.apiUrl as string) || '',
        apiKey: (node.data?.apiKey as string) || '',
        model: (node.data?.model as string) || '',
        temperature: (node.data?.temperature as number) || 0.7,
        prompt: (node.data?.prompt as string) || '',
        maxTokens: (node.data?.maxTokens as number) || 2048,
        enableStream: (node.data?.enableStream as boolean) || false
      });
      setLlmInputParams((node.data?.inputParams as LlmInputParam[]) || []);
      setLlmOutputParams((node.data?.outputParams as LlmOutputParam[]) || []);
    } else if (node.data?.type === 'tts') {
      // 加载 TTS 节点配置
      setTtsConfig({
        apiKey: (node.data?.apiKey as string) || '',
        model: (node.data?.model as string) || 'qwen3-tts-flash',
        voice: (node.data?.voice as string) || 'Cherry',
        languageType: (node.data?.languageType as string) || 'Auto'
      });
      setTtsInputParams((node.data?.inputParams as TtsInputParam[]) || []);
      setTtsOutputParams((node.data?.outputParams as TtsOutputParam[]) || []);
    }
  };

  // 从 URL 加载工作流
  useEffect(() => {
    if (id) {
      const workflowId = parseInt(id);
      // 避免重复加载 - 使用 ref 标记
      if (hasLoadedRef.current !== workflowId) {
        hasLoadedRef.current = workflowId;
        loadWorkflowById(workflowId);
      }
    }
  }, [id]);

  // 加载指定工作流
  const loadWorkflowById = async (workflowId: number) => {
    try {
      const result = await getWorkflow(workflowId);
      if (result.code === 200) {
        const workflow = result.data;
        setWorkflowName(workflow.name);
        setCurrentWorkflowId(workflow.id);
        
        const flowData = JSON.parse(workflow.flowData);
        console.log('加载的工作流数据:', flowData);
        
        // 加载节点
        const loadedNodes = flowData.nodes || [];
        setNodes(loadedNodes);
        
        // 加载连线并恢复箭头
        const loadedEdges = (flowData.edges || []).map((edge: any) => ({
          ...edge,
          markerEnd: {
            type: 'arrowclosed',
            width: 20,
            height: 20,
          },
        }));
        setEdges(loadedEdges);
        
        // 恢复输出节点配置
        const outputNode = loadedNodes.find((n: any) => n.data?.type === 'output');
        console.log('找到输出节点:', outputNode);
        console.log('输出节点配置 - outputParams:', outputNode?.data?.outputParams);
        console.log('输出节点配置 - responseContent:', outputNode?.data?.responseContent);
        
        if (outputNode?.data?.outputParams) {
          setOutputParams(outputNode.data.outputParams);
        } else {
          setOutputParams([]);
        }
        if (outputNode?.data?.responseContent) {
          setResponseContent(outputNode.data.responseContent);
        } else {
          setResponseContent('');
        }
        
        message.success('工作流加载成功');
      }
    } catch (error) {
      message.error('工作流加载失败');
      console.error(error);
    }
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
          const workflowId = result.data.id;
          setCurrentWorkflowId(workflowId);
          // 更新 URL
          navigate(`/editor/${workflowId}`, { replace: true });
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

  // 添加输出参数
  const handleAddOutputParam = () => {
    setOutputParams([...outputParams, { name: '', type: 'input', value: '' }]);
  };

  // 删除输出参数
  const handleRemoveOutputParam = (index: number) => {
    setOutputParams(outputParams.filter((_, i) => i !== index));
  };

  // 更新输出参数
  const handleUpdateOutputParam = (index: number, field: keyof OutputParam, value: string) => {
    const newParams = [...outputParams];
    newParams[index] = { ...newParams[index], [field]: value };
    setOutputParams(newParams);
  };

  // 获取可引用的节点列表（输出节点之前的所有节点）
  const getReferenceableNodes = () => {
    return nodes.filter(node => 
      node.id !== selectedNode?.id && node.data?.type !== 'output'
    );
  };

  // 获取节点的输出参数
  const getNodeOutputParams = (nodeType: string): string[] => {
    switch (nodeType) {
      case 'input':
        return ['user_input'];
      case 'openai':
      case 'deepseek':
      case 'qwen':
      case 'zhipu':
        return ['output', 'tokens'];
      case 'tts':
        return ['audioUrl', 'fileName', 'output'];
      default:
        return ['output'];
    }
  };

  // 获取所有可引用的参数（节点.参数名格式）
  const getReferenceableParams = () => {
    const params: { label: string; value: string }[] = [];
    getReferenceableNodes().forEach(node => {
      const nodeType = (node.data?.type as string) || '';
      const nodeLabel = (node.data?.label as string) || node.id;
      const outputParams = getNodeOutputParams(nodeType);
      
      outputParams.forEach(param => {
        params.push({
          label: `${nodeLabel}.${param}`,
          value: `${node.id}.${param}`
        });
      });
    });
    return params;
  };

  // 保存输出节点配置
  const handleSaveOutputConfig = () => {
    if (!selectedNode) return;

    // 验证参数配置
    for (const param of outputParams) {
      if (!param.name) {
        message.warning('请填写所有参数名');
        return;
      }
      if (param.type === 'input' && !param.value) {
        message.warning('请填写输入值');
        return;
      }
      if (param.type === 'reference' && !param.referenceNode) {
        message.warning('请选择引用参数');
        return;
      }
    }

    // 验证回答内容配置中的参数引用
    const paramNames = new Set(outputParams.map(p => p.name));
    const templateParamRegex = /\{\{(\w+)\}\}/g;
    const matches = responseContent.matchAll(templateParamRegex);
    const undefinedParams: string[] = [];
    
    for (const match of matches) {
      const paramName = match[1];
      if (!paramNames.has(paramName)) {
        undefinedParams.push(paramName);
      }
    }
    
    if (undefinedParams.length > 0) {
      message.warning(`回答内容中引用了未定义的参数: ${undefinedParams.join(', ')}`);
      return;
    }

    // 保存到节点的 data 中
    const updatedData = {
      ...selectedNode.data,
      outputParams,
      responseContent
    };

    console.log('保存输出节点配置:', {
      nodeId: selectedNode.id,
      outputParams,
      responseContent,
      updatedData
    });

    useWorkflowStore.getState().updateNode(selectedNode.id, updatedData);
    message.success('配置保存成功');
  };

  // 打开加载工作流对话框
  const handleOpenLoadModal = async () => {
    setLoadingWorkflows(true);
    setLoadModalOpen(true);
    try {
      const result = await getWorkflows();
      if (result.code === 200) {
        setWorkflows(result.data);
      }
    } catch (error) {
      message.error('获取工作流列表失败');
    } finally {
      setLoadingWorkflows(false);
    }
  };

  // 加载选中的工作流
  const handleLoadWorkflow = (workflow: Workflow) => {
    setLoadModalOpen(false);
    navigate(`/editor/${workflow.id}`);
  };

  // 新建工作流
  const handleCreateNew = () => {
    setCurrentWorkflowId(null);
    setWorkflowName('未命名工作流');
    
    // 创建默认的输入和输出节点(上下排列)
    const defaultNodes = [
      {
        id: 'input-default',
        type: 'default',
        position: { x: 250, y: 100 },
        data: { 
          label: '输入节点',
          type: 'input'
        },
      },
      {
        id: 'output-default',
        type: 'default',
        position: { x: 250, y: 400 },
        data: { 
          label: '输出节点',
          type: 'output',
          outputParams: [],
          responseContent: ''
        },
      },
    ];
    
    setNodes(defaultNodes);
    setEdges([]);
    navigate('/editor');
    message.info('已创建新工作流');
  };

  // 保存 LLM 节点配置
  const handleSaveLlmConfig = () => {
    if (!selectedNode) return;

    // 验证输入参数
    for (const param of llmInputParams) {
      if (!param.name) {
        message.warning('请填写所有参数名');
        return;
      }
      if (param.type === 'input' && !param.value) {
        message.warning('请填写输入值');
        return;
      }
      if (param.type === 'reference' && !param.referenceNode) {
        message.warning('请选择引用参数');
        return;
      }
    }

    // 验证提示词
    if (!llmConfig.prompt) {
      message.warning('请填写提示词模板');
      return;
    }

    // 验证提示词中的参数引用
    const paramNames = new Set(llmInputParams.map(p => p.name));
    const templateParamRegex = /\{\{(\w+)\}\}/g;
    const matches = llmConfig.prompt.matchAll(templateParamRegex);
    const undefinedParams: string[] = [];
    
    for (const match of matches) {
      const paramName = match[1];
      if (!paramNames.has(paramName)) {
        undefinedParams.push(paramName);
      }
    }
    
    if (undefinedParams.length > 0) {
      message.warning(`提示词模板中引用了未定义的参数: ${undefinedParams.join(', ')}`);
      return;
    }

    // 验证 API 配置
    if (!llmConfig.apiUrl) {
      message.warning('请填写 API 地址');
      return;
    }
    if (!llmConfig.apiKey) {
      message.warning('请填写 API 密钥');
      return;
    }
    if (!llmConfig.model) {
      message.warning('请填写模型名称');
      return;
    }

    const updatedData = {
      ...selectedNode.data,
      apiUrl: llmConfig.apiUrl,
      apiKey: llmConfig.apiKey,
      model: llmConfig.model,
      temperature: llmConfig.temperature,
      prompt: llmConfig.prompt,
      maxTokens: llmConfig.maxTokens,
      enableStream: llmConfig.enableStream,
      inputParams: llmInputParams,
      outputParams: llmOutputParams
    };

    useWorkflowStore.getState().updateNode(selectedNode.id, updatedData);
    message.success('配置保存成功');
  };

  // 添加 LLM 输入参数
  const handleAddLlmInputParam = () => {
    setLlmInputParams([...llmInputParams, { name: '', type: 'input', value: '' }]);
  };

  // 删除 LLM 输入参数
  const handleRemoveLlmInputParam = (index: number) => {
    setLlmInputParams(llmInputParams.filter((_, i) => i !== index));
  };

  // 更新 LLM 输入参数
  const handleUpdateLlmInputParam = (index: number, field: keyof LlmInputParam, value: string) => {
    const newParams = [...llmInputParams];
    newParams[index] = { ...newParams[index], [field]: value };
    setLlmInputParams(newParams);
  };

  // 保存 TTS 节点配置
  const handleSaveTtsConfig = () => {
    if (!selectedNode) return;

    if (!ttsConfig.apiKey) {
      message.warning('请填写 API Key');
      return;
    }
    if (!ttsConfig.model) {
      message.warning('请填写模型名称');
      return;
    }

    // 验证输入参数
    for (const param of ttsInputParams) {
      if (!param.name) {
        message.warning('请填写所有参数名');
        return;
      }
      if (param.type === 'input' && !param.value) {
        message.warning('请填写输入值');
        return;
      }
      if (param.type === 'reference' && !param.referenceNode) {
        message.warning('请选择引用参数');
        return;
      }
    }

    // 验证输出参数
    for (const param of ttsOutputParams) {
      if (!param.name) {
        message.warning('请填写所有输出参数名');
        return;
      }
    }

    const updatedData = {
      ...selectedNode.data,
      apiKey: ttsConfig.apiKey,
      model: ttsConfig.model,
      voice: ttsConfig.voice,
      languageType: ttsConfig.languageType,
      inputParams: ttsInputParams,
      outputParams: ttsOutputParams
    };

    useWorkflowStore.getState().updateNode(selectedNode.id, updatedData);
    message.success('配置保存成功');
  };

  // 添加 TTS 输入参数
  const handleAddTtsInputParam = () => {
    setTtsInputParams([...ttsInputParams, { name: '', type: 'input', value: '' }]);
  };

  // 删除 TTS 输入参数
  const handleRemoveTtsInputParam = (index: number) => {
    setTtsInputParams(ttsInputParams.filter((_, i) => i !== index));
  };

  // 更新 TTS 输入参数
  const handleUpdateTtsInputParam = (index: number, field: keyof TtsInputParam, value: string) => {
    const newParams = [...ttsInputParams];
    newParams[index] = { ...newParams[index], [field]: value };
    setTtsInputParams(newParams);
  };

  // 添加 TTS 输出参数
  const handleAddTtsOutputParam = () => {
    setTtsOutputParams([...ttsOutputParams, { name: '', value: '' }]);
  };

  // 删除 TTS 输出参数
  const handleRemoveTtsOutputParam = (index: number) => {
    setTtsOutputParams(ttsOutputParams.filter((_, i) => i !== index));
  };

  // 更新 TTS 输出参数
  const handleUpdateTtsOutputParam = (index: number, field: keyof TtsOutputParam, value: string) => {
    const newParams = [...ttsOutputParams];
    newParams[index] = { ...newParams[index], [field]: value };
    setTtsOutputParams(newParams);
  };

  // 添加 LLM 输出参数
  const handleAddLlmOutputParam = () => {
    setLlmOutputParams([...llmOutputParams, { name: '', type: 'string', description: '' }]);
  };

  // 删除 LLM 输出参数
  const handleRemoveLlmOutputParam = (index: number) => {
    setLlmOutputParams(llmOutputParams.filter((_, i) => i !== index));
  };

  // 更新 LLM 输出参数
  const handleUpdateLlmOutputParam = (index: number, field: keyof LlmOutputParam, value: string) => {
    const newParams = [...llmOutputParams];
    newParams[index] = { ...newParams[index], [field]: value };
    setLlmOutputParams(newParams);
  };

  return (
    <div className="h-screen flex flex-col bg-cyber-black cyber-grid">
      {/* 顶部工具栏 */}
      <div className="bg-cyber-dark/95 shadow-neon-purple px-6 py-4 flex items-center justify-between border-b-2 border-cyber-purple">
        <div className="flex items-center gap-4">
          <h1 className="text-2xl font-bold text-cyber-cyan text-neon">PaiAgent</h1>
          <Input
            value={workflowName}
            onChange={(e) => setWorkflowName(e.target.value)}
            className="w-64"
            placeholder="工作流名称"
            bordered={false}
            style={{ borderBottom: '2px solid #b026ff', color: '#e0e0e0' }}
          />
        </div>

        <div className="flex items-center gap-3">
          <Button
            icon={<PlusOutlined />}
            onClick={handleCreateNew}
            size="large"
            className="border-cyber-purple text-cyber-purple hover:bg-cyber-purple/20"
          >
            新建
          </Button>
          <Button
            icon={<FolderOpenOutlined />}
            onClick={handleOpenLoadModal}
            size="large"
            className="border-cyber-cyan text-cyber-cyan hover:bg-cyber-cyan/20"
          >
            加载
          </Button>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            onClick={handleSave}
            loading={saving}
            size="large"
          >
            保存
          </Button>
          <Button
            type="primary"
            icon={<BugOutlined />}
            onClick={handleOpenDebug}
            disabled={!currentWorkflowId}
            size="large"
          >
            调试
          </Button>
          <div className="ml-4 flex items-center gap-2 px-3 py-1 bg-cyber-gray/30 rounded-lg border border-cyber-purple/20 border border-cyber-purple/50">
            <span className="text-cyber-cyan">👤 {username}</span>
            <Button
              icon={<LogoutOutlined />}
              onClick={handleLogout}
              type="text"
              className="text-cyber-pink hover:text-cyber-pink"
            >
              登出
            </Button>
          </div>
        </div>
      </div>

      {/* 主要内容区域 */}
      <div className="flex-1 flex overflow-hidden gap-4 p-4">
        {/* 左侧节点面板 */}
        <div className="w-64 flex-shrink-0 bg-cyber-dark/95 rounded-lg shadow-neon-purple overflow-hidden border border-cyber-purple/30">
          <NodePanel onDragStart={handleDragStart} />
        </div>

        {/* 中间画布 */}
        <div className="flex-1 bg-cyber-dark/95 rounded-lg shadow-neon-cyan overflow-hidden border border-cyber-cyan/30">
          <FlowCanvas onNodeClick={handleNodeClick} />
        </div>

        {/* 右侧配置面板 */}
        <div className="w-[420px] flex-shrink-0 bg-cyber-dark/95 rounded-lg shadow-neon-pink border border-cyber-pink/30 flex flex-col">
          {/* 标题 */}
          <div className="px-4 py-3 border-b border-cyber-pink/30 bg-cyber-dark/50">
            <h3 className="font-semibold text-cyber-cyan text-glow text-base">节点配置</h3>
          </div>

          {/* 内容区 */}
          <div className="flex-1 overflow-y-auto px-4 py-3">
            {selectedNode ? (
              <div>
                <div className="mb-3 p-3 bg-cyber-gray/30 rounded-lg border border-cyber-purple/30">
                  <p className="text-xs text-cyber-cyan mb-1 opacity-70">节点 ID</p>
                  <p className="text-sm text-cyber-pink font-medium break-all">{selectedNode.id}</p>
                </div>
                <div className="mb-4 p-3 bg-cyber-gray/30 rounded-lg border border-cyber-purple/30">
                  <p className="text-xs text-cyber-cyan mb-1 opacity-70">节点类型</p>
                  <p className="text-sm text-cyber-pink font-medium">{String(selectedNode.data?.type || '')}</p>
                </div>
                
                {/* 输入节点配置 */}
                {selectedNode.data?.type === 'input' && (
                  <Form layout="vertical" className="mt-4">
                    <Form.Item label="变量名">
                      <Input value="user_input" disabled />
                    </Form.Item>
                    <Form.Item label="变量类型">
                      <Input value="String" disabled />
                    </Form.Item>
                    <Form.Item label="描述">
                      <Input.TextArea value="用户本轮的输入内容" disabled rows={2} />
                    </Form.Item>
                    <Form.Item label="是否必要">
                      <Checkbox checked disabled>必要</Checkbox>
                    </Form.Item>
                  </Form>
                )}

                {/* 输出节点配置 */}
                {selectedNode.data?.type === 'output' && (
                  <Form layout="vertical" className="mt-4">
                    {/* 输出配置 */}
                    <div className="mb-5">
                      <div className="flex justify-between items-center mb-2">
                        <label className="font-medium text-cyber-cyan text-sm">输出配置</label>
                        <Button 
                          type="dashed" 
                          size="small" 
                          icon={<PlusOutlined />}
                          onClick={handleAddOutputParam}
                        >
                          添加
                        </Button>
                      </div>
                      
                      {outputParams.map((param, index) => (
                        <Space key={index} className="w-full mb-3" align="start">
                          <div>
                            <Input 
                              placeholder="参数名"
                              value={param.name}
                              onChange={(e) => handleUpdateOutputParam(index, 'name', e.target.value)}
                              style={{ width: '100px' }}
                            />
                          </div>
                          <div>
                            <Select
                              value={param.type}
                              onChange={(value) => handleUpdateOutputParam(index, 'type', value)}
                              style={{ width: '80px' }}
                            >
                              <Select.Option value="input">输入</Select.Option>
                              <Select.Option value="reference">引用</Select.Option>
                            </Select>
                          </div>
                          <div className="flex-1">
                            {param.type === 'input' ? (
                              <Input 
                                placeholder="输入值"
                                value={param.value}
                                onChange={(e) => handleUpdateOutputParam(index, 'value', e.target.value)}
                              />
                            ) : (
                              <Select
                                placeholder="选择参数"
                                value={param.referenceNode}
                                onChange={(value) => handleUpdateOutputParam(index, 'referenceNode', value)}
                                className="w-full"
                              >
                                {getReferenceableParams().map(param => (
                                  <Select.Option key={param.value} value={param.value}>
                                    {param.label}
                                  </Select.Option>
                                ))}
                              </Select>
                            )}
                          </div>
                          <Button 
                            type="text" 
                            danger 
                            size="small"
                            icon={<DeleteOutlined />}
                            onClick={() => handleRemoveOutputParam(index)}
                          />
                        </Space>
                      ))}
                      
                      {outputParams.length === 0 && (
                        <div className="text-cyber-pink/60 text-center py-8 border border-dashed border-cyber-purple/50 rounded">
                          点击"添加"按钮添加输出参数
                        </div>
                      )}
                    </div>

                    {/* 回答内容配置 */}
                    <Form.Item label="回答内容配置">
                      <Input.TextArea 
                        rows={6}
                        placeholder="使用 {{参数名}} 引用输出配置中的参数"
                        value={responseContent}
                        onChange={(e) => setResponseContent(e.target.value)}
                      />
                      <div className="mt-1 text-xs text-cyber-pink/50">
                        💡 提示: 使用 {'{{'} 参数名 {'}'} 引用上面定义的参数
                      </div>
                    </Form.Item>

                    <Button type="primary" block onClick={handleSaveOutputConfig}>
                      保存配置
                    </Button>
                  </Form>
                )}

                {/* LLM 节点配置 (OpenAI/DeepSeek/Qwen/Zhipu) */}
                {(selectedNode.data?.type === 'openai' || selectedNode.data?.type === 'deepseek' || selectedNode.data?.type === 'qwen' || selectedNode.data?.type === 'zhipu') && (
                  <Form layout="vertical" className="mt-4">
                    {/* 输入参数配置 */}
                    <div className="mb-5">
                      <div className="flex justify-between items-center mb-2">
                        <label className="font-medium text-cyber-cyan text-sm">输入参数</label>
                        <Button 
                          type="dashed" 
                          size="small" 
                          icon={<PlusOutlined />}
                          onClick={handleAddLlmInputParam}
                        >
                          添加
                        </Button>
                      </div>
                      
                      {llmInputParams.map((param, index) => (
                        <div key={index} className="flex items-start gap-2 mb-3">
                          <Input 
                            placeholder="参数名"
                            value={param.name}
                            onChange={(e) => handleUpdateLlmInputParam(index, 'name', e.target.value)}
                            style={{ width: '90px' }}
                          />
                          <Select
                            value={param.type}
                            onChange={(value) => handleUpdateLlmInputParam(index, 'type', value)}
                            style={{ width: '70px' }}
                          >
                            <Select.Option value="input">输入</Select.Option>
                            <Select.Option value="reference">引用</Select.Option>
                          </Select>
                          <div className="flex-1">
                            {param.type === 'input' ? (
                              <Input 
                                placeholder="输入值"
                                value={param.value}
                                onChange={(e) => handleUpdateLlmInputParam(index, 'value', e.target.value)}
                              />
                            ) : (
                              <Select
                                placeholder="选择参数"
                                value={param.referenceNode}
                                onChange={(value) => handleUpdateLlmInputParam(index, 'referenceNode', value)}
                                className="w-full"
                              >
                                {getReferenceableParams().map(p => (
                                  <Select.Option key={p.value} value={p.value}>
                                    {p.label}
                                  </Select.Option>
                                ))}
                              </Select>
                            )}
                          </div>
                          <Button 
                            type="text" 
                            danger 
                            size="small"
                            icon={<DeleteOutlined />}
                            onClick={() => handleRemoveLlmInputParam(index)}
                          />
                        </div>
                      ))}
                      
                      {llmInputParams.length === 0 && (
                        <div className="text-cyber-pink/60 text-center py-8 border border-dashed border-cyber-purple/50 rounded">
                          点击"添加"按钮添加输入参数
                        </div>
                      )}
                    </div>

                    {/* 输出参数配置 */}
                    <div className="mb-5">
                      <div className="flex justify-between items-center mb-2">
                        <label className="font-medium text-cyber-cyan text-sm">输出参数</label>
                        <Button 
                          type="dashed" 
                          size="small" 
                          icon={<PlusOutlined />}
                          onClick={handleAddLlmOutputParam}
                        >
                          添加
                        </Button>
                      </div>
                      
                      {llmOutputParams.map((param, index) => (
                        <div key={index} className="flex items-start gap-2 mb-3">
                          <Input 
                            placeholder="变量名"
                            value={param.name}
                            onChange={(e) => handleUpdateLlmOutputParam(index, 'name', e.target.value)}
                            style={{ width: '100px' }}
                          />
                          <Input
                            value="string"
                            disabled
                            style={{ width: '70px' }}
                          />
                          <div className="flex-1">
                            <Input 
                              placeholder="描述（可选）"
                              value={param.description}
                              onChange={(e) => handleUpdateLlmOutputParam(index, 'description', e.target.value)}
                            />
                          </div>
                          <Button 
                            type="text" 
                            danger 
                            size="small"
                            icon={<DeleteOutlined />}
                            onClick={() => handleRemoveLlmOutputParam(index)}
                          />
                        </div>
                      ))}
                      
                      {llmOutputParams.length === 0 && (
                        <div className="text-cyber-pink/60 text-center py-8 border border-dashed border-cyber-purple/50 rounded">
                          点击"添加"按钮添加输出参数
                        </div>
                      )}
                    </div>

                    <Form.Item label="提示词模板" required>
                      <Input.TextArea 
                        rows={12} 
                        placeholder="输入提示词模板，使用 {{参数名}} 引用输入参数"
                        value={llmConfig.prompt}
                        onChange={(e) => setLlmConfig({...llmConfig, prompt: e.target.value})}
                        style={{ fontFamily: 'monospace', fontSize: '12px' }}
                      />
                      <div className="text-xs text-cyber-pink/50 mt-1">
                        💡 使用 {'{{'} 参数名 {'}'} 引用上面定义的输入参数
                      </div>
                    </Form.Item>
                    <Form.Item label="API 地址" required>
                      <Input 
                        placeholder="例如: https://api.deepseek.com"
                        value={llmConfig.apiUrl}
                        onChange={(e) => setLlmConfig({...llmConfig, apiUrl: e.target.value})}
                      />
                    </Form.Item>
                    <Form.Item label="API 密钥" required>
                      <Input.Password 
                        placeholder="输入 API Key"
                        value={llmConfig.apiKey}
                        onChange={(e) => setLlmConfig({...llmConfig, apiKey: e.target.value})}
                      />
                    </Form.Item>
                    <Form.Item label="模型名称" required>
                      <Input 
                        placeholder="例如: deepseek-chat"
                        value={llmConfig.model}
                        onChange={(e) => setLlmConfig({...llmConfig, model: e.target.value})}
                      />
                    </Form.Item>
                    <Form.Item label="温度">
                      <Input
                        type="number"
                        step="0.1"
                        min="0"
                        max="2"
                        value={llmConfig.temperature}
                        onChange={(e) => setLlmConfig({...llmConfig, temperature: parseFloat(e.target.value) || 0.7})}
                      />
                      <div className="text-xs text-cyber-pink/50 mt-1">
                        控制输出随机性，范围 0-2，值越高越随机
                      </div>
                    </Form.Item>
                    <Form.Item label="最大 Tokens">
                      <Input
                        type="number"
                        min="1"
                        max="128000"
                        step="1"
                        value={llmConfig.maxTokens}
                        onChange={(e) => setLlmConfig({...llmConfig, maxTokens: parseInt(e.target.value) || 2048})}
                      />
                      <div className="text-xs text-cyber-pink/50 mt-1">
                        限制输出最大 token 数，默认 2048
                      </div>
                    </Form.Item>
                    <Form.Item label="流式响应">
                      <Checkbox
                        checked={llmConfig.enableStream}
                        onChange={(e) => setLlmConfig({...llmConfig, enableStream: e.target.checked})}
                      >
                        启用流式响应（速度更快）
                      </Checkbox>
                      <div className="text-xs text-cyber-pink/50 mt-1">
                        启用后会更快返回结果，数据逐块生成
                      </div>
                    </Form.Item>
                    <Button type="primary" block onClick={handleSaveLlmConfig}>
                      保存配置
                    </Button>
                  </Form>
                )}

                {/* TTS 节点配置 (超拟人音频) */}
                {selectedNode.data?.type === 'tts' && (
                  <Form layout="vertical" className="mt-4">
                    {/* 输入配置 */}
                    <div className="mb-5">
                      <div className="flex justify-between items-center mb-2">
                        <label className="font-medium text-cyber-cyan text-sm">输入配置</label>
                        <Button 
                          type="dashed" 
                          size="small" 
                          icon={<PlusOutlined />}
                          onClick={handleAddTtsInputParam}
                        >
                          添加
                        </Button>
                      </div>
                      
                      {ttsInputParams.map((param, index) => (
                        <div key={index} className="mb-3 p-3 bg-cyber-gray/30 rounded-lg border border-cyber-purple/30">
                          <div className="flex items-center gap-2 mb-2">
                            <Input 
                              placeholder="参数名 (如: text)"
                              value={param.name}
                              onChange={(e) => handleUpdateTtsInputParam(index, 'name', e.target.value)}
                              style={{ width: 120 }}
                            />
                            <Select
                              value={param.type}
                              onChange={(value) => handleUpdateTtsInputParam(index, 'type', value)}
                              style={{ width: 100 }}
                            >
                              <Select.Option value="input">输入</Select.Option>
                              <Select.Option value="reference">引用</Select.Option>
                            </Select>
                            <Button 
                              type="text" 
                              danger 
                              icon={<DeleteOutlined />}
                              onClick={() => handleRemoveTtsInputParam(index)}
                            />
                          </div>
                          <div>
                            {param.type === 'input' ? (
                              <Input.TextArea
                                placeholder="输入值"
                                value={param.value}
                                onChange={(e) => handleUpdateTtsInputParam(index, 'value', e.target.value)}
                                rows={2}
                              />
                            ) : (
                              <Select
                                placeholder="选择引用参数"
                                value={param.referenceNode}
                                onChange={(value) => handleUpdateTtsInputParam(index, 'referenceNode', value)}
                                style={{ width: '100%' }}
                              >
                                {getReferenceableParams().map((p) => (
                                  <Select.Option key={p.value} value={p.value}>
                                    {p.label}
                                  </Select.Option>
                                ))}
                              </Select>
                            )}
                          </div>
                        </div>
                      ))}
                      
                      {ttsInputParams.length === 0 && (
                        <div className="text-center py-8 text-cyber-pink/60 text-sm border border-dashed border-cyber-purple/30 rounded">
                          暂无输入参数,点击"添加"按钮创建 text 参数
                        </div>
                      )}
                      
                      {/* 固定配置项 */}
                      <div className="mt-4 space-y-3">
                        <Form.Item label="音色 (voice)" className="mb-0">
                          <Select
                            value={ttsConfig.voice}
                            onChange={(value) => setTtsConfig({ ...ttsConfig, voice: value })}
                          >
                            <Select.Option value="Cherry">Cherry (芊悦)</Select.Option>
                            <Select.Option value="Serena">Serena (苏瑶)</Select.Option>
                            <Select.Option value="Ethan">Ethan (晨煦)</Select.Option>
                            <Select.Option value="Chelsie">Chelsie (千雪)</Select.Option>
                            <Select.Option value="Momo">Momo (茉兔)</Select.Option>
                            <Select.Option value="Vivian">Vivian (十三)</Select.Option>
                            <Select.Option value="Moon">Moon (月白)</Select.Option>
                            <Select.Option value="Maia">Maia (四月)</Select.Option>
                            <Select.Option value="Kai">Kai (凯)</Select.Option>
                            <Select.Option value="Nofish">Nofish (不吃鱼)</Select.Option>
                            <Select.Option value="Bella">Bella (萌宝)</Select.Option>
                            <Select.Option value="Jennifer">Jennifer (詹妮弗)</Select.Option>
                            <Select.Option value="Ryan">Ryan (甜茶)</Select.Option>
                            <Select.Option value="Katerina">Katerina (卡捷琳娜)</Select.Option>
                            <Select.Option value="Aiden">Aiden (艾登)</Select.Option>
                          </Select>
                        </Form.Item>

                        <Form.Item label="语言类型 (language_type)" className="mb-0">
                          <Select
                            value={ttsConfig.languageType}
                            onChange={(value) => setTtsConfig({ ...ttsConfig, languageType: value })}
                          >
                            <Select.Option value="Auto">Auto</Select.Option>
                          </Select>
                        </Form.Item>
                      </div>
                    </div>

                    {/* 输出配置 */}
                    <div className="mb-5">
                      <div className="flex justify-between items-center mb-2">
                        <label className="font-medium text-cyber-cyan text-sm">输出配置</label>
                        <Button 
                          type="dashed" 
                          size="small" 
                          icon={<PlusOutlined />}
                          onClick={handleAddTtsOutputParam}
                        >
                          添加
                        </Button>
                      </div>
                      
                      {ttsOutputParams.map((param, index) => (
                        <div key={index} className="flex items-center gap-2 mb-3">
                          <Input 
                            placeholder="参数名 (如: voice_url)"
                            value={param.name}
                            onChange={(e) => handleUpdateTtsOutputParam(index, 'name', e.target.value)}
                            style={{ flex: 1 }}
                          />
                          <Input
                            placeholder="参数值 (引用字段,如: audioUrl)"
                            value={param.value}
                            onChange={(e) => handleUpdateTtsOutputParam(index, 'value', e.target.value)}
                            style={{ flex: 1 }}
                          />
                          <Button 
                            type="text" 
                            danger 
                            icon={<DeleteOutlined />}
                            onClick={() => handleRemoveTtsOutputParam(index)}
                          />
                        </div>
                      ))}
                      
                      {ttsOutputParams.length === 0 && (
                        <div className="text-center py-8 text-cyber-pink/60 text-sm border border-dashed border-cyber-purple/30 rounded">
                          暂无输出参数,点击"添加"按钮创建 voice_url 参数
                        </div>
                      )}
                    </div>

                    {/* 基本信息 */}
                    <div className="mb-4">
                      <label className="font-medium text-cyber-cyan block mb-3">基本信息</label>
                      <Form.Item label="API Key">
                        <Input.Password
                          placeholder="请输入阿里百炼 API Key"
                          value={ttsConfig.apiKey}
                          onChange={(e) => setTtsConfig({ ...ttsConfig, apiKey: e.target.value })}
                        />
                      </Form.Item>
                      <Form.Item label="模型名称">
                        <Input
                          placeholder="请输入模型名称"
                          value={ttsConfig.model}
                          onChange={(e) => setTtsConfig({ ...ttsConfig, model: e.target.value })}
                        />
                      </Form.Item>
                    </div>

                    <Button type="primary" block onClick={handleSaveTtsConfig}>
                      保存配置
                    </Button>
                  </Form>
                )}

                {/* 其他节点配置 */}
                {selectedNode.data?.type !== 'input' &&
                 selectedNode.data?.type !== 'output' &&
                 selectedNode.data?.type !== 'openai' &&
                 selectedNode.data?.type !== 'deepseek' &&
                 selectedNode.data?.type !== 'qwen' &&
                 selectedNode.data?.type !== 'zhipu' &&
                 selectedNode.data?.type !== 'tts' && (
                  <div className="mt-4 text-center text-cyber-pink/60 text-sm">
                    该节点暂无可配置项
                  </div>
                )}
              </div>
            ) : (
              <div className="text-center py-12">
                <p className="text-cyber-pink/60 text-sm">请选择一个节点</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 调试抽屉 */}
      <DebugDrawer
        open={debugDrawerOpen}
        onClose={() => setDebugDrawerOpen(false)}
        onExecute={handleExecute}
      />

      {/* 加载工作流对话框 */}
      <Modal
        title="加载工作流"
        open={loadModalOpen}
        onCancel={() => setLoadModalOpen(false)}
        footer={null}
        width={600}
      >
        <List
          loading={loadingWorkflows}
          dataSource={workflows}
          renderItem={(workflow) => (
            <List.Item
              key={workflow.id}
              actions={[
                <Button type="link" onClick={() => handleLoadWorkflow(workflow)}>
                  加载
                </Button>
              ]}
            >
              <List.Item.Meta
                title={workflow.name}
                description={`创建于: ${new Date(workflow.createdAt).toLocaleString()}`}
              />
            </List.Item>
          )}
        />
      </Modal>
    </div>
  );
};

export default EditorPage;