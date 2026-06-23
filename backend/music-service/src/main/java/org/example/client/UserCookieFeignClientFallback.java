package org.example.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * User Cookie Feign Client Fallback
 * 当 user-service 不可用时的降级处理
 */
@Component
public class UserCookieFeignClientFallback implements UserCookieFeignClient {

    private static final Logger log = LoggerFactory.getLogger(UserCookieFeignClientFallback.class);

    @Override
    public String getCookie(Long musicUserId) {
        log.warn("User service is unavailable, fallback triggered for getCookie: musicUserId={}", musicUserId);
        return null;
    }

    @Override
    public Boolean userExists(Long musicUserId) {
        log.warn("User service is unavailable, fallback triggered for userExists: musicUserId={}", musicUserId);
        return false;
    }
}
