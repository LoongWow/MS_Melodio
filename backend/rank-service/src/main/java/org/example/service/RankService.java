package org.example.service;

import org.example.entity.RankItem;
import org.example.repo.RankRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RankService {

    @Autowired
    private RankRepository rankRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "rank:";
    private static final long CACHE_EXPIRE_MINUTES = 5; // 缓存5分钟

    /**
     * 获取排行榜数据（带缓存）
     */
    @SuppressWarnings("unchecked")
    public List<RankItem> getRankList(String type) {
        String cacheKey = CACHE_PREFIX + type;

        // 尝试从缓存获取
        List<RankItem> cachedData = (List<RankItem>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null && !cachedData.isEmpty()) {
            return cachedData;
        }

        // 缓存未命中，从数据库查询
        List<RankItem> rankList;
        switch (type) {
            case "hot":
                rankList = rankRepository.getHotRank();
                break;
            case "new":
                rankList = rankRepository.getNewRank();
                break;
            case "rising":
                rankList = rankRepository.getRisingRank();
                break;
            default:
                rankList = rankRepository.getHotRank();
        }

        // 存入缓存
        if (rankList != null && !rankList.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, rankList, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }

        return rankList;
    }

    /**
     * 清除排行榜缓存
     */
    public void clearCache(String type) {
        String cacheKey = CACHE_PREFIX + type;
        redisTemplate.delete(cacheKey);
    }

    /**
     * 清除所有排行榜缓存
     */
    public void clearAllCache() {
        redisTemplate.delete(CACHE_PREFIX + "hot");
        redisTemplate.delete(CACHE_PREFIX + "new");
        redisTemplate.delete(CACHE_PREFIX + "rising");
    }
}
