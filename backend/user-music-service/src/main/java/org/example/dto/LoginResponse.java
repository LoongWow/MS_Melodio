package org.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
public class LoginResponse {
    private Long userId;
    private String message;
    private String nickname;

    public LoginResponse(Long userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public LoginResponse(Long userId, String message, String nickname) {
        this.userId = userId;
        this.message = message;
        this.nickname = nickname;
    }
}

