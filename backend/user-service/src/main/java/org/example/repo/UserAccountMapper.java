package org.example.repo;

import org.apache.ibatis.annotations.*;
import org.example.entity.UserAccount;
import java.util.Optional;

@Mapper
public interface UserAccountMapper {

    @Select("SELECT * FROM user_account WHERE music_user_id = #{musicUserId}")
    Optional<UserAccount> findByMusicUserId(@Param("musicUserId") Long musicUserId);

    @Select("SELECT * FROM user_account WHERE account = #{account}")
    Optional<UserAccount> findByAccount(@Param("account") String account);

    @Select("SELECT * FROM user_account WHERE id = #{id}")
    Optional<UserAccount> findById(@Param("id") Long id);

    @Insert("INSERT INTO user_account(music_user_id, nickname, avatar_url, account, password, created_at, updated_at) " +
            "VALUES(#{musicUserId}, #{nickname}, #{avatarUrl}, #{account}, #{password}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE nickname=VALUES(nickname), avatar_url=VALUES(avatar_url), password=VALUES(password), updated_at=NOW()")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(UserAccount userAccount);
}
