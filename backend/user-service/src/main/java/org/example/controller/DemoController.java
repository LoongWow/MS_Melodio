package org.example.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/demo")
@RefreshScope
public class DemoController {

    @Value("${demo.message:这是默认的消息(如果在配置文件里没找到配置的话)}")
    private String demoMessage;

    @GetMapping("/message")
    public String getMessage() {
        return "当前配置中心的 demo.message 值为: " + demoMessage;
    }
}
