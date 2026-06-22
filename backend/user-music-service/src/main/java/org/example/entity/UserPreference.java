package org.example.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    private Long id;

    private Long userId;

    private String artist;

    private Integer playCount;

    private LocalDateTime lastPlayedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
