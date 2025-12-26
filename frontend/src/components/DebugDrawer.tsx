import { useState } from 'react';
import { Drawer, Input, Button, Card, Timeline, Progress, Tag, Collapse, Alert } from 'antd';
import { PlayCircleOutlined, CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons';
import AudioPlayer from './AudioPlayer';
import { ExecutionEvent, executeWorkflowStream } from '../api/workflow';
import { useWorkflowStore } from '../store/workflowStore';

const { TextArea } = Input;

interface NodeResult {
  nodeId: string;
  nodeName: string;
  status: 'SUCCESS' | 'FAILED' | 'RUNNING';
  input: Record<string, unknown>;
  output: Record<string, unknown>;
  duration: number;
  error?: string;
}

interface ExecutionResponse {
  executionId: number;
  status: 'SUCCESS' | 'FAILED' | 'RUNNING';
  nodeResults: NodeResult[];
  outputData: Record<string, unknown>;
  duration: number;
  errorMessage?: string;
}

interface DebugDrawerProps {
  open: boolean;
  onClose: () => void;
  onExecute: (inputData: string) => Promise<ExecutionResponse>;
}

const DebugDrawer = ({ open, onClose, onExecute }: DebugDrawerProps) => {
  const [inputData, setInputData] = useState('');
  const [executing, setExecuting] = useState(false);
  const [executionResult, setExecutionResult] = useState<ExecutionResponse | null>(null);
  const [logs, setLogs] = useState<string[]>([]);
  const [nodeStatusMap, setNodeStatusMap] = useState<Map<string, NodeResult>>(new Map());
  const { currentWorkflowId } = useWorkflowStore();

  const addLog = (message: string) => {
    const timestamp = new Date().toLocaleTimeString();
    setLogs((prev) => [...prev, `[${timestamp}] ${message}`]);
  };

  const handleExecute = async () => {
    if (!inputData.trim()) {
      addLog('❌ 错误: 输入数据不能为空');
      return;
    }

    if (!currentWorkflowId) {
      addLog('❌ 错误: 请先保存工作流');
      return;
    }

    setExecuting(true);
    setExecutionResult(null);
    setLogs([]);
    setNodeStatusMap(new Map());
    addLog('🚀 开始执行工作流...');

    try {
      const nodeResults: NodeResult[] = [];
      const tempNodeStatusMap = new Map<string, NodeResult>();
      
      executeWorkflowStream(
        currentWorkflowId,
        inputData,
        (event: ExecutionEvent) => {
          console.log('收到事件:', event);
          
          switch (event.eventType) {
            case 'WORKFLOW_START':
              addLog('🚀 工作流开始执行');
              break;
              
            case 'NODE_START':
              addLog(`📍 节点 [${event.nodeName}] 开始执行...`);
              if (event.nodeId && event.nodeName) {
                const nodeResult: NodeResult = {
                  nodeId: event.nodeId,
                  nodeName: event.nodeName,
                  status: 'RUNNING',
                  input: {},
                  output: {},
                  duration: 0
                };
                tempNodeStatusMap.set(event.nodeId, nodeResult);
                setNodeStatusMap(new Map(tempNodeStatusMap));
              }
              break;
              
            case 'NODE_SUCCESS':
              if (event.nodeId && event.nodeName) {
                const duration = event.message?.match(/耗时 (\d+)ms/)?.[1] || '0';
                addLog(`✅ 节点 [${event.nodeName}] 执行成功,耗时 ${duration}ms`);
                
                const eventData = event.data as any;
                const nodeResult: NodeResult = {
                  nodeId: event.nodeId,
                  nodeName: event.nodeName,
                  status: 'SUCCESS',
                  input: eventData?.input || {},
                  output: eventData?.output || event.data || {},
                  duration: parseInt(duration)
                };
                tempNodeStatusMap.set(event.nodeId, nodeResult);
                nodeResults.push(nodeResult);
                setNodeStatusMap(new Map(tempNodeStatusMap));
              }
              break;
              
            case 'NODE_PROGRESS':
              if (event.nodeId && event.message) {
                addLog(`📊 ${event.message}`);
                const existingNode = tempNodeStatusMap.get(event.nodeId);
                if (existingNode) {
                  existingNode.status = 'RUNNING';
                  setNodeStatusMap(new Map(tempNodeStatusMap));
                }
              }
              break;
              
            case 'NODE_ERROR':
              if (event.nodeId && event.nodeName) {
                addLog(`❌ 节点 [${event.nodeName}] 执行失败: ${event.message}`);
                const nodeResult: NodeResult = {
                  nodeId: event.nodeId,
                  nodeName: event.nodeName,
                  status: 'FAILED',
                  input: {},
                  output: {},
                  duration: 0,
                  error: event.message
                };
                tempNodeStatusMap.set(event.nodeId, nodeResult);
                nodeResults.push(nodeResult);
                setNodeStatusMap(new Map(tempNodeStatusMap));
              }
              break;
              
            case 'WORKFLOW_COMPLETE':
              const totalDuration = event.message?.match(/总耗时 (\d+)ms/)?.[1] || '0';
              addLog(`${event.status === 'SUCCESS' ? '✅' : '❌'} 工作流执行${event.status === 'SUCCESS' ? '成功' : '失败'},总耗时 ${totalDuration}ms`);
              
              setExecutionResult({
                executionId: 0,
                status: event.status as 'SUCCESS' | 'FAILED',
                nodeResults: Array.from(tempNodeStatusMap.values()),
                outputData: event.data || {},
                duration: parseInt(totalDuration),
                errorMessage: event.status === 'FAILED' ? event.message : undefined
              });
              break;
          }
        },
        () => {
          setExecuting(false);
        },
        (error: Error) => {
          const errorMsg = error.message.includes('连接失败') 
            ? '连接失败,请检查后端服务是否运行或重新登录' 
            : error.message;
          addLog(`❌ 执行异常: ${errorMsg}`);
          setExecuting(false);
        }
      );
    } catch (error) {
      addLog(`❌ 执行异常: ${error instanceof Error ? error.message : '未知错误'}`);
      setExecuting(false);
    }
  };

  const getProgress = () => {
    if (!executionResult) {
      const total = nodeStatusMap.size;
      if (total === 0) return 0;
      const completed = Array.from(nodeStatusMap.values()).filter((r) => r.status === 'SUCCESS').length;
      return Math.round((completed / total) * 100);
    }
    const total = executionResult.nodeResults.length;
    if (total === 0) return 0;
    const completed = executionResult.nodeResults.filter((r) => r.status === 'SUCCESS').length;
    return Math.round((completed / total) * 100);
  };

  const renderNodeResultItem = (nodeResult: NodeResult) => {
    let statusColor = 'default';
    let statusIcon = <LoadingOutlined />;
    
    if (nodeResult.status === 'SUCCESS') {
      statusColor = 'success';
      statusIcon = <CheckCircleOutlined />;
    } else if (nodeResult.status === 'FAILED') {
      statusColor = 'error';
      statusIcon = <CloseCircleOutlined />;
    } else if (nodeResult.status === 'RUNNING') {
      statusColor = 'processing';
      statusIcon = <LoadingOutlined />;
    }

    return {
      key: nodeResult.nodeId,
      label: (
        <div className="flex items-center justify-between">
          <span>
            {statusIcon} {nodeResult.nodeName}
          </span>
          <Tag color={statusColor}>{nodeResult.duration}ms</Tag>
        </div>
      ),
      children: (
        <div className="space-y-2">
          <div>
            <div className="text-gray-600 text-xs mb-1">输入数据:</div>
            <pre className="bg-gray-50 p-2 rounded text-xs overflow-auto max-h-32">
              {JSON.stringify(nodeResult.input, null, 2)}
            </pre>
          </div>
          <div>
            <div className="text-gray-600 text-xs mb-1">输出数据:</div>
            <pre className="bg-gray-50 p-2 rounded text-xs overflow-auto max-h-32">
              {JSON.stringify(nodeResult.output, null, 2)}
            </pre>
          </div>
          {nodeResult.error && (
            <Alert message="错误信息" description={nodeResult.error} type="error" showIcon />
          )}
        </div>
      ),
    };
  };

  const currentNodeResults = executionResult 
    ? executionResult.nodeResults 
    : Array.from(nodeStatusMap.values());

  return (
    <Drawer
      title="调试面板"
      placement="right"
      onClose={onClose}
      open={open}
      width={450}
      styles={{ body: { padding: 0 } }}
    >
      <div className="flex flex-col h-full">
        <div className="p-4 border-b border-gray-200">
          <Card title="输入测试文本" size="small">
            <TextArea
              rows={4}
              placeholder="请输入测试文本,例如: 人工智能的未来发展"
              value={inputData}
              onChange={(e) => setInputData(e.target.value)}
              disabled={executing}
            />
            <Button
              type="primary"
              icon={<PlayCircleOutlined />}
              onClick={handleExecute}
              loading={executing}
              block
              className="mt-2"
            >
              {executing ? '执行中...' : '执行工作流'}
            </Button>
          </Card>
        </div>

        {(executing || executionResult) && (
          <div className="p-4 border-b border-gray-200">
            <Card title="执行状态" size="small">
              {executing && !executionResult && (
                <div className="flex items-center gap-2">
                  <LoadingOutlined className="text-blue-500" />
                  <span>执行中...</span>
                </div>
              )}
              {(executionResult || nodeStatusMap.size > 0) && (
                <>
                  <div className="flex items-center justify-between mb-2">
                    <span>
                      状态:{' '}
                      <Tag color={executionResult?.status === 'SUCCESS' ? 'success' : executionResult?.status === 'FAILED' ? 'error' : 'processing'}>
                        {executionResult?.status === 'SUCCESS' ? '成功' : executionResult?.status === 'FAILED' ? '失败' : '执行中'}
                      </Tag>
                    </span>
                    {executionResult && <span className="text-gray-600 text-sm">耗时: {executionResult.duration}ms</span>}
                  </div>
                  <Progress 
                    percent={getProgress()} 
                    status={executionResult?.status === 'SUCCESS' ? 'success' : executionResult?.status === 'FAILED' ? 'exception' : 'active'} 
                  />
                  <div className="mt-2 text-sm text-gray-600">
                    已完成节点: {currentNodeResults.filter((r) => r.status === 'SUCCESS').length} / {currentNodeResults.length}
                  </div>
                </>
              )}
            </Card>
          </div>
        )}

        {currentNodeResults.length > 0 && (
          <div className="p-4 border-b border-gray-200">
            <Card title="节点执行结果" size="small">
              <Collapse
                items={currentNodeResults.map(renderNodeResultItem)}
                defaultActiveKey={currentNodeResults.map((r) => r.nodeId)}
              />
            </Card>
          </div>
        )}

        {executionResult && executionResult.status === 'SUCCESS' && (
          <div className="p-4 border-b border-gray-200">
            <Card title="最终输出" size="small">
              {(() => {
                let audioUrl: string | null = null;
                let fileName: string | undefined = undefined;
                
                let outputData = executionResult.outputData;
                if (typeof outputData === 'string') {
                  try {
                    outputData = JSON.parse(outputData);
                  } catch (e) {
                    console.error('Failed to parse outputData:', e);
                  }
                }
                
                if (typeof outputData === 'object' && outputData !== null) {
                  fileName = outputData.fileName as string | undefined;
                  
                  if (outputData.audioUrl && typeof outputData.audioUrl === 'string') {
                    audioUrl = outputData.audioUrl;
                  }
                  
                  if (!audioUrl && outputData.output && typeof outputData.output === 'string') {
                    const output = outputData.output;
                    if (output.includes('http://') || output.includes('https://')) {
                      audioUrl = output;
                    } else if (output.includes('<audio') && output.includes('src=')) {
                      const srcMatch = output.match(/src="([^"]+)"/);
                      if (srcMatch && srcMatch[1]) {
                        audioUrl = srcMatch[1];
                      }
                    } else if (output.startsWith('/audio/')) {
                      audioUrl = 'http://localhost:8080' + output;
                    }
                  }
                }
                
                console.log('检测到的 audioUrl:', audioUrl);
                
                if (audioUrl) {
                  return (
                    <AudioPlayer 
                      audioUrl={audioUrl}
                      fileName={fileName}
                    />
                  );
                }
                
                return (
                  <pre className="bg-gray-50 p-2 rounded text-xs overflow-auto max-h-48">
                    {JSON.stringify(executionResult.outputData, null, 2)}
                  </pre>
                );
              })()}
            </Card>
          </div>
        )}

        <div className="p-4 bg-gray-50">
          <Card title="执行日志" size="small">
            <Timeline
              items={logs.map((log, index) => ({
                key: index,
                children: <span className="text-xs font-mono">{log}</span>,
                color: log.includes('❌') ? 'red' : log.includes('✅') ? 'green' : 'blue',
              }))}
            />
            {logs.length === 0 && (
              <div className="text-gray-400 text-center py-4">暂无日志</div>
            )}
          </Card>
        </div>
      </div>
    </Drawer>
  );
};

export default DebugDrawer;