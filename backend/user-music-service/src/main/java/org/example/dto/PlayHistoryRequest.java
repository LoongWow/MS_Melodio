package org.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayHistoryRequest {
    private Long userId;
    private Long songId;
    private String songName;
    private String artist;
    private Long duration;
    private Boolean completed;
}
