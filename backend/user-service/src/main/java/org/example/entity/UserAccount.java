package org.example.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserAccount {

    private Long id;

    private Long musicUserId;

    private String nickname;

    private String avatarUrl;

    private String account;

    private String password;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
