package com.paiagent.engine.executor.impl;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.paiagent.engine.executor.NodeExecutor;
import com.paiagent.engine.model.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class TTSNodeExecutor implements NodeExecutor {
    
    private static final String AUDIO_STORAGE_DIR = "audio_output";
    
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
        
        String fileName = downloadAudioFile(audioUrl);
        String localAudioUrl = "/audio/" + fileName;
        
        Map<String, Object> output = new HashMap<>();
        output.put("audioUrl", localAudioUrl);
        output.put("fileName", fileName);
        output.put("output", localAudioUrl);
        
        log.info("TTS 音频生成成功 - 远程URL: {}, 本地文件: {}", audioUrl, fileName);
        
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
    
    private String downloadAudioFile(String audioUrl) throws Exception {
        String fileName = "audio_" + UUID.randomUUID() + ".wav";
        Path audioDir = Paths.get(AUDIO_STORAGE_DIR);
        
        if (!Files.exists(audioDir)) {
            Files.createDirectories(audioDir);
        }
        
        Path audioFile = audioDir.resolve(fileName);
        
        try (InputStream in = new URL(audioUrl).openStream();
             FileOutputStream out = new FileOutputStream(audioFile.toFile())) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        
        log.info("音频文件已下载到本地: {}", audioFile.toAbsolutePath());
        return fileName;
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