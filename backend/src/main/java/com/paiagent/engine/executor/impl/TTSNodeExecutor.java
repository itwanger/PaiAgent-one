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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TTSNodeExecutor implements NodeExecutor {

    private static final int MAX_TEXT_LENGTH = 300;

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

        // Check if text exceeds maximum length
        if (text.length() > MAX_TEXT_LENGTH) {
            log.warn("文本长度 {} 超过限制 {}, 将分片处理", text.length(), MAX_TEXT_LENGTH);
            return processLongText(text, apiKey, model, voice, languageType);
        }

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

        // Upload audio file to MinIO
        String fileName = "audio_" + UUID.randomUUID() + ".wav";
        String objectName = "audio/" + fileName;
        String minioUrl = minioService.uploadFromUrl(audioUrl, objectName, "audio/wav");

        Map<String, Object> output = new HashMap<>();
        output.put("audioUrl", minioUrl);
        output.put("fileName", fileName);
        output.put("output", minioUrl);
        output.put("chunkCount", 1);

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

    private Map<String, Object> processLongText(String text, String apiKey, String model,
                                                 AudioParameters.Voice voice, String languageType) throws Exception {
        List<String> chunks = splitText(text);
        log.info("文本已分割为 {} 个片段", chunks.size());

        List<String> audioUrls = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            log.info("处理第 {}/{} 个片段, 长度: {}", i + 1, chunks.size(), chunk.length());

            // Validate chunk length before sending to API
            if (chunk.length() > MAX_TEXT_LENGTH) {
                log.error("片段 {} 长度 {} 超过限制 {}, 将截断", i + 1, chunk.length(), MAX_TEXT_LENGTH);
                chunk = chunk.substring(0, MAX_TEXT_LENGTH);
            }

            MultiModalConversation conv = new MultiModalConversation();
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

            // Upload to MinIO
            String fileName = "audio_" + UUID.randomUUID() + "_part" + (i + 1) + ".wav";
            String objectName = "audio/" + fileName;
            String minioUrl = minioService.uploadFromUrl(audioUrl, objectName, "audio/wav");

            audioUrls.add(minioUrl);
            fileNames.add(fileName);

            log.info("片段 {}/{} 已上传到 MinIO: {}", i + 1, chunks.size(), minioUrl);
        }

        Map<String, Object> output = new HashMap<>();
        output.put("audioUrls", audioUrls);
        output.put("fileNames", fileNames);
        output.put("chunkCount", chunks.size());

        // For backward compatibility, set the first audio URL as the main output
        if (!audioUrls.isEmpty()) {
            output.put("audioUrl", audioUrls.get(0));
            output.put("fileName", fileNames.get(0));
            output.put("output", audioUrls.get(0));
        }

        log.info("所有 TTS 音频片段已处理完成, 共 {} 个", chunks.size());
        return output;
    }

    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_TEXT_LENGTH, text.length());

            // If we're not at the end and the chunk doesn't end at a sentence boundary
            if (end < text.length()) {
                // Look for sentence boundary near the end (search backwards)
                int lastBoundary = -1;
                for (int i = end - 1; i >= start + MAX_TEXT_LENGTH / 2; i--) {
                    char c = text.charAt(i);
                    if (c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?') {
                        lastBoundary = i + 1;
                        break;
                    }
                }

                if (lastBoundary > start) {
                    end = lastBoundary;
                } else {
                    // No sentence boundary found, look for comma or space
                    int lastComma = text.lastIndexOf(',', end);
                    int lastSpace = text.lastIndexOf(' ', end);
                    int lastBreak = Math.max(lastComma, lastSpace);

                    if (lastBreak > start + MAX_TEXT_LENGTH / 2) {
                        end = lastBreak + 1;
                    } else if (end > start) {
                        // Force split at MAX_TEXT_LENGTH as last resort
                        end = Math.min(start + MAX_TEXT_LENGTH, text.length());
                    }
                }
            }

            String chunk = text.substring(start, end).trim();
            // Safety check: ensure chunk doesn't exceed limit
            if (chunk.length() > MAX_TEXT_LENGTH) {
                chunk = chunk.substring(0, MAX_TEXT_LENGTH);
                log.warn("强制截断超长片段到 {} 字符", MAX_TEXT_LENGTH);
            }
            if (chunk.length() > 0) {
                chunks.add(chunk);
                log.info("分割片段: 长度={}, 预览={}", chunk.length(),
                        chunk.substring(0, Math.min(50, chunk.length())) + "...");
            }
            start = end;
        }

        return chunks;
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
                                    return extractContentFromString((String) value);
                                }
                            }
                        }
                    }
                }
            }
        }

        Object textObj = input.get("output");
        if (textObj != null) {
            return extractContentFromString(String.valueOf(textObj));
        }

        textObj = input.get("input");
        if (textObj != null) {
            return extractContentFromString(String.valueOf(textObj));
        }

        textObj = input.get("text");
        if (textObj != null) {
            return extractContentFromString(String.valueOf(textObj));
        }

        return "";
    }

    /**
     * Extract content from a string, handling ChatMessage toString() format
     * Example: ChatMessage(role=assistant, content=xxx, reasoningContent=xxx, ...)
     */
    private String extractContentFromString(String text) {
        if (text == null) {
            return "";
        }

        // Check if the text looks like a ChatMessage toString() format
        if (text.startsWith("ChatMessage(")) {
            // Extract content field from ChatMessage toString() format
            // Pattern: content=xxx, (extract until next comma or closing paren)
            int contentIndex = text.indexOf("content=");
            if (contentIndex != -1) {
                int startIndex = contentIndex + 8; // "content=".length()
                int endIndex = text.indexOf(',', startIndex);
                if (endIndex == -1) {
                    endIndex = text.length() - 1; // Remove trailing ')'
                }
                String content = text.substring(startIndex, endIndex).trim();
                log.info("从 ChatMessage 对象提取内容: {}", content);
                return content;
            }
        }

        return text;
    }
    
    @Override
    public String getSupportedNodeType() {
        return "tts";
    }
}