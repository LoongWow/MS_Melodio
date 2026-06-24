package org.example.controller;

import org.example.entity.RankItem;
import org.example.service.RankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/rank")
@CrossOrigin(origins = "*")
public class RankController {

    @Autowired
    private RankService rankService;

    /**
     * 获取排行榜列表
     * @param type 榜单类型：hot-热歌榜, new-新歌榜, rising-飙升榜
     */
    @GetMapping("/list")
    public ResponseEntity<List<RankItem>> getRankList(@RequestParam(defaultValue = "hot") String type) {
        try {
            List<RankItem> rankList = rankService.getRankList(type);
            return ResponseEntity.ok(rankList != null ? rankList : Collections.emptyList());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    /**
     * 清除排行榜缓存（管理员接口）
     */
    @DeleteMapping("/cache")
    public ResponseEntity<String> clearCache(@RequestParam(required = false) String type) {
        try {
            if (type != null && !type.isEmpty()) {
                rankService.clearCache(type);
            } else {
                rankService.clearAllCache();
            }
            return ResponseEntity.ok("缓存清除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("缓存清除失败");
        }
    }
}
