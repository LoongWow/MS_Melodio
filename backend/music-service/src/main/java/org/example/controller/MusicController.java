package org.example.controller;

import javax.validation.Valid;
import org.example.dto.*;
import org.example.entity.PlayHistory;
import org.example.service.MusicApiService;
import org.example.service.PlayHistoryService;
import org.example.service.UserPreferenceService;
import org.example.tools.MusicTools;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class MusicController {

    private final MusicApiService musicApiService;
    private final MusicTools musicTools;
    private final PlayHistoryService playHistoryService;
    private final UserPreferenceService userPreferenceService;

    public MusicController(MusicApiService musicApiService,
                          MusicTools musicTools,
                          PlayHistoryService playHistoryService,
                          UserPreferenceService userPreferenceService) {
        this.musicApiService = musicApiService;
        this.musicTools = musicTools;
        this.playHistoryService = playHistoryService;
        this.userPreferenceService = userPreferenceService;
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String keywords, @RequestParam Long userId) {
        List<MusicSongDto> songs = musicTools.searchSongs(keywords, userId);
        return new SearchResponse(songs);
    }

    @GetMapping("/play-url")
    public PlayUrlResponse playUrl(@RequestParam Long songId, @RequestParam Long userId) {
        return new PlayUrlResponse(songId, musicTools.getPlayUrl(songId, userId));
    }

    @GetMapping("/cache-song-id")
    public Long cacheSongId(@RequestParam int index, @RequestParam Long userId) {
        return musicTools.getSongFromCache(index, userId);
    }

    @GetMapping("/lyric/new")
    public ResponseEntity<?> lyric(@RequestParam Long id) {
        return ResponseEntity.ok(musicApiService.getLyric(id));
    }

    @PostMapping("/play-history")
    public ResponseEntity<?> recordPlayHistory(@Valid @RequestBody PlayHistoryRequest request) {
        playHistoryService.recordPlay(
            request.getUserId(),
            request.getSongId(),
            request.getSongName(),
            request.getArtist(),
            request.getDuration() != null ? request.getDuration().intValue() : 0,
            request.getCompleted() != null ? request.getCompleted() : false
        );
        userPreferenceService.updatePreference(request.getUserId(), request.getArtist());

        return ResponseEntity.ok(Map.of("success", true, "message", "播放历史已记录"));
    }

    @GetMapping("/user-preference")
    public ResponseEntity<?> getUserPreference(@RequestParam Long userId) {
        Map<String, Object> analysis = userPreferenceService.analyzeUserPreference(userId);
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/play-history")
    public ResponseEntity<List<PlayHistory>> getPlayHistory(@RequestParam Long userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        List<PlayHistory> history = playHistoryService.getUniqueRecentHistory(userId);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/play-history")
    public ResponseEntity<?> clearPlayHistory(@RequestParam Long userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        playHistoryService.clearHistory(userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "播放历史已清空"));
    }
}
