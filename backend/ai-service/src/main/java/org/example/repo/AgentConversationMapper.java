package org.example.repo;

import org.apache.ibatis.annotations.*;
import org.example.entity.AgentConversation;
import java.util.List;

@Mapper
public interface AgentConversationMapper {

    @Select("SELECT c.* FROM agent_conversation c JOIN user_account a ON c.user_id = a.id WHERE a.music_user_id = #{musicUserId} ORDER BY c.created_at DESC LIMIT 10")
    @Results({
        @Result(property = "user.id", column = "user_id")
    })
    List<AgentConversation> findTop10ByUser_MusicUserIdOrderByCreatedAtDesc(@Param("musicUserId") Long musicUserId);

    @Select("SELECT * FROM agent_conversation WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT 20")
    List<AgentConversation> findTop20ByUser_IdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Insert("INSERT INTO agent_conversation(user_id, role, content, created_at) " +
            "VALUES(#{user.id}, #{role}, #{content}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(AgentConversation conversation);
}
