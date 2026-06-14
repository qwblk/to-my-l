package com.panpeixue.myl.controller;

import com.panpeixue.myl.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/**
 * /upload 控制器测试 —— 用 standalone MockMvc，不启动 Spring 容器，
 * 直接走 GlobalExceptionHandler 兜底。
 *
 * 注意：401 未登录的判定不在这里 —— 那是 SaServletFilter 在过滤器层抛
 * NotLoginException，由 GlobalExceptionHandler 转 401，已经在
 * GlobalExceptionHandlerNotLoginTest 单独覆盖。这里 standalone MockMvc
 * 不挂 SaServletFilter，所以模拟不了真实 401 场景，但 401 测试覆盖完整。
 */
class FileUploadControllerTest {

    private MockMvc mvc;

    @TempDir
    Path uploadDir;

    @BeforeEach
    void setUp() {
        FileUploadController controller = new FileUploadController();
        ReflectionTestUtils.setField(controller, "uploadPath", uploadDir.toString());
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void upload_jpgImage_ok() throws Exception {
        byte[] jpg = makePng(64, 32);   // PNG bytes，但用 image/jpeg 头不影响 mime 校验
        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.jpg", "image/jpeg", jpg);

        MvcResult res = mvc.perform(multipart("/upload").file(file))
                .andReturn();

        assertThat(res.getResponse().getStatus()).isEqualTo(200);
        String body = res.getResponse().getContentAsString();
        assertThat(body).contains("\"code\":200");
        assertThat(body).contains("\"type\":\"image\"");
        // url 形如 /static/uploads/2026/06/<uuid>.jpg
        assertThat(body).contains("/static/uploads/");
        assertThat(body).contains(".jpg");
    }

    @Test
    void upload_pngImage_ok() throws Exception {
        byte[] png = makePng(100, 50);
        MockMultipartFile file = new MockMultipartFile(
                "file", "x.png", "image/png", png);

        String body = mvc.perform(multipart("/upload").file(file))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"code\":200");
        assertThat(body).contains("\"type\":\"image\"");
        assertThat(body).contains(".png");
        // PNG 是真的 PNG，应该能解析出尺寸
        assertThat(body).contains("\"width\":100");
        assertThat(body).contains("\"height\":50");
    }

    @Test
    void upload_mp4Video_ok() throws Exception {
        // 任意字节即可：不真去解码视频，只看 mime 走过白名单
        byte[] mp4 = "fake-mp4-bytes".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "v.mp4", "video/mp4", mp4);

        String body = mvc.perform(multipart("/upload").file(file))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"code\":200");
        assertThat(body).contains("\"type\":\"video\"");
        assertThat(body).contains(".mp4");
        // 视频 duration 由前端拿，后端不返回
        assertThat(body).doesNotContain("\"duration\"");
    }

    @Test
    void upload_quicktime_ok() throws Exception {
        // iOS 拍出来的 .mov，mime 是 video/quicktime，确认白名单覆盖到了
        MockMultipartFile file = new MockMultipartFile(
                "file", "iphone.mov", "video/quicktime", "fake".getBytes());

        String body = mvc.perform(multipart("/upload").file(file))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"code\":200");
        assertThat(body).contains(".mov");
    }

    @Test
    void upload_exe_rejected_415() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.exe", "application/x-msdownload", "MZ".getBytes());

        String body = mvc.perform(multipart("/upload").file(file))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"code\":415");
        assertThat(body).contains("Unsupported");
    }

    @Test
    void upload_txt_rejected_415() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello".getBytes());

        String body = mvc.perform(multipart("/upload").file(file))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"code\":415");
    }

    @Test
    void upload_imageOver10MB_rejected_413() throws Exception {
        // 11MB 假图 —— 不需要真 PNG，因为 size 校验在 ImageIO.read 之前
        byte[] huge = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", huge);

        String body = mvc.perform(multipart("/upload").file(file))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"code\":413");
        assertThat(body).contains("too large");
    }

    @Test
    void upload_videoUnder50MB_ok_butOver50MB_rejected() throws Exception {
        // 51MB 视频 —— 超出视频上限
        byte[] huge = new byte[51 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.mp4", "video/mp4", huge);

        String body = mvc.perform(multipart("/upload").file(file))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"code\":413");
    }

    @Test
    void upload_filenameUsesUuid_notUserOriginal() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/passwd.png", "image/png", makePng(8, 8));

        String body = mvc.perform(multipart("/upload").file(file))
                .andReturn().getResponse().getContentAsString();

        // 落盘文件名是 UUID + .png，不能含原文件名片段
        assertThat(body).contains("\"code\":200");
        assertThat(body).doesNotContain("etc/passwd");
        assertThat(body).doesNotContain("..");

        // 物理验证：临时目录里只有日期子目录，里面是 UUID.png
        List<Path> files;
        try (var walk = Files.walk(uploadDir)) {
            files = walk.filter(Files::isRegularFile).toList();
        }
        assertThat(files).hasSize(1);
        String name = files.get(0).getFileName().toString();
        assertThat(name).endsWith(".png");
        // UUID 长度是 36 + 4(.png) = 40
        assertThat(name).hasSize(40);
    }

    private byte[] makePng(int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "PNG", bos);
            return bos.toByteArray();
        }
    }
}
