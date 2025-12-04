import { useState } from 'react';
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
  status: 'SUCCESS' | 'FAILED';
  input: Record<string, unknown>;
  output: Record<string, unknown>;
  duration: number;
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
    setLogs([]);
    addLog('🚀 开始执行工作流...');

    try {
      const result = await onExecute(inputData);
      setExecutionResult(result);

      if (result.status === 'SUCCESS') {
        addLog(`✅ 工作流执行成功,耗时 ${result.duration}ms`);
        result.nodeResults.forEach((nodeResult) => {
          addLog(
            `📊 节点 [${nodeResult.nodeName}] 执行${nodeResult.status === 'SUCCESS' ? '成功' : '失败'},耗时 ${nodeResult.duration}ms`
          );
        });
      } else {
        addLog(`❌ 工作流执行失败: ${result.errorMessage || '未知错误'}`);
      }
    } catch (error) {
      addLog(`❌ 执行异常: ${error instanceof Error ? error.message : '未知错误'}`);
    } finally {
      setExecuting(false);
    }
  };

  // 计算执行进度
  const getProgress = () => {
    if (!executionResult) return 0;
    const total = executionResult.nodeResults.length;
    if (total === 0) return 0;
    const completed = executionResult.nodeResults.filter((r) => r.status === 'SUCCESS').length;
    return Math.round((completed / total) * 100);
  };

  // 渲染节点结果项
  const renderNodeResultItem = (nodeResult: NodeResult) => {
    const statusColor = nodeResult.status === 'SUCCESS' ? 'success' : 'error';
    const statusIcon = nodeResult.status === 'SUCCESS' ? <CheckCircleOutlined /> : <CloseCircleOutlined />;

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

        {/* 执行状态 */}
        {(executing || executionResult) && (
          <div className="p-4 border-b border-gray-200">
            <Card title="执行状态" size="small">
              {executing && (
                <div className="flex items-center gap-2">
                  <LoadingOutlined className="text-blue-500" />
                  <span>执行中...</span>
                </div>
              )}
              {executionResult && (
                <>
                  <div className="flex items-center justify-between mb-2">
                    <span>
                      状态:{' '}
                      <Tag color={executionResult.status === 'SUCCESS' ? 'success' : 'error'}>
                        {executionResult.status === 'SUCCESS' ? '成功' : '失败'}
                      </Tag>
                    </span>
                    <span className="text-gray-600 text-sm">耗时: {executionResult.duration}ms</span>
                  </div>
                  <Progress percent={getProgress()} status={executionResult.status === 'SUCCESS' ? 'success' : 'exception'} />
                  <div className="mt-2 text-sm text-gray-600">
                    已完成节点: {executionResult.nodeResults.filter((r) => r.status === 'SUCCESS').length} / {executionResult.nodeResults.length}
                  </div>
                </>
              )}
            </Card>
          </div>
        )}

        {/* 结果展示区 */}
        {executionResult && executionResult.nodeResults.length > 0 && (
          <div className="p-4 border-b border-gray-200">
            <Card title="节点执行结果" size="small">
              <Collapse
                items={executionResult.nodeResults.map(renderNodeResultItem)}
                defaultActiveKey={executionResult.nodeResults.map((r) => r.nodeId)}
              />
            </Card>
          </div>
        )}

        {/* 最终输出 */}
        {executionResult && executionResult.status === 'SUCCESS' && (
          <div className="p-4 border-b border-gray-200">
            <Card title="最终输出" size="small">
              {/* 如果输出包含音频URL或output字段指向音频,显示音频播放器 */}
              {(() => {
                let audioUrl: string | null = null;
                let fileName: string | undefined = undefined;
                
                // 解析 outputData (可能是字符串)
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
                  
                  // 先检查 audioUrl 字段
                  if (outputData.audioUrl && typeof outputData.audioUrl === 'string') {
                    audioUrl = outputData.audioUrl;
                  }
                  
                  // 检查 output 字段
                  if (!audioUrl && outputData.output && typeof outputData.output === 'string') {
                    const output = outputData.output;
                    // 检查是否是 MinIO URL 或包含 <audio> 标签
                    if (output.includes('http://') || output.includes('https://')) {
                      // 直接是 URL
                      audioUrl = output;
                    } else if (output.includes('<audio') && output.includes('src=')) {
                      // 提取 src 属性中的 URL
                      const srcMatch = output.match(/src="([^"]+)"/);
                      if (srcMatch && srcMatch[1]) {
                        audioUrl = srcMatch[1];
                      }
                    } else if (output.startsWith('/audio/')) {
                      // 相对路径
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
                
                // 如果不是音频,显示原始输出数据
                return (
                  <pre className="bg-gray-50 p-2 rounded text-xs overflow-auto max-h-48">
                    {JSON.stringify(executionResult.outputData, null, 2)}
                  </pre>
                );
              })()}
            </Card>
          </div>
        )}

        {/* 日志区域 */}
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