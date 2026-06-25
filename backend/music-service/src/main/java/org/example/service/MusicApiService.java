package org.example.service;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.client.UserCookieFeignClient;
import org.example.dto.MusicSongDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RefreshScope
public class MusicApiService {

    private static final Logger log = LoggerFactory.getLogger(MusicApiService.class);

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserCookieFeignClient userCookieFeignClient;
    private final String baseUrl;

    public MusicApiService(RestTemplate restTemplate,
                           @Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
                           ObjectMapper objectMapper,
                           UserCookieFeignClient userCookieFeignClient,
                           @Value("${music.api.base-url:http://localhost:3000}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.userCookieFeignClient = userCookieFeignClient;
        this.baseUrl = baseUrl;

        if (redisTemplate == null) {
            log.warn("MusicApiService: Redis is unavailable, using DB only");
        }
    }

    public List<MusicSongDto> searchSongs(String keywords) {
        try {
            JsonNode node = restTemplate.getForObject(baseUrl + "/search?keywords={keywords}&type=1", JsonNode.class, keywords);
            JsonNode songsNode = node == null ? null : node.path("result").path("songs");
            List<MusicSongDto> songs = new ArrayList<>();
            if (songsNode != null && songsNode.isArray()) {
                songsNode.forEach(song -> {
                    JsonNode artistsNode = song.path("artists");
                    if (artistsNode.isMissingNode()) {
                        artistsNode = song.path("ar");
                    }

                    String artist = Optional.ofNullable(artistsNode)
                            .filter(JsonNode::isArray)
                            .map(arr -> {
                                List<String> names = new ArrayList<>();
                                arr.forEach(a -> names.add(a.path("name").asText("Unknown")));
                                return String.join("/", names);
                            })
                            .orElse("Unknown");
                    songs.add(new MusicSongDto(song.path("id").asLong(), song.path("name").asText("Unknown"), artist));
                });
            }
            return songs;
        } catch (Exception e) {
            log.error("搜索歌曲失败，关键词: {}, 错误: {}", keywords, e.getMessage(), e);
            return List.of();
        }
    }

    public String getPlayUrl(Long songId, String cookie) {
        try {
            String formattedCookie = cookie;
            if (!cookie.contains("MUSIC_U=")) {
                formattedCookie = "MUSIC_U=" + cookie;
            }

            // 不使用 UriComponentsBuilder，因为它会对 Cookie 进行 URL 编码
            // 网易云 API 需要原始的 Cookie 字符串
            String url = String.format(
                "%s/song/url/v1?id=%d&level=lossless&cookie=%s",
                baseUrl, songId, formattedCookie
            );

            log.debug("请求播放 URL: {}", url);

            JsonNode node = restTemplate.getForObject(url, JsonNode.class);
            JsonNode data = node == null ? null : node.path("data");

            if (data != null && data.isArray() && !data.isEmpty()) {
                JsonNode firstItem = data.get(0);
                String playUrl = firstItem.path("url").asText(null);
                String level = firstItem.path("level").asText("");
                String type = firstItem.path("type").asText("");
                long size = firstItem.path("size").asLong(0);
                long br = firstItem.path("br").asLong(0);

                log.info("获取播放 URL 成功: songId={}, level={}, type={}, size={} bytes, br={}",
                    songId, level, type, size, br);

                return playUrl;
            }

            log.warn("未找到播放 URL: songId={}", songId);
            return null;
        } catch (Exception e) {
            log.error("获取播放链接失败，songId: {}, 错误: {}", songId, e.getMessage(), e);
            return null;
        }
    }

    public Map<String, Object> getLyric(Long songId) {
        String url = baseUrl + "/lyric/new?id=" + songId;

        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response != null && response.path("code").asInt() == 200) {
                Map<String, Object> result = new HashMap<>();

                // 获取逐字歌词
                JsonNode yrc = response.path("yrc");
                if (!yrc.isMissingNode() && !yrc.isNull()) {
                    result.put("yrc", Map.of(
                        "version", yrc.path("version").asInt(0),
                        "lyric", yrc.path("lyric").asText("")
                    ));
                }

                // 获取普通歌词（作为备用）
                JsonNode lrc = response.path("lrc");
                if (!lrc.isMissingNode() && !lrc.isNull()) {
                    result.put("lrc", Map.of(
                        "version", lrc.path("version").asInt(0),
                        "lyric", lrc.path("lyric").asText("")
                    ));
                }

                return result;
            }

            log.warn("Failed to get lyric for songId={}, response code: {}",
                songId, response != null ? response.path("code").asInt() : "null");
        } catch (Exception e) {
            log.error("Error fetching lyric for songId={}", songId, e);
        }

