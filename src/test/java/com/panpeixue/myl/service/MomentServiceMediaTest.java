package com.panpeixue.myl.service;

import com.panpeixue.myl.mapper.MomentMapper;
import com.panpeixue.myl.model.dto.MomentMedia;
import com.panpeixue.myl.model.pojo.Moment;
import com.panpeixue.myl.service.impl.MomentServiceImpl;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * MomentService 在多媒体改造后的关键业务规则：
 *   - content 不能为空
 *   - mediaList 每项 url 必须以 /static/uploads/ 开头（堵外站 URL）
 *   - mediaList ≤ 9
 *   - type 只接受 image / video
 *   - listAll 老数据（image 有值、media_list null）→ mediaList 兼容包成单元素
 *   - listAll 新数据（media_list JSON）→ 解析回 List<MomentMedia>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MomentServiceMediaTest {

    @Mock
    MomentMapper momentMapper;

    @Mock
    WebSocketSessionManager sessionManager;

    @InjectMocks
    MomentServiceImpl service;

    @Test
    void create_emptyContent_rejected() {
        assertThatThrownBy(() -> service.create(1L, "", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Content");
        assertThatThrownBy(() -> service.create(1L, "   ", null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(1L, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_externalUrl_rejected() {
        List<MomentMedia> bad = List.of(
            new MomentMedia("image", "http://evil.com/x.jpg", null, null, null));

        assertThatThrownBy(() -> service.create(1L, "hello", bad))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("/static/uploads/");
    }

    @Test
    void create_protocolRelativeUrl_rejected() {
        // //evil.com/x.jpg 也算外站
        List<MomentMedia> bad = List.of(
            new MomentMedia("image", "//evil.com/x.jpg", null, null, null));

        assertThatThrownBy(() -> service.create(1L, "hello", bad))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_unknownType_rejected() {
        List<MomentMedia> bad = List.of(
            new MomentMedia("audio", "/static/uploads/2026/06/a.mp3", null, null, null));

        assertThatThrownBy(() -> service.create(1L, "hello", bad))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("image or video");
    }

    @Test
    void create_tooManyMedia_rejected() {
        // 10 个超限
        List<MomentMedia> tooMany = java.util.stream.IntStream.range(0, 10)
            .mapToObj(i -> new MomentMedia("image", "/static/uploads/2026/06/" + i + ".jpg", null, null, null))
            .toList();

        assertThatThrownBy(() -> service.create(1L, "hello", tooMany))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("9");
    }

    @Test
    void create_validMediaList_persistsAsJson() {
        List<MomentMedia> mediaList = List.of(
            new MomentMedia("image", "/static/uploads/2026/06/a.jpg", 1024, 768, null),
            new MomentMedia("video", "/static/uploads/2026/06/b.mp4", null, null, null));
        when(momentMapper.insert(any(Moment.class))).thenReturn(1);

        Moment created = service.create(1L, "hello world", mediaList);

        // image 字段不再写
        assertThat(created.getImage()).isNull();
        // mediaListJson 是序列化后的 JSON
        assertThat(created.getMediaListJson())
            .startsWith("[")
            .contains("\"type\":\"image\"")
            .contains("\"url\":\"/static/uploads/2026/06/a.jpg\"")
            .contains("\"width\":1024")
            .contains("\"type\":\"video\"")
            // duration 是 null，配合 @JsonInclude.NON_NULL 应该不出现
            .doesNotContain("\"duration\"");
        // 给前端的对象也回填了 mediaList
        assertThat(created.getMediaList()).hasSize(2);
    }

    @Test
    void create_nullMediaList_writesNullToDb() {
        when(momentMapper.insert(any(Moment.class))).thenReturn(1);

        Moment created = service.create(1L, "just text", null);

        assertThat(created.getMediaListJson()).isNull();
        assertThat(created.getMediaList()).isEmpty();
    }

    @Test
    void listAll_legacyData_imageWrappedAsMediaList() {
        Moment legacy = new Moment();
        legacy.setId(1L);
        legacy.setUserId(1L);
        legacy.setContent("old");
        legacy.setImage("/uploads/legacy.jpg");
        legacy.setMediaListJson(null);

        when(momentMapper.selectAll()).thenReturn(List.of(legacy));
        lenient().when(momentMapper.selectLikesByMomentId(any())).thenReturn(List.of());
        lenient().when(momentMapper.selectCommentsByMomentId(any())).thenReturn(List.of());
        lenient().when(momentMapper.countLikes(any())).thenReturn(0);

        List<Moment> list = service.listAll();

        assertThat(list).hasSize(1);
        List<MomentMedia> media = list.get(0).getMediaList();
        assertThat(media).hasSize(1);
        assertThat(media.get(0).getType()).isEqualTo("image");
        assertThat(media.get(0).getUrl()).isEqualTo("/uploads/legacy.jpg");
        // image 字段保留，前端可以选择忽略
        assertThat(list.get(0).getImage()).isEqualTo("/uploads/legacy.jpg");
    }

    @Test
    void listAll_newData_parsesMediaListJson() {
        Moment fresh = new Moment();
        fresh.setId(2L);
        fresh.setUserId(1L);
        fresh.setContent("new");
        fresh.setImage(null);
        fresh.setMediaListJson(
            "[{\"type\":\"image\",\"url\":\"/static/uploads/2026/06/a.jpg\",\"width\":800,\"height\":600},"
            + "{\"type\":\"video\",\"url\":\"/static/uploads/2026/06/b.mp4\"}]");

        when(momentMapper.selectAll()).thenReturn(List.of(fresh));
        lenient().when(momentMapper.selectLikesByMomentId(any())).thenReturn(List.of());
        lenient().when(momentMapper.selectCommentsByMomentId(any())).thenReturn(List.of());
        lenient().when(momentMapper.countLikes(any())).thenReturn(0);

        List<MomentMedia> media = service.listAll().get(0).getMediaList();

        assertThat(media).hasSize(2);
        assertThat(media.get(0).getType()).isEqualTo("image");
        assertThat(media.get(0).getUrl()).isEqualTo("/static/uploads/2026/06/a.jpg");
        assertThat(media.get(0).getWidth()).isEqualTo(800);
        assertThat(media.get(1).getType()).isEqualTo("video");
        assertThat(media.get(1).getUrl()).isEqualTo("/static/uploads/2026/06/b.mp4");
    }

    @Test
    void listAll_corruptJson_fallbackToImage() {
        // DB 里 media_list 不是合法 JSON 时降级，不抛错
        Moment broken = new Moment();
        broken.setId(3L);
        broken.setUserId(1L);
        broken.setContent("bad");
        broken.setImage("/uploads/fallback.jpg");
        broken.setMediaListJson("{not json");

        when(momentMapper.selectAll()).thenReturn(List.of(broken));
        lenient().when(momentMapper.selectLikesByMomentId(any())).thenReturn(List.of());
        lenient().when(momentMapper.selectCommentsByMomentId(any())).thenReturn(List.of());
        lenient().when(momentMapper.countLikes(any())).thenReturn(0);

        List<MomentMedia> media = service.listAll().get(0).getMediaList();

        assertThat(media).hasSize(1);
        assertThat(media.get(0).getUrl()).isEqualTo("/uploads/fallback.jpg");
    }
}
