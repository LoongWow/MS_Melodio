package org.example.repo;

import org.apache.ibatis.annotations.*;
import org.example.entity.PlayHistory;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PlayHistoryMapper {

    @Select("SELECT * FROM play_history WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<PlayHistory> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Select("SELECT * FROM play_history WHERE user_id = #{userId} AND created_at >= #{since} ORDER BY created_at DESC")
    List<PlayHistory> findRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Select("SELECT * FROM play_history WHERE user_id = #{userId} AND completed = 1 ORDER BY created_at DESC")
    List<PlayHistory> findCompletedByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM play_history WHERE song_id = #{songId} AND genre IS NOT NULL ORDER BY created_at DESC LIMIT 1")
    java.util.Optional<PlayHistory> findTopBySongIdAndGenreIsNotNullOrderByCreatedAtDesc(@Param("songId") Long songId);

    @Select("SELECT * FROM play_history WHERE user_id = #{userId} AND song_id = #{songId} LIMIT 1")
    PlayHistory findByUserIdAndSongId(@Param("userId") Long userId, @Param("songId") Long songId);

    @Select("SELECT count(*) FROM play_history WHERE user_id = #{userId} AND completed = 1")
    long countByUserIdAndCompletedTrue(@Param("userId") Long userId);

    @Insert("INSERT INTO play_history(user_id, song_id, song_name, artist, play_duration, completed, genre, genre_source, created_at) " +
            "VALUES(#{userId}, #{songId}, #{songName}, #{artist}, #{playDuration}, #{completed}, #{genre}, #{genreSource}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(PlayHistory playHistory);
    
    @Update("UPDATE play_history SET play_duration=#{playDuration}, completed=#{completed} WHERE id=#{id}")
    void update(PlayHistory playHistory);

    @Delete("DELETE FROM play_history WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);
}
