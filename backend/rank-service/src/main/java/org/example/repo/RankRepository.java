package org.example.repo;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.entity.RankItem;
import java.util.List;

@Mapper
public interface RankRepository {

    /**
     * 热歌榜 - 按播放总次数排序
     */
    @Select("SELECT song_id as songId, song_name as songName, artist, " +
            "COUNT(*) as playCount, MAX(created_at) as lastPlayedAt " +
            "FROM play_history " +
            "GROUP BY song_id, song_name, artist " +
            "ORDER BY playCount DESC, lastPlayedAt DESC " +
            "LIMIT 100")
    List<RankItem> getHotRank();

    /**
     * 新歌榜 - 最近7天内的热门歌曲
     */
    @Select("SELECT song_id as songId, song_name as songName, artist, " +
            "COUNT(*) as playCount, MAX(created_at) as lastPlayedAt " +
            "FROM play_history " +
            "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY song_id, song_name, artist " +
            "ORDER BY playCount DESC, lastPlayedAt DESC " +
            "LIMIT 100")
    List<RankItem> getNewRank();

    /**
     * 飙升榜 - 最近3天播放量对比前7天的增长
     */
    @Select("SELECT t1.song_id as songId, t1.song_name as songName, t1.artist, " +
            "t1.recent_count as playCount, t1.last_played as lastPlayedAt " +
            "FROM ( " +
            "  SELECT song_id, song_name, artist, " +
            "         COUNT(*) as recent_count, " +
            "         MAX(created_at) as last_played " +
            "  FROM play_history " +
            "  WHERE created_at >= DATE_SUB(NOW(), INTERVAL 3 DAY) " +
            "  GROUP BY song_id, song_name, artist " +
            ") t1 " +
            "LEFT JOIN ( " +
            "  SELECT song_id, COUNT(*) as old_count " +
            "  FROM play_history " +
            "  WHERE created_at >= DATE_SUB(NOW(), INTERVAL 10 DAY) " +
            "    AND created_at < DATE_SUB(NOW(), INTERVAL 3 DAY) " +
            "  GROUP BY song_id " +
            ") t2 ON t1.song_id = t2.song_id " +
            "ORDER BY (t1.recent_count / COALESCE(NULLIF(t2.old_count, 0), 1)) DESC, t1.recent_count DESC " +
            "LIMIT 100")
    List<RankItem> getRisingRank();
}
