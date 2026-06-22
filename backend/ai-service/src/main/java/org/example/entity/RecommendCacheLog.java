package org.example.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecommendCacheLog {

    private Long id;

    private UserAccount user;

    private String queryText;

    private String songsJson;

    private LocalDateTime createdAt;

}
