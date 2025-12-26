package com.paiagent.engine.executor.impl;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.paiagent.dto.ExecutionEvent;
import com.paiagent.engine.executor.NodeExecutor;
import com.paiagent.engine.model.WorkflowNode;
import com.paiagent.service.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Component
public class TTSNodeExecutor implements NodeExecutor {
    
    private static final int MAX_TTS_INPUT_LENGTH = 400;
    
    @Autowired
    private MinioService minioService;
    
    @Override
    public Map<String, Object> execute(WorkflowNode node, Map<String, Object> input) throws Exception {
        return execute(node, input, null);
    }
    
    @Override
    public Map<String, Object> execute(WorkflowNode node, Map<String, Object> input, Consumer<ExecutionEvent> progressCallback) throws Exception {
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
        
        List<String> textChunks = splitText(text, MAX_TTS_INPUT_LENGTH);
        log.info("文本分割为 {} 个片段", textChunks.size());
        
        if (progressCallback != null) {
            Map<String, Object> progressData = new HashMap<>();
            progressData.put("totalChunks", textChunks.size());
            progressData.put("currentChunk", 0);
            progressCallback.accept(ExecutionEvent.nodeProgress(
                node.getId(), 
                node.getType(), 
                "文本已分割为 " + textChunks.size() + " 个片段", 
                progressData
            ));
        }
        
        List<byte[]> audioChunks = new ArrayList<>();
        MultiModalConversation conv = new MultiModalConversation();
        
        for (int i = 0; i < textChunks.size(); i++) {
            String chunk = textChunks.get(i);
            int utf8ByteLength = chunk.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            log.info("处理第 {}/{} 个片段, 字符数: {}, UTF-8 字节数: {}", 
                    i + 1, textChunks.size(), chunk.length(), utf8ByteLength);
            
            if (progressCallback != null) {
                Map<String, Object> progressData = new HashMap<>();
                progressData.put("totalChunks", textChunks.size());
                progressData.put("currentChunk", i + 1);
                progressData.put("chunkText", chunk.substring(0, Math.min(50, chunk.length())) + "...");
                progressCallback.accept(ExecutionEvent.nodeProgress(
                    node.getId(), 
                    node.getType(), 
                    "正在处理第 " + (i + 1) + "/" + textChunks.size() + " 个片段", 
                    progressData
                ));
            }
            
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .text(chunk)
                    .voice(voice)
                    .languageType(languageType)
                    .build();
            
            MultiModalConversationResult result = conv.call(param);
            String audioUrl = result.getOutput().getAudio().getUrl();
            
            if (!StringUtils.hasText(audioUrl)) {
                throw new RuntimeException("阿里百炼 TTS 返回的音频URL为空 (片段 " + (i + 1) + ")");
            }
            
            log.info("第 {}/{} 个片段音频URL: {}", i + 1, textChunks.size(), audioUrl);
            
            byte[] audioData = downloadAudio(audioUrl);
            audioChunks.add(audioData);
            
            if (progressCallback != null) {
                Map<String, Object> progressData = new HashMap<>();
                progressData.put("totalChunks", textChunks.size());
                progressData.put("currentChunk", i + 1);
                progressData.put("completedChunks", i + 1);
                progressCallback.accept(ExecutionEvent.nodeProgress(
                    node.getId(), 
                    node.getType(), 
                    "已完成第 " + (i + 1) + "/" + textChunks.size() + " 个片段", 
                    progressData
                ));
            }
        }
        
        if (progressCallback != null) {
            progressCallback.accept(ExecutionEvent.nodeProgress(
                node.getId(), 
                node.getType(), 
                "正在合并 " + audioChunks.size() + " 个音频片段...", 
                null
            ));
        }
        
        byte[] mergedAudio = mergeWavFiles(audioChunks);
        
        String fileName = "audio_" + UUID.randomUUID() + ".wav";
        String objectName = "audio/" + fileName;
        String minioUrl = minioService.uploadFromBytes(mergedAudio, objectName, "audio/wav");
        
        Map<String, Object> output = new HashMap<>();
        output.put("audioUrl", minioUrl);
        output.put("fileName", fileName);
        output.put("output", minioUrl);
        output.put("chunks", textChunks.size());
        
        log.info("TTS 合并音频已上传到 MinIO: {}, 共 {} 个片段", minioUrl, textChunks.size());
        
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
    
    private List<String> splitText(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        
        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            
            while (end > start) {
                String candidate = text.substring(start, end);
                int byteLength = candidate.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                
                if (byteLength <= 600) {
                    if (end < text.length()) {
                        int lastPunctuation = findLastPunctuation(text, start, end);
                        if (lastPunctuation > start) {
                            end = lastPunctuation + 1;
                            candidate = text.substring(start, end);
                        }
                    }
                    
                    chunks.add(candidate);
                    start = end;
                    break;
                }
                
                end -= 10;
            }
            
            if (end <= start) {
                end = start + 1;
                while (end <= text.length()) {
                    String candidate = text.substring(start, end);
                    int byteLength = candidate.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                    if (byteLength > 600) {
                        if (end - 1 > start) {
                            chunks.add(text.substring(start, end - 1));
                            start = end - 1;
                        } else {
                            throw new IllegalArgumentException("单个字符超过 600 字节,无法处理");
                        }
                        break;
                    }
                    end++;
                }
            }
        }
        
        return chunks;
    }
    
    private int findLastPunctuation(String text, int start, int end) {
        String punctuations = "。！？；,.!?;";
        for (int i = end - 1; i >= start; i--) {
            if (punctuations.indexOf(text.charAt(i)) >= 0) {
                return i;
            }
        }
        return -1;
    }
    
    private byte[] downloadAudio(String audioUrl) throws Exception {
        URL url = new URL(audioUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        
        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        } finally {
            conn.disconnect();
        }
    }
    
    private byte[] mergeWavFiles(List<byte[]> audioChunks) throws Exception {
        if (audioChunks.isEmpty()) {
            throw new IllegalArgumentException("音频片段列表为空");
        }
        
        if (audioChunks.size() == 1) {
            return audioChunks.get(0);
        }
        
        byte[] firstChunk = audioChunks.get(0);
        if (firstChunk.length < 44) {
            throw new IllegalArgumentException("无效的 WAV 文件格式");
        }
        
        ByteArrayOutputStream mergedStream = new ByteArrayOutputStream();
        
        byte[] header = Arrays.copyOf(firstChunk, 44);
        mergedStream.write(header);
        
        for (byte[] chunk : audioChunks) {
            if (chunk.length > 44) {
                mergedStream.write(chunk, 44, chunk.length - 44);
            }
        }
        
        byte[] mergedData = mergedStream.toByteArray();
        
        int dataSize = mergedData.length - 44;
        int fileSize = mergedData.length - 8;
        
        mergedData[4] = (byte) (fileSize & 0xFF);
        mergedData[5] = (byte) ((fileSize >> 8) & 0xFF);
        mergedData[6] = (byte) ((fileSize >> 16) & 0xFF);
        mergedData[7] = (byte) ((fileSize >> 24) & 0xFF);
        
        mergedData[40] = (byte) (dataSize & 0xFF);
        mergedData[41] = (byte) ((dataSize >> 8) & 0xFF);
        mergedData[42] = (byte) ((dataSize >> 16) & 0xFF);
        mergedData[43] = (byte) ((dataSize >> 24) & 0xFF);
        
        return mergedData;
    }
}