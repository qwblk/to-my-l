package com.panpeixue.myl.service;

import com.panpeixue.myl.mapper.UserMapper;
import com.panpeixue.myl.service.impl.UserServiceImpl;
import com.panpeixue.myl.websocket.WebSocketSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Service-level unit tests for the cross-device "last seen" sync feature.
 * Plain Mockito — no Spring context, so no MySQL / Redis required.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceLastSeenTest {

    @Mock
    UserMapper userMapper;

    @Mock
    WebSocketSessionManager sessionManager;

    @InjectMocks
    UserServiceImpl userService;

    @Test
    void heartbeatIsIdempotent_canBeCalledRepeatedlyWithoutError() {
        when(userMapper.updateLastSeenAt(1L)).thenReturn(1);

        userService.heartbeat(1L);
        userService.heartbeat(1L);
        userService.heartbeat(1L);

        verify(userMapper, times(3)).updateLastSeenAt(1L);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void getLastSeenAt_returnsNullForNeverActiveUser() {
        when(userMapper.selectLastSeenAt(1L)).thenReturn(null);

        assertThat(userService.getLastSeenAt(1L)).isNull();
    }

    @Test
    void getLastSeenAt_returnsStoredTimestamp() {
        LocalDateTime stored = LocalDateTime.of(2026, 6, 13, 10, 0, 0);
        when(userMapper.selectLastSeenAt(7L)).thenReturn(stored);

        assertThat(userService.getLastSeenAt(7L)).isEqualTo(stored);
    }

    /**
     * alice (id=1) calling getLastSeenAt must only ever touch alice's row,
     * never bob's (id=2). The controller derives userId from StpUtil.getLoginIdAsLong(),
     * so as long as the service forwards exactly that id into the mapper,
     * cross-user leakage is impossible.
     */
    @Test
    void aliceCannotReadBobsLastSeen() {
        LocalDateTime aliceTs = LocalDateTime.of(2026, 6, 10, 8, 0);
        LocalDateTime bobTs = LocalDateTime.of(2026, 6, 12, 23, 30);
        when(userMapper.selectLastSeenAt(1L)).thenReturn(aliceTs);
        // bob's stub is intentionally lenient: we *want* it set up so the mock
        // could return bob's timestamp if asked — and then assert it never is.
        lenient().when(userMapper.selectLastSeenAt(2L)).thenReturn(bobTs);

        // alice asks for "her own" timestamp — service must hit row 1, not row 2
        LocalDateTime got = userService.getLastSeenAt(1L);

        assertThat(got).isEqualTo(aliceTs).isNotEqualTo(bobTs);
        verify(userMapper).selectLastSeenAt(1L);
        verify(userMapper, never()).selectLastSeenAt(2L);
    }
}
