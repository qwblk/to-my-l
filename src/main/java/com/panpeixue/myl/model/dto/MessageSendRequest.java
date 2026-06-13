package com.panpeixue.myl.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendRequest {
    private Long receiverId;
    private String content;
}