        return Map.of();
    }

    /**
     * 根据网易云音乐用户 ID 获取认证 Cookie
     * 通过 Feign Client 调用 user-service
     * @param musicUserId 网易云音乐用户 ID（不是数据库主键 ID）
     * @return Cookie 字符串
     */
    public String getAuthCookie(Long musicUserId) {
        // 优先从 Redis 读取（如果可用）
        if (redisTemplate != null) {
            try {
                Object value = redisTemplate.opsForValue().get(authKey(musicUserId));
                if (value != null) {
                    log.debug("从 Redis 获取 Cookie: musicUserId={}", musicUserId);
                    return value.toString();
                }
            } catch (Exception e) {
                log.warn("Redis 读取失败，降级到 user-service: {}", e.getMessage());
            }
        }

        // 通过 Feign Client 从 user-service 获取
        try {
            String cookie = userCookieFeignClient.getCookie(musicUserId);
            if (cookie != null) {
                log.debug("从 user-service 获取 Cookie: musicUserId={}", musicUserId);
                // 缓存到 Redis（如果可用）
                if (redisTemplate != null) {
                    try {
                        redisTemplate.opsForValue().set(authKey(musicUserId), cookie, Duration.ofDays(30));
                    } catch (Exception e) {
                        log.warn("Redis 写入失败: {}", e.getMessage());
                    }
                }
            }
            return cookie;
        } catch (Exception e) {
            log.error("从 user-service 获取 Cookie 失败: musicUserId={}", musicUserId, e);
            return null;
        }
    }

    public void cachePendingSwitchAccount(Long userId, String account) {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(pendingSwitchKey(userId), account, Duration.ofMinutes(10));
            } catch (Exception e) {
                log.warn("Redis 写入失败: {}", e.getMessage());
            }
        }
    }

    public Optional<String> getPendingSwitchAccount(Long userId) {
        if (redisTemplate != null) {
            try {
                Object value = redisTemplate.opsForValue().get(pendingSwitchKey(userId));
                return value == null ? Optional.empty() : Optional.of(value.toString());
            } catch (Exception e) {
                log.warn("Redis 读取失败: {}", e.getMessage());
            }
        }
        return Optional.empty();
    }

    public void clearPendingSwitchAccount(Long userId) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(pendingSwitchKey(userId));
            } catch (Exception e) {
                log.warn("Redis 删除失败: {}", e.getMessage());
            }
        }
    }

    private String writeJson(List<MusicSongDto> songs) {
        try {
            return objectMapper.writeValueAsString(songs);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String authKey(Long userId) {
        return "music:auth:" + userId;
    }

    private String recommendKey(Long userId) {
        return "music:recommend:" + userId;
    }

    private String pendingSwitchKey(Long userId) {
        return "music:pending-switch:" + userId;
    }

    private String conversationHistoryKey(Long userId) {
        return "conversation:history:" + userId;
    }

    public void cacheRecommend(Long userId, java.util.List<org.example.dto.MusicSongDto> songs) {
        if (redisTemplate == null) return;
        String key = "music:recommend:" + userId;
        redisTemplate.opsForValue().set(key, songs, java.time.Duration.ofHours(24));
    }

    public java.util.List<org.example.dto.MusicSongDto> getCachedRecommend(Long userId) {
        if (redisTemplate == null) return java.util.Collections.emptyList();
        String key = "music:recommend:" + userId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof java.util.List) {
            return (java.util.List<org.example.dto.MusicSongDto>) cached;
        }
        return java.util.Collections.emptyList();
    }
}
