package org.example.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayHistory {

    private Long id;

    private Long userId;

    private Long songId;

    private String songName;

    private String artist;

    private Integer playDuration;

    private Boolean completed;

    private String genre;

    private String genreSource;

    private LocalDateTime createdAt;

}
