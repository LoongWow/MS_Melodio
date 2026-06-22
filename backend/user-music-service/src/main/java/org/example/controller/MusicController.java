package org.example.controller;

import javax.validation.Valid;
import org.example.dto.*;
import org.example.entity.PlayHistory;
import org.example.service.AuthService;
import org.example.service.CaptchaLoginService;
import org.example.service.MusicApiService;
import org.example.service.NeteaseCookieService;
import org.example.service.PlayHistoryService;
import org.example.service.QrLoginService;
import org.example.service.UserPreferenceService;
import org.example.tools.MusicTools;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/music")
public class MusicController {

    private final MusicApiService musicApiService;
    private final MusicTools musicTools;
    private final AuthService authService;
    private final PlayHistoryService playHistoryService;
    private final UserPreferenceService userPreferenceService;
    private final NeteaseCookieService neteaseCookieService;
    private final CaptchaLoginService captchaLoginService;
    private final QrLoginService qrLoginService;

    public MusicController(MusicApiService musicApiService,
                          MusicTools musicTools,
                          AuthService authService,
                          PlayHistoryService playHistoryService,
                          UserPreferenceService userPreferenceService,
                          NeteaseCookieService neteaseCookieService,
                          CaptchaLoginService captchaLoginService,
                          QrLoginService qrLoginService) {
        this.musicApiService = musicApiService;
        this.musicTools = musicTools;
        this.authService = authService;
        this.playHistoryService = playHistoryService;
        this.userPreferenceService = userPreferenceService;
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

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String keywords, @RequestParam Long userId) {
        List<MusicSongDto> songs = musicTools.searchSongs(keywords, userId);
        return new SearchResponse(songs);
    }

    @GetMapping("/play-url")
    public PlayUrlResponse playUrl(@RequestParam Long songId, @RequestParam Long userId) {
        return new PlayUrlResponse(songId, musicTools.getPlayUrl(songId, userId));
    }

    @GetMapping("/cache-song-id")
    public Long cacheSongId(@RequestParam int index, @RequestParam Long userId) {
        return musicTools.getSongFromCache(index, userId);
    }

    @GetMapping("/lyric/new")
    public ResponseEntity<?> lyric(@RequestParam Long id) {
        return ResponseEntity.ok(musicApiService.getLyric(id));
    }

    @PostMapping("/play-history")
    public ResponseEntity<?> recordPlayHistory(@Valid @RequestBody PlayHistoryRequest request) {
        playHistoryService.recordPlay(
            request.getUserId(),
            request.getSongId(),
            request.getSongName(),
            request.getArtist(),
            request.getDuration() != null ? request.getDuration().intValue() : 0,
            request.getCompleted() != null ? request.getCompleted() : false
        );
        userPreferenceService.updatePreference(request.getUserId(), request.getArtist());

        return ResponseEntity.ok(Map.of("success", true, "message", "播放历史已记录"));
    }

    @GetMapping("/user-preference")
    public ResponseEntity<?> getUserPreference(@RequestParam Long userId) {
        Map<String, Object> analysis = userPreferenceService.analyzeUserPreference(userId);
        return ResponseEntity.ok(analysis);
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

    @GetMapping("/play-history")
    public ResponseEntity<List<PlayHistory>> getPlayHistory(@RequestParam Long userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        List<PlayHistory> history = playHistoryService.getUniqueRecentHistory(userId);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/play-history")
    public ResponseEntity<?> clearPlayHistory(@RequestParam Long userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        playHistoryService.clearHistory(userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "播放历史已清空"));
    }

}
