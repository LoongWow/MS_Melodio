package org.example.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MusicKnowledge {

    private Long id;

    private String title;

    private String content;

    private String knowledgeType; // artist, genre, song, music_theory, etc.

    private String relatedKeywords; // 逗号分隔的关键词

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
