package org.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 播放历史记录请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayHistoryRequest {
    private Long userId;
    private Long songId;
    private String songName;
    private String artist;
    private String album;
    private Long duration;
    private Boolean completed;
}

