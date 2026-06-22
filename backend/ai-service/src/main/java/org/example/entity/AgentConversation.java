package org.example.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentConversation {

    private Long id;

    private UserAccount user;

    private String role;

    private String content;

    private LocalDateTime createdAt;

}
