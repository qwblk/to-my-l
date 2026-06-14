package com.panpeixue.myl.controller;

import com.panpeixue.myl.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * POST /upload —— 图片/视频上传，登录用户。
 *
 * 业务约束（题面规格）：
 *   - 图片 ≤ 10MB；视频 ≤ 50MB（application.yml 的 multipart.max-file-size = 50MB 是硬上限）
 *   - 接受 mime: image/jpeg|png|webp|gif, video/mp4, video/quicktime
 *   - 存到 uploads/YYYY/MM/<uuid><ext>
 *   - URL 形如 /static/uploads/2026/06/xxx.jpg（不是 /uploads/...）
 *
 * 错误码沿用项目约定（HTTP 200 + Result.code）：
 *   - 401 未登录（SaServletFilter 抛 NotLoginException → GlobalExceptionHandler）
 *   - 413 文件大小超限
 *   - 415 类型不在白名单
 *   - 400 文件为空 / 其他参数错误
 *
 * 不做：
 *   - duration（前端用 <video>.loadedmetadata 拿，准且零依赖）
 *   - EXIF 清洗 / 病毒扫描 / 压缩 / 缩略图
 */
@RestController
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    /** 图片白名单 → 扩展名映射。映射保证扩展名是「我们认得的」而不是用户传上来的，防 .jpg.exe */
    private static final Map<String, String> IMAGE_EXT = Map.of(
        "image/jpeg", ".jpg",
        "image/png",  ".png",
        "image/webp", ".webp",
        "image/gif",  ".gif"
    );

    /** 视频白名单 → 扩展名映射。iOS 拍出来是 video/quicktime → .mov */
    private static final Map<String, String> VIDEO_EXT = Map.of(
        "video/mp4",       ".mp4",
        "video/quicktime", ".mov"
    );

    private static final long IMAGE_MAX = 10L * 1024 * 1024;   // 10MB
    private static final long VIDEO_MAX = 50L * 1024 * 1024;   // 50MB

    /** uploads/2026/06 这种 */
    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM");

    /** 前端拼出来的 URL 前缀 */
    private static final String URL_PREFIX = "/static/uploads/";

    @Value("${app.upload-path:./uploads}")
    private String uploadPath;

    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "File is empty");
        }

        String mime = file.getContentType();
        boolean isImage = IMAGE_EXT.containsKey(mime);
        boolean isVideo = VIDEO_EXT.containsKey(mime);

        if (!isImage && !isVideo) {
            return Result.error(415, "Unsupported file type: " + mime
                + ". Allowed: " + Set.of(
                    "image/jpeg", "image/png", "image/webp", "image/gif",
                    "video/mp4", "video/quicktime"));
        }

        long size = file.getSize();
        long maxAllowed = isImage ? IMAGE_MAX : VIDEO_MAX;
        if (size > maxAllowed) {
            return Result.error(413, (isImage ? "Image" : "Video")
                + " too large: " + size + " bytes, limit " + maxAllowed);
        }

        // 文件名：UUID + 我们认得的扩展名（不复用用户原扩展名 → 杜绝路径穿越/双扩展名攻击）
        String ext = isImage ? IMAGE_EXT.get(mime) : VIDEO_EXT.get(mime);
        String filename = UUID.randomUUID() + ext;

        // 解析目标目录：uploads/YYYY/MM/，加 normalize 防穿越
        String dateDir = LocalDate.now().format(DATE_DIR);
        Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path targetDir = baseDir.resolve(dateDir).normalize();
        if (!targetDir.startsWith(baseDir)) {
            // 理论上 dateDir 是我们 format 出来的不会越界，这里是兜底
            return Result.error(400, "Invalid path");
        }
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            log.error("Cannot create upload directory: {}", targetDir, e);
            return Result.error(500, "Upload failed: cannot create directory");
        }

        Path target = targetDir.resolve(filename);
        try {
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("Failed to write upload file: {}", target, e);
            return Result.error(500, "Upload failed: " + e.getMessage());
        }

        // 拼响应 —— 用 LinkedHashMap 保证字段顺序稳定，方便日志/抓包对照
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", URL_PREFIX + dateDir + "/" + filename);
        data.put("type", isImage ? "image" : "video");

        if (isImage) {
            // ImageIO 失败不算致命错误：图片已存盘可用，前端拿不到 width/height 也能用 CSS 兜底
            try {
                int[] wh = readImageDimension(target);
                if (wh != null) {
                    data.put("width", wh[0]);
                    data.put("height", wh[1]);
                }
            } catch (Exception e) {
                log.warn("Cannot read image dimension for {}: {}", target, e.getMessage());
            }
        }
        // 视频 duration 由前端 <video>.loadedmetadata 取，后端不返回（字段缺省即 null）

        log.info("Uploaded: {} -> {} ({} bytes)", file.getOriginalFilename(), data.get("url"), size);
        return Result.ok(data);
    }

    /** ImageIO.read 对损坏文件返回 null，调用方自行容错 */
    private int[] readImageDimension(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) return null;
            return new int[]{img.getWidth(), img.getHeight()};
        }
    }
}
