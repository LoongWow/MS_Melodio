package org.example.repo;

import org.apache.ibatis.annotations.*;
import org.example.entity.MusicKnowledge;
import java.util.List;

@Mapper
public interface MusicKnowledgeMapper {

    @Select("SELECT * FROM music_knowledge")
    List<MusicKnowledge> findAll();

    @Insert("INSERT INTO music_knowledge(title, content, knowledge_type, related_keywords, created_at, updated_at) " +
            "VALUES(#{title}, #{content}, #{knowledgeType}, #{relatedKeywords}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void save(MusicKnowledge knowledge);
}
