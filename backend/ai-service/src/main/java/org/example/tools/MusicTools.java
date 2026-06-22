package org.example.tools;

import dev.langchain4j.agent.tool.Tool;
import org.example.dto.LoginResponse;
import org.example.dto.MusicSongDto;
import org.example.service.MusicApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Component
public class MusicTools {

    private final MusicApiService musicApiService;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public MusicTools(MusicApiService musicApiService,
                     RestTemplate restTemplate,
                     @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.musicApiService = musicApiService;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Tool("Search songs by keywords and cache the recommendation list for the current user")
    public List<MusicSongDto> searchSongs(String keywords, Long userId) {
        List<MusicSongDto> songs = musicApiService.searchSongs(keywords);
        if (userId != null) {
            musicApiService.cacheRecommend(userId, songs);
        }
        return songs;
    }

    @Tool("Get playable URL of a song for the current user")
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

    @Tool("Get song id from cached recommendation list by 1-based index")
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

    @Tool("退出当前用户的登录状态，清除服务端缓存")
    public String logout(Long userId) {
        if (redisTemplate != null) {
            redisTemplate.delete("music:auth:" + userId);
        }
        return "logout_success";
    }

    @Tool("切换到另一个账号，需要提供账号名和密码")
    public String switchAccount(String account, String password, Long currentUserId) {
        try {
            Map<String, String> request = new HashMap<>();
            request.put("account", account);
            request.put("password", password);
            LoginResponse response = restTemplate.postForObject("http://user-music-service/api/music/login", request, LoginResponse.class);
            if (response == null || response.getUserId() == null) {
                return "error:" + (response != null ? response.getMessage() : "登录失败");
            }
            return "success:" + response.getUserId() + ":" + response.getMessage();
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    @Tool("暂停当前播放的音乐")
    public String pauseMusic(Long userId) {
        return "pause_music";
    }

    @Tool("继续播放当前音乐")
    public String resumeMusic(Long userId) {
        return "resume_music";
    }

    @Tool("播放下一首歌曲")
    public String playNext(Long userId) {
        return "play_next";
    }

    @Tool("播放上一首歌曲")
    public String playPrevious(Long userId) {
        return "play_previous";
    }

    @Tool("调整音量，volume范围0-100")
    public String setVolume(int volume, Long userId) {
        if (volume < 0 || volume > 100) {
            return "error:音量必须在0-100之间";
        }
        return "set_volume:" + volume;
    }

    @Tool("获取当前播放状态")
    public String getPlayerStatus(Long userId) {
        return "get_status";
    }
}
