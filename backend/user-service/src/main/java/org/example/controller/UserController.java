package org.example.controller;

import javax.validation.Valid;
import org.example.dto.*;
import org.example.service.AuthService;
import org.example.service.CaptchaLoginService;
import org.example.service.NeteaseCookieService;
import org.example.service.QrLoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;
    private final NeteaseCookieService neteaseCookieService;
    private final CaptchaLoginService captchaLoginService;
    private final QrLoginService qrLoginService;

    public UserController(AuthService authService,
                         NeteaseCookieService neteaseCookieService,
                         CaptchaLoginService captchaLoginService,
                         QrLoginService qrLoginService) {
        this.authService = authService;
        this.neteaseCookieService = neteaseCookieService;
        this.captchaLoginService = captchaLoginService;
        this.qrLoginService = qrLoginService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.getAccount(), request.getPassword(), request.getCookie());
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.getAccount(), request.getPassword(), request.getCookie());
    }

    @PostMapping("/netease-login")
    public ResponseEntity<NeteaseCookieResponse> neteaseLogin(@Valid @RequestBody NeteaseLoginRequest request) {
        NeteaseCookieResponse response;

        if ("phone".equals(request.getLoginType())) {
            response = neteaseCookieService.loginWithPhone(request.getAccount(), request.getPassword());
        } else if ("email".equals(request.getLoginType())) {
            response = neteaseCookieService.loginWithEmail(request.getAccount(), request.getPassword());
        } else {
            return ResponseEntity.badRequest()
                .body(NeteaseCookieResponse.failure("无效的登录类型"));
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/cookie-status")
    public ResponseEntity<CookieStatusResponse> checkCookieStatus(
            @RequestParam(required = true) String cookie) {

        if (cookie == null || cookie.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        boolean valid = neteaseCookieService.validateCookie(cookie);
        return ResponseEntity.ok(new CookieStatusResponse(valid));
    }

    @PostMapping("/send-captcha")
    public ResponseEntity<CaptchaResponse> sendCaptcha(
            @RequestParam(required = true) String phone) {

        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        CaptchaResponse response = captchaLoginService.sendCaptcha(phone);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/captcha-login")
    public ResponseEntity<NeteaseCookieResponse> captchaLogin(
            @Valid @RequestBody CaptchaLoginRequest request) {

        NeteaseCookieResponse response = captchaLoginService.loginWithCaptcha(
            request.getPhone(),
            request.getCaptcha()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/qr-key")
    public ResponseEntity<QrKeyResponse> generateQrKey() {
        QrKeyResponse response = qrLoginService.generateQrKey();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/qr-create")
    public ResponseEntity<QrImageResponse> createQrImage(
            @RequestParam(required = true) String key) {

        if (key == null || key.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        QrImageResponse response = qrLoginService.createQrImage(key);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/qr-check")
    public ResponseEntity<QrStatusResponse> checkQrStatus(
            @RequestParam(required = true) String key) {

        if (key == null || key.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        QrStatusResponse response = qrLoginService.checkQrStatus(key);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/qr-login")
    public ResponseEntity<LoginResponse> completeQrLogin(
            @RequestParam(required = true) String cookie) {

        if (cookie == null || cookie.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(null, "Cookie 不能为空"));
        }

        LoginResponse response = qrLoginService.completeQrLogin(cookie);

        if (response.getUserId() == null) {
            return ResponseEntity.status(400).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 供其他微服务调用：根据 musicUserId 获取用户的 Cookie
     */
    @GetMapping("/cookie/{musicUserId}")
    public ResponseEntity<String> getCookie(@PathVariable Long musicUserId) {
        String cookie = authService.getCookieByMusicUserId(musicUserId);
        if (cookie == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cookie);
    }

    /**
     * 供其他微服务调用：检查用户是否存在
     */
    @GetMapping("/exists/{musicUserId}")
    public ResponseEntity<Boolean> userExists(@PathVariable Long musicUserId) {
        boolean exists = authService.userExists(musicUserId);
        return ResponseEntity.ok(exists);
    }
}
