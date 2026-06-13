package com.panpeixue.myl.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.panpeixue.myl.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    private static final Set<String> IMAGE_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );
    private static final Set<String> VIDEO_TYPES = Set.of(
        "video/mp4", "video/webm", "video/ogg"
    );

    @Value("${app.upload-path:./uploads}")
    private String uploadPath;

    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        String category;
        if (IMAGE_TYPES.contains(contentType)) {
            category = "images";
        } else if (VIDEO_TYPES.contains(contentType)) {
            category = "videos";
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + contentType
                + ". Allowed: jpg, png, gif, webp, svg, mp4, webm, ogg");
        }

        /* generate unique filename: 20260113/uuid.ext */
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String ext = getExtension(Objects.requireNonNull(file.getOriginalFilename()));
        String filename = UUID.randomUUID() + ext;
        Path dir = Paths.get(uploadPath, category, dateDir);
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            String url = "/uploads/" + category + "/" + dateDir + "/" + filename;
            log.info("Uploaded: {} -> {} ({} bytes)", file.getOriginalFilename(), url, file.getSize());
            return Result.ok(Map.of("url", url, "size", file.getSize(), "type", contentType));
        } catch (IOException e) {
            log.error("Upload failed", e);
            throw new IllegalArgumentException("Upload failed: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot).toLowerCase();
    }
}