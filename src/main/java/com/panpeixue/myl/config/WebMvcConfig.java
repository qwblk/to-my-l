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
        /* 把 uploads/ 目录挂在两个 URL 路径下：
         *   /static/uploads/**  —— 当前规范（前端拼这个）
         *   /uploads/**         —— 历史 URL，老 moment.image 字段里存的 /uploads/...
         *                          如果前端老数据要回放，这条 mapping 必须保留
         */
        String absolutePath = new java.io.File(uploadPath).getAbsolutePath()
            .replace("\\", "/");
        if (!absolutePath.endsWith("/")) absolutePath += "/";
        String location = "file:" + absolutePath;

        registry.addResourceHandler("/static/uploads/**").addResourceLocations(location);
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}