package org.example.repo;

import org.apache.ibatis.annotations.*;
import org.example.entity.RecommendCacheLog;
import java.util.Optional;

@Mapper
public interface RecommendCacheLogMapper {

    @Select("SELECT c.* FROM recommend_cache_log c JOIN user_account a ON c.user_id = a.id WHERE a.music_user_id = #{musicUserId} ORDER BY c.created_at DESC LIMIT 1")
    @Results({
        @Result(property = "user.id", column = "user_id")
    })
    Optional<RecommendCacheLog> findTop1ByUser_MusicUserIdOrderByCreatedAtDesc(@Param("musicUserId") Long musicUserId);

    @Select("SELECT * FROM recommend_cache_log WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT 1")
    Optional<RecommendCacheLog> findTopByUser_IdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Insert("INSERT INTO recommend_cache_log(user_id, query_text, songs_json, created_at) " +
            "VALUES(#{user.id}, #{queryText}, #{songsJson}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(RecommendCacheLog log);
}
