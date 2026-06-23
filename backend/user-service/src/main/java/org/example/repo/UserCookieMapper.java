package org.example.repo;

import org.apache.ibatis.annotations.*;
import org.example.entity.UserCookie;
import java.util.Optional;

@Mapper
public interface UserCookieMapper {

    @Select("SELECT * FROM user_cookie WHERE user_id = #{userId} ORDER BY updated_at DESC LIMIT 1")
    @Results({
        @Result(property = "user.id", column = "user_id")
    })
    Optional<UserCookie> findTopByUser_IdOrderByUpdatedAtDesc(@Param("userId") Long userId);

    @Select("SELECT c.* FROM user_cookie c JOIN user_account a ON c.user_id = a.id WHERE a.music_user_id = #{musicUserId} ORDER BY c.updated_at DESC LIMIT 1")
    @Results({
        @Result(property = "user.id", column = "user_id")
    })
    Optional<UserCookie> findTopByUser_MusicUserIdOrderByUpdatedAtDesc(@Param("musicUserId") Long musicUserId);

    @Insert("INSERT INTO user_cookie(user_id, cookie_ciphertext, expires_at, created_at, updated_at) " +
            "VALUES(#{user.id}, #{cookieCiphertext}, #{expiresAt}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE cookie_ciphertext=VALUES(cookie_ciphertext), expires_at=VALUES(expires_at), updated_at=NOW()")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(UserCookie cookie);
}
