package com.panpeixue.myl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload-path:./uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /* map /uploads/** to local disk folder */
        String absolutePath = new java.io.File(uploadPath).getAbsolutePath()
            .replace("\\", "/");
        if (!absolutePath.endsWith("/")) absolutePath += "/";
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + absolutePath);
    }
}