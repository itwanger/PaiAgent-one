package com.paiagent.engine.executor.impl;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.paiagent.engine.executor.NodeExecutor;
import com.paiagent.engine.model.WorkflowNode;
import com.paiagent.service.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class TTSNodeExecutor implements NodeExecutor {
    
    @Autowired
    private MinioService minioService;
    
    @Override
    public Map<String, Object> execute(WorkflowNode node, Map<String, Object> input) throws Exception {
        String text = extractInputText(node, input);
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("输入文本不能为空");
        }
        
        Map<String, Object> data = node.getData();
        String apiKey = (String) data.get("apiKey");
        String model = (String) data.getOrDefault("model", "qwen3-tts-flash");
        String voiceStr = (String) data.getOrDefault("voice", "Cherry");
        String languageType = (String) data.getOrDefault("languageType", "Auto");
        
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("阿里百炼 API Key 不能为空,请在节点配置中设置");
        }
        
        log.info("TTS 节点执行 - 模型: {}, 文本长度: {}, 音色: {}, 语言类型: {}", 
                model, text.length(), voiceStr, languageType);
        
        AudioParameters.Voice voice = convertVoice(voiceStr);
        
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .text(text)
                .voice(voice)
                .languageType(languageType)
                .build();
        
        MultiModalConversationResult result = conv.call(param);
        String audioUrl = result.getOutput().getAudio().getUrl();
        
        if (!StringUtils.hasText(audioUrl)) {
            throw new RuntimeException("阿里百炼 TTS 返回的音频URL为空");
        }
        
        log.info("阿里百炼 TTS 返回音频URL: {}", audioUrl);
        
        // 上传音频文件到 MinIO
        String fileName = "audio_" + UUID.randomUUID() + ".wav";
        String objectName = "audio/" + fileName;
        String minioUrl = minioService.uploadFromUrl(audioUrl, objectName, "audio/wav");
        
        Map<String, Object> output = new HashMap<>();
        output.put("audioUrl", minioUrl);
        output.put("fileName", fileName);
        output.put("output", minioUrl);
        
        log.info("TTS 音频已上传到 MinIO: {}", minioUrl);
        
        return output;
    }
    
    private AudioParameters.Voice convertVoice(String voiceStr) {
        try {
            return AudioParameters.Voice.valueOf(voiceStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("未知音色: {}, 使用默认音色 CHERRY", voiceStr);
            return AudioParameters.Voice.CHERRY;
        }
    }
    
    private String extractInputText(WorkflowNode node, Map<String, Object> input) {
        Map<String, Object> data = node.getData();
        List<Map<String, Object>> inputParams = (List<Map<String, Object>>) data.get("inputParams");
        
        if (inputParams != null && !inputParams.isEmpty()) {
            for (Map<String, Object> param : inputParams) {
                String paramName = (String) param.get("name");
                if ("text".equals(paramName)) {
                    String type = (String) param.get("type");
                    if ("input".equals(type)) {
                        return (String) param.get("value");
                    } else if ("reference".equals(type)) {
                        String referenceNode = (String) param.get("referenceNode");
                        if (StringUtils.hasText(referenceNode)) {
                            String[] parts = referenceNode.split("\\.");
                            if (parts.length == 2) {
                                String paramKey = parts[1];
                                Object value = input.get(paramKey);
                                if (value instanceof String) {
                                    return (String) value;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        String text = (String) input.get("output");
        if (StringUtils.hasText(text)) {
            return text;
        }
        
        text = (String) input.get("input");
        if (StringUtils.hasText(text)) {
            return text;
        }
        
        return (String) input.get("text");
    }
    
    @Override
    public String getSupportedNodeType() {
        return "tts";
    }
}