import { useState, useEffect, useRef } from 'react';
import { Drawer, Input, Button, Card, Timeline, Progress, Tag, Collapse, Alert } from 'antd';
import { PlayCircleOutlined, CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons';
import AudioPlayer from './AudioPlayer';

const { TextArea } = Input;

/**
 * 节点执行结果
 */
interface NodeResult {
  nodeId: string;
  nodeName: string;
  status: 'SUCCESS' | 'FAILED' | 'RUNNING';
  input: Record<string, unknown>;
  output?: Record<string, unknown>;
  duration?: number;
  error?: string;
}

/**
 * 执行响应
 */
interface ExecutionResponse {
  executionId: number;
  status: 'SUCCESS' | 'FAILED';
  nodeResults: NodeResult[];
  outputData: Record<string, unknown>;
  duration: number;
  errorMessage?: string;
}

/**
 * WebSocket 消息
 */
interface WebSocketMessage {
  type: 'START' | 'NODE_START' | 'NODE_COMPLETE' | 'PROGRESS' | 'COMPLETE' | 'ERROR';
  nodeId?: string;
  nodeName?: string;
  status?: string;
  input?: Record<string, unknown>;
  output?: Record<string, unknown>;
  error?: string;
  duration?: number;
  progress?: number;
  message?: string;
  timestamp?: number;
}

interface DebugDrawerProps {
  open: boolean;
  onClose: () => void;
  onExecute: (inputData: string) => Promise<ExecutionResponse>;
}

/**
 * 调试抽屉组件
 */
const DebugDrawer = ({ open, onClose, onExecute }: DebugDrawerProps) => {
  const [inputData, setInputData] = useState('');
  const [executing, setExecuting] = useState(false);
  const [executionResult, setExecutionResult] = useState<ExecutionResponse | null>(null);
  const [logs, setLogs] = useState<string[]>([]);
  const [nodeResults, setNodeResults] = useState<NodeResult[]>([]);
  const [progress, setProgress] = useState(0);

  const wsRef = useRef<WebSocket | null>(null);
  const logContainerRef = useRef<HTMLDivElement>(null);

  // 初始化 WebSocket
  useEffect(() => {
    if (open && !wsRef.current) {
      const ws = new WebSocket('ws://localhost:8080/ws/execution');

      ws.onopen = () => {
        console.log('WebSocket 连接成功');
      };

      ws.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data);
          console.log('收到 WebSocket 消息:', message);

          switch (message.type) {
            case 'START':
              addLog('🚀 开始执行工作流...');
              setNodeResults([]);
              setProgress(0);
              break;

            case 'NODE_START':
              addLog(`⚡ 节点 [${message.nodeName}] 开始执行...`);
              setNodeResults(prev => [...prev, {
                nodeId: message.nodeId!,
                nodeName: message.nodeName!,
                status: 'RUNNING',
                input: message.input || {},
              }]);
              break;

            case 'NODE_COMPLETE':
              if (message.status === 'SUCCESS') {
                addLog(`✅ 节点 [${message.nodeName}] 执行成功, 耗时 ${message.duration}ms`);
              } else {
                addLog(`❌ 节点 [${message.nodeName}] 执行失败: ${message.error}`);
              }
              setNodeResults(prev => prev.map(node =>
                node.nodeId === message.nodeId
                  ? {
                      ...node,
                      status: message.status as 'SUCCESS' | 'FAILED',
                      output: message.output,
                      duration: message.duration,
                      error: message.error,
                    }
                  : node
              ));
              break;

            case 'PROGRESS':
              setProgress(message.progress || 0);
              if (message.message) {
                addLog(`📊 ${message.message}`);
              }
              break;

            case 'COMPLETE':
              if (message.status === 'SUCCESS') {
                addLog(`✅ 工作流执行成功, 总耗时 ${message.duration}ms`);
              } else {
                addLog(`❌ 工作流执行失败`);
              }
              setExecuting(false);
              break;

            case 'ERROR':
              addLog(`❌ 执行错误: ${message.error}`);
              setExecuting(false);
              break;
          }
        } catch (error) {
          console.error('解析 WebSocket 消息失败:', error);
        }
      };

      ws.onerror = (error) => {
        console.error('WebSocket 错误:', error);
      };

      ws.onclose = () => {
        console.log('WebSocket 连接关闭');
      };

      wsRef.current = ws;
    }

    return () => {
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [open]);

  // 自动滚动日志到底部
  useEffect(() => {
    if (logContainerRef.current) {
      logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
    }
  }, [logs]);

  // 添加日志
  const addLog = (message: string) => {
    const timestamp = new Date().toLocaleTimeString();
    setLogs((prev) => [...prev, `[${timestamp}] ${message}`]);
  };

  // 执行工作流
  const handleExecute = async () => {
    if (!inputData.trim()) {
      addLog('❌ 错误: 输入数据不能为空');
      return;
    }

    setExecuting(true);
    setExecutionResult(null);
    setNodeResults([]);
    setLogs([]);
    setProgress(0);

    try {
      const result = await onExecute(inputData);
      setExecutionResult(result);
    } catch (error) {
      addLog(`❌ 执行异常: ${error instanceof Error ? error.message : '未知错误'}`);
      setExecuting(false);
    }
  };

  // 渲染节点结果项
  const renderNodeResultItem = (nodeResult: NodeResult) => {
    let statusColor: 'success' | 'error' | 'processing' = 'processing';
    let statusIcon = <LoadingOutlined />;

    if (nodeResult.status === 'SUCCESS') {
      statusColor = 'success';
      statusIcon = <CheckCircleOutlined />;
    } else if (nodeResult.status === 'FAILED') {
      statusColor = 'error';
      statusIcon = <CloseCircleOutlined />;
    }

    return {
      key: nodeResult.nodeId,
      label: (
        <div className="flex items-center justify-between">
          <span>
            {statusIcon} {nodeResult.nodeName}
          </span>
          <Tag color={statusColor}>
            {nodeResult.status === 'RUNNING' ? '执行中' :
             nodeResult.status === 'SUCCESS' ? '成功' : '失败'}
            {nodeResult.duration && ` (${nodeResult.duration}ms)`}
          </Tag>
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
          {nodeResult.output && (
            <div>
              <div className="text-gray-600 text-xs mb-1">输出数据:</div>
              <pre className="bg-gray-50 p-2 rounded text-xs overflow-auto max-h-32">
                {JSON.stringify(nodeResult.output, null, 2)}
              </pre>
            </div>
          )}
          {nodeResult.error && (
            <Alert message="错误信息" description={nodeResult.error} type="error" showIcon />
          )}
        </div>
      ),
    };
  };

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
        {/* 输入区域 */}
        <div className="p-4 bg-gray-50 border-gray-200">
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

        {/* 执行状态 */}
        {(executing || nodeResults.length > 0) && (
          <div className="p-4 bg-gray-50 border-gray-200">
            <Card title="执行状态" size="small">
              {executing && (
                <div className="flex items-center gap-2 mb-2">
                  <LoadingOutlined className="text-blue-500" />
                  <span>执行中...</span>
                </div>
              )}
              {nodeResults.length > 0 && (
                <>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-gray-600 text-sm">
                      已完成节点: {nodeResults.filter(r => r.status !== 'RUNNING').length} / {nodeResults.length}
                    </span>
                  </div>
                  <Progress percent={progress} status={executing ? 'active' : 'success'} />
                </>
              )}
            </Card>
          </div>
        )}

        {/* 结果展示区 */}
        {nodeResults.length > 0 && (
          <div className="p-4 bg-gray-50 border-gray-200">
            <Card title="节点执行结果" size="small">
              <Collapse
                items={nodeResults.map(renderNodeResultItem)}
                defaultActiveKey={nodeResults.map(r => r.nodeId)}
              />
            </Card>
          </div>
        )}

        {/* 最终输出 */}
        {!executing && nodeResults.length > 0 && nodeResults[nodeResults.length - 1].output && (
          <div className="p-4 bg-gray-50 border-gray-200">
            <Card title="最终输出" size="small">
              {(() => {
                const lastOutput = nodeResults[nodeResults.length - 1].output!;
                let audioUrl: string | null = null;
                let fileName: string | undefined = undefined;

                if (typeof lastOutput === 'object' && lastOutput !== null) {
                  fileName = lastOutput.fileName as string | undefined;

                  if (lastOutput.audioUrl && typeof lastOutput.audioUrl === 'string') {
                    audioUrl = lastOutput.audioUrl;
                  }

                  if (!audioUrl && lastOutput.output && typeof lastOutput.output === 'string') {
                    const output = lastOutput.output as string;
                    if (output.includes('http://') || output.includes('https://')) {
                      audioUrl = output;
                    } else if (output.includes('<audio') && output.includes('src=')) {
                      const srcMatch = output.match(/src="([^"]+)"/);
                      if (srcMatch && srcMatch[1]) {
                        audioUrl = srcMatch[1];
                      }
                    }
                  }
                }

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
                    {JSON.stringify(lastOutput, null, 2)}
                  </pre>
                );
              })()}
            </Card>
          </div>
        )}

        {/* 日志区域 */}
        <div className="p-4 bg-gray-50 flex-1 flex flex-col">
          <Card title="执行日志" size="small" className="flex-1 flex flex-col">
            <div
              ref={logContainerRef}
              className="flex-1 overflow-auto min-h-0"
            >
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
            </div>
          </Card>
        </div>
      </div>
    </Drawer>
  );
};

export default DebugDrawer;
