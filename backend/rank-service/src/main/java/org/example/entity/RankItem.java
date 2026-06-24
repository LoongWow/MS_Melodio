package org.example.entity;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class RankItem {
    private Long songId;
    private String songName;
    private String artist;
    private Long playCount;
    private Timestamp lastPlayedAt;
}
