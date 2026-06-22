package org.example.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCookie {

    private Long id;

    private UserAccount user;

    private String cookieCiphertext;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
