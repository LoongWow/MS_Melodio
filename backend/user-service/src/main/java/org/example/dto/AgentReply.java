package org.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentReply {
    private String reply;
    private List<MusicSongDto> songs;
    private Long songId;
    private String action;
    private Long newUserId;
    private String newNickname;
}

