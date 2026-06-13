package com.panpeixue.myl.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private Long id;
    private String name;
    private Integer gender;
    private String username;
    private LocalDate birthday;
    private Integer age;
    private LocalDateTime createTime;
}