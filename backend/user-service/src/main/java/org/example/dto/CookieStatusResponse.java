package org.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


/**
 * Cookie 状态响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CookieStatusResponse {
    private boolean valid;
}

