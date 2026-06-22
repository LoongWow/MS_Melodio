package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.MusicSongDto;
import org.example.entity.PlayHistory;
import org.example.entity.UserPreference;
import org.example.repo.UserPreferenceMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceService {

    private final UserPreferenceMapper userPreferenceMapper;
    private final PlayHistoryService playHistoryService;
    private final MusicApiService musicApiService;

    /**
     * 更新用户瀵规煇涓壓鏈鐨勫亸濂?
     */
    @Transactional
    public void updatePreference(Long userId, String artist) {
        Optional<UserPreference> existing = userPreferenceMapper.findByUserIdAndArtist(userId, artist);

        if (existing.isPresent()) {
            UserPreference preference = existing.get();
            preference.setPlayCount(preference.getPlayCount() + 1);
            preference.setLastPlayedAt(LocalDateTime.now());
            userPreferenceMapper.save(preference);
        } else {
            UserPreference preference = new UserPreference();
            preference.setUserId(userId);
            preference.setArtist(artist);
            preference.setPlayCount(1);
            preference.setLastPlayedAt(LocalDateTime.now());
            userPreferenceMapper.save(preference);
        }
    }

    /**
     * 鑾峰彇用户鏈€鍠滄鐨勮壓鏈列表
     */
    public List<String> getTopArtists(Long userId, int limit) {
        List<String> artists = userPreferenceMapper
                .findTopArtistsByUserIdOrderByPlayCountAndRecency(userId, limit)
                .stream()
                .map(UserPreference::getArtist)
                .collect(Collectors.toList());
        log.info("[MusicMemory] 查询用户常听歌手: userId={}, limit={}, artists={}", userId, limit, artists);
        return artists;
    }

    /**
     * 鑾峰彇推荐鍏抽敭璇嶏紙基于用户偏好锛?
     */
    public List<String> getRecommendationKeywords(Long userId) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();

        List<PlayHistory> completedPlays = playHistoryService.getRecentHistory(userId, 7).stream()
                .filter(PlayHistory::getCompleted)
                .limit(20)
                .collect(java.util.stream.Collectors.toList());

        if (!completedPlays.isEmpty()) {
            Map<String, Long> artistFrequency = completedPlays.stream()
                    .collect(Collectors.groupingBy(PlayHistory::getArtist, Collectors.counting()));

            artistFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .forEach(keywords::add);
        }

        if (keywords.size() < 3) {
            getTopArtists(userId, 3).forEach(keywords::add);
        }

        List<String> result = new ArrayList<>(keywords).stream().limit(3).collect(Collectors.toList());
        log.info("[Recommend] 生成涓€у寲推荐鍏抽敭璇? userId={}, keywords={}", userId, result);
        return result;
    }

    /**
     * 基于用户偏好生成涓€у寲推荐
     */
    public List<MusicSongDto> generateRecommendations(Long userId, int count) {
        List<String> topArtists = getRecommendationKeywords(userId);

        if (topArtists.isEmpty()) {
            log.info("[Recommend] 用户没有可用偏好锛岃繑鍥炵儹闂ㄦ帹鑽? userId={}", userId);
            List<MusicSongDto> hotSongs = musicApiService.searchSongs("热门音乐");
            return hotSongs.stream().limit(count).collect(Collectors.toList());
        }

        log.info("[Recommend] 用户偏好鑹烘湳瀹? userId={}, artists={}", userId, topArtists);

        Set<Long> playedSongIds = playHistoryService.getRecentHistory(userId, 60).stream()
                .map(PlayHistory::getSongId)
                .collect(Collectors.toSet());

        List<MusicSongDto> recommendations = new ArrayList<>();
        int songsPerArtist = Math.max(2, count / topArtists.size());

        for (String artist : topArtists) {
            List<MusicSongDto> songs = musicApiService.searchSongs(artist);

            List<MusicSongDto> newSongs = songs.stream()
                    .filter(song -> !playedSongIds.contains(song.getId()))
                    .filter(song -> recommendations.stream().noneMatch(r -> r.getId().equals(song.getId())))
                    .limit(songsPerArtist)
                    .collect(Collectors.toList());

            recommendations.addAll(newSongs);

            if (recommendations.size() >= count) {
                break;
            }
        }

        if (recommendations.size() < count) {
            int needed = count - recommendations.size();
            List<MusicSongDto> hotSongs = musicApiService.searchSongs("热门音乐");

            List<MusicSongDto> newHotSongs = hotSongs.stream()
                    .filter(song -> !playedSongIds.contains(song.getId()))
                    .filter(song -> recommendations.stream().noneMatch(r -> r.getId().equals(song.getId())))
                    .limit(needed)
                    .collect(Collectors.toList());

            recommendations.addAll(newHotSongs);
        }

        List<MusicSongDto> result = recommendations.stream()
                .limit(count)
                .collect(Collectors.toList());

        log.info("[Recommend] 生成涓€у寲推荐缁撴灉: userId={}, resultCount={}", userId, result.size());
        return result;
    }

    /**
     * 分析用户偏好统计信息
     */
    public Map<String, Object> analyzeUserPreference(Long userId) {
        List<UserPreference> preferences = userPreferenceMapper.findTopArtistsByUserId(userId);
        List<PlayHistory> recentHistory = playHistoryService.getRecentHistory(userId, 30);

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("totalArtists", preferences.size());
        analysis.put("topArtists", preferences.stream()
                .limit(5)
                .map(p -> Map.of(
                        "artist", p.getArtist(),
                        "playCount", p.getPlayCount(),
                        "lastPlayed", p.getLastPlayedAt()
                ))
                .collect(Collectors.toList()));
        analysis.put("recentPlaysCount", recentHistory.size());
        analysis.put("completedPlaysCount", recentHistory.stream()
                .filter(PlayHistory::getCompleted)
                .count());

        return analysis;
    }
}