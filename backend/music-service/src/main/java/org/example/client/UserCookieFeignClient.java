package org.example.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * User Service Feign Client
 * 用于从 user-service 获取用户 Cookie 信息
 */
@FeignClient(name = "user-service", fallback = UserCookieFeignClientFallback.class)
public interface UserCookieFeignClient {

    /**
     * 根据 musicUserId 获取用户的 Cookie
     */
    @GetMapping("/api/user/cookie/{musicUserId}")
    String getCookie(@PathVariable("musicUserId") Long musicUserId);

    /**
     * 检查用户是否存在
     */
    @GetMapping("/api/user/exists/{musicUserId}")
    Boolean userExists(@PathVariable("musicUserId") Long musicUserId);
}
