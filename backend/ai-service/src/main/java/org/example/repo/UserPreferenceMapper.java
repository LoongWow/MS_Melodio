package org.example.repo;

import org.apache.ibatis.annotations.*;
import org.example.entity.UserPreference;
import java.util.Optional;
import java.util.List;

@Mapper
public interface UserPreferenceMapper {

    @Select("SELECT * FROM user_preference WHERE user_id = #{userId} AND artist = #{artist} LIMIT 1")
    Optional<UserPreference> findByUserIdAndArtist(@Param("userId") Long userId, @Param("artist") String artist);

    @Select("SELECT * FROM user_preference WHERE user_id = #{userId} ORDER BY play_count DESC, last_played_at DESC LIMIT #{limit}")
    List<UserPreference> findTopArtistsByUserIdOrderByPlayCountAndRecency(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT * FROM user_preference WHERE user_id = #{userId} ORDER BY play_count DESC")
    List<UserPreference> findTopArtistsByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO user_preference(user_id, artist, play_count, last_played_at, created_at, updated_at) " +
            "VALUES(#{userId}, #{artist}, #{playCount}, #{lastPlayedAt}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE play_count=VALUES(play_count), last_played_at=VALUES(last_played_at), updated_at=NOW()")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(UserPreference userPreference);
}
