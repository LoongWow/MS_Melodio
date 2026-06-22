package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.AgentReply;
import org.example.dto.ChatRequest;
import org.example.dto.GenreResult;
import org.example.dto.MusicSongDto;
import org.example.service.AgentFacadeService;
import org.example.service.GenreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/music")
public class AiMusicController {

    private final AgentFacadeService agentFacadeService;
    private final GenreService genreService;

    public AiMusicController(AgentFacadeService agentFacadeService, GenreService genreService) {
        this.agentFacadeService = agentFacadeService;
        this.genreService = genreService;
    }

    @PostMapping("/chat")
    public AgentReply chat(@Valid @RequestBody ChatRequest request) {
        return agentFacadeService.chat(request.getMessage(), request.getUserId());
    }

    @GetMapping("/greeting")
    public AgentReply getGreeting(@RequestParam Long userId) {
        return agentFacadeService.generateGreeting(userId);
    }

    @GetMapping("/genre")
    public GenreResult getGenre(@RequestParam Long songId,
                                @RequestParam String songName,
                                @RequestParam String artist) {
        return genreService.getGenre(songId, songName, artist);
    }

    @PostMapping("/genres/batch")
    public Map<Long, String> getBatchGenres(@RequestBody List<MusicSongDto> songs) {
        return genreService.batchLoadGenres(songs);
    }
}
