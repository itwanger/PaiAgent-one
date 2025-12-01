# AI Ping 节点配置说明

## 🎯 配置参数说明

AI Ping 节点现在支持真实的 API 调用，包含以下配置参数：

### 1. API 地址 (apiUrl)
- **默认**: `https://api.aiping.com/v1`
- **说明**: AI Ping 服务的 API 基础地址
- **完整请求地址**: `{apiUrl}/chat/completions`

### 2. API 密钥 (apiKey)
- **说明**: 访问 AI Ping 服务的认证密钥
- **格式**: Bearer Token 格式
- **示例**: `sk-aiping-xxxxxxxxxxxx`

### 3. 模型名称 (model)
- **默认**: `ai-ping-v1`
- **说明**: 使用的 AI Ping 模型版本
- **示例**: `ai-ping-pro`, `ai-ping-v1`

### 4. 温度 (temperature)
- **默认**: `0.7`
- **范围**: `0-2`
- **说明**: 控制输出随机性，值越高越随机

### 5. 提示词模板 (prompt)
- **说明**: 与 AI Ping 交互的提示词
- **支持参数**: 使用 `{{参数名}}` 引用输入参数
- **示例**:
  ```
  你是一个专业的 AI 助手，请回答以下问题：{{input}}
  ```

### 6. 输入参数配置
- **类型**:
  - `input`: 直接输入值
  - `reference`: 引用其他节点输出
- **示例**: 添加名为 `input` 的参数，值为 `用户输入`

### 7. 输出参数配置
- **默认输出**: `output` 和 `tokens`
- **可自定义**: 添加更多输出参数名

## 🔧 API 请求格式

AI Ping 使用 OpenAI 兼容的 API 格式：

```json
{
  "model": "ai-ping-v1",
  "temperature": 0.7,
  "messages": [
    {
      "role": "user",
      "content": "最终提示词内容"
    }
  ]
}
```

## 📝 API 响应格式

AI Ping 返回类似 OpenAI 的响应格式：

```json
{
  "choices": [
    {
      "message": {
        "content": "AI Ping 的回复内容"
      }
    }
  ],
  "usage": {
    "total_tokens": 123
  }
}
```

## 🎛️ 使用步骤

### 1. 基本配置
```
API 地址: https://api.aiping.com/v1
API 密钥: 您的实际 API Key
模型名称: ai-ping-v1
温度: 0.7
```

### 2. 提示词设置
```
你是一个专业的 AI 助手，请用简洁明了的语言回答：
{{input}}
```

### 3. 工作流连接
```
输入节点 → AI Ping 节点 → 输出节点
```

## 🚨 注意事项

1. **API 密钥安全**: 请勿在公共代码中暴露 API 密钥
2. **网络连通性**: 确保后端服务器能访问 AI Ping API
3. **错误处理**: API 调用失败时会返回错误信息
4. **限流**: 注意 AI Ping API 的调用频率限制

## 🔍 调试

### 查看日志
后端日志会输出：
- API 请求地址和请求体
- API 响应状态码和内容
- 参数映射结果
- 最终输出结果

### 常见问题
- **API 调用失败**: 检查 API 地址和密钥
- **参数替换失败**: 确认提示词中的参数名与输入参数名一致
- **响应格式异常**: AI Ping 可能使用不同的响应格式，已添加兼容处理

## 📊 示例输出

成功调用时的输出：
```json
{
  "output": "AI Ping 的实际回复内容...",
  "tokens": 125
}
```

API 错误时的输出：
```json
{
  "output": "调用 API 失败: Connection timeout",
  "tokens": 0
}
```
