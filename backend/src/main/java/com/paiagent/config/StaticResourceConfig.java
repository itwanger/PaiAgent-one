package com.paiagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * 静态资源配置
 * 用于提供音频文件的访问
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置音频文件访问路径 - 使用绝对路径
        File audioDir = new File("audio_output");
        String absolutePath = audioDir.getAbsolutePath() + File.separator;
        
        registry.addResourceHandler("/audio/**")
                .addResourceLocations("file:" + absolutePath);
    }
}