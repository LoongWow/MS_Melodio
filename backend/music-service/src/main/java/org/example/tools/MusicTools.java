package org.example.tools;

import org.example.dto.MusicSongDto;
import org.example.service.MusicApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MusicTools {

    private final MusicApiService musicApiService;
    private final RedisTemplate<String, Object> redisTemplate;

    public MusicTools(MusicApiService musicApiService,
                     @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.musicApiService = musicApiService;
        this.redisTemplate = redisTemplate;
    }

    public List<MusicSongDto> searchSongs(String keywords, Long userId) {
        List<MusicSongDto> songs = musicApiService.searchSongs(keywords);
        if (userId != null) {
            musicApiService.cacheRecommend(userId, songs);
        }
        return songs;
    }

    public String getPlayUrl(Long songId, Long userId) {
        if (userId == null) {
            return null;
        }
        String cookie = musicApiService.getAuthCookie(userId);
        if (cookie == null) {
            return null;
        }
        return musicApiService.getPlayUrl(songId, cookie);
    }

    public Long getSongFromCache(int index, Long userId) {
        if (userId == null || index < 1) {
            return null;
        }
        List<MusicSongDto> songs = musicApiService.getCachedRecommend(userId);
        if (index > songs.size()) {
            return null;
        }
        return songs.get(index - 1).getId();
    }

    public String logout(Long userId) {
        if (redisTemplate != null) {
            redisTemplate.delete("music:auth:" + userId);
        }
        return "logout_success";
    }

    public String pauseMusic(Long userId) {
        return "pause_music";
    }

    public String resumeMusic(Long userId) {
        return "resume_music";
    }

    public String playNext(Long userId) {
        return "play_next";
    }

    public String playPrevious(Long userId) {
        return "play_previous";
    }

    public String setVolume(int volume, Long userId) {
        if (volume < 0 || volume > 100) {
            return "error:音量必须在0-100之间";
        }
        return "set_volume:" + volume;
    }

    public String getPlayerStatus(Long userId) {
        return "get_status";
    }
}
