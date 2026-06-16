package com.panpeixue.myl.service;

import com.panpeixue.myl.mapper.UserMapper;
import com.panpeixue.myl.model.pojo.User;
import com.panpeixue.myl.service.impl.UserServiceImpl;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileTextTest {

    @Mock
    UserMapper userMapper;

    @Mock
    WebSocketSessionManager sessionManager;

    @Test
    void profileTextAliasesBioOnUserPojo() {
        User user = new User();
        user.setProfileText("写给你的一段话");

        assertThat(user.getBio()).isEqualTo("写给你的一段话");
        assertThat(user.getProfileText()).isEqualTo("写给你的一段话");
    }

    @Test
    void updateInfo_updatesProfileTextThroughBioColumn() {
        UserServiceImpl service = new UserServiceImpl(userMapper, sessionManager);
        User update = new User();
        update.setName("  王水群  ");
        update.setGender(1);
        update.setBirthday(LocalDate.of(2002, 1, 1));
        update.setProfileText("这里是一段很长的话");
        User saved = new User();
        saved.setId(1L);
        saved.setName("王水群");
        saved.setProfileText("这里是一段很长的话");
        when(userMapper.selectById(1L)).thenReturn(saved);

        User result = service.updateInfo(1L, update);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateInfo(captor.capture());
        User persisted = captor.getValue();
        assertThat(persisted.getId()).isEqualTo(1L);
        assertThat(persisted.getName()).isEqualTo("王水群");
        assertThat(persisted.getBio()).isEqualTo("这里是一段很长的话");
        assertThat(result.getProfileText()).isEqualTo("这里是一段很长的话");
    }

    @Test
    void updateInfo_allowsEmptyProfileText() {
        UserServiceImpl service = new UserServiceImpl(userMapper, sessionManager);
        User update = new User();
        update.setName("王水群");
        update.setGender(1);
        update.setProfileText("");
        when(userMapper.selectById(1L)).thenReturn(new User());

        service.updateInfo(1L, update);

        verify(userMapper).updateInfo(any(User.class));
    }

    @Test
    void updateInfo_rejectsBlankName() {
        UserServiceImpl service = new UserServiceImpl(userMapper, sessionManager);
        User update = new User();
        update.setName("   ");

        assertThatThrownBy(() -> service.updateInfo(1L, update))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Name");
        verifyNoInteractions(userMapper);
    }

    @Test
    void updateInfo_rejectsInvalidGender() {
        UserServiceImpl service = new UserServiceImpl(userMapper, sessionManager);
        User update = new User();
        update.setName("王水群");
        update.setGender(2);

        assertThatThrownBy(() -> service.updateInfo(1L, update))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Gender");
    }

    @Test
    void updateInfo_rejectsTooLongProfileText() {
        UserServiceImpl service = new UserServiceImpl(userMapper, sessionManager);
        User update = new User();
        update.setName("王水群");
        update.setGender(1);
        update.setProfileText("x".repeat(10001));

        assertThatThrownBy(() -> service.updateInfo(1L, update))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("10000");
    }
}
