package com.LinkedKnowledge.controller;
import com.LinkedKnowledge.common.JwtUtil;
import com.LinkedKnowledge.common.Result;
import com.LinkedKnowledge.config.AuthCookieSupport;
import com.LinkedKnowledge.entity.User;
import com.LinkedKnowledge.service.SmsService;
import com.LinkedKnowledge.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.redis.core.RedisTemplate;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final AuthCookieSupport authCookieSupport;
    private final SmsService smsService;

    // POST /api/auth/register
    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return Result.error(400, "请填写用户名");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return Result.error(400, "密码至少 6 位");
        }
        User user = userService.register(
                request.getUsername().trim(),
                request.getPassword(),
                request.getEmail()
        );
        user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return Result.error(400, "请填写用户名和密码");
        }
        User user = userService.login(
                request.getUsername().trim(),
                request.getPassword()
        );
        return issueSession(user, response);
    }

    @PostMapping("/sms/send")
    public Result<Map<String, Object>> sendSms(@RequestBody SmsSendRequest request, HttpServletRequest httpRequest) {
        SmsService.SendResult sent = smsService.sendCode(request.getPhone(), clientIp(httpRequest));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cooldownSeconds", sent.cooldownSeconds());
        if (sent.devCode() != null) {
            data.put("devCode", sent.devCode());
            data.put("devHint", "未配置短信网关，验证码仅在本地回显");
        }
        return Result.success(data);
    }

    @PostMapping("/sms/login")
    public Result<User> smsLogin(@RequestBody SmsLoginRequest request, HttpServletResponse response) {
        smsService.verifyOrThrow(request.getPhone(), request.getCode());
        User user = userService.loginOrRegisterByPhone(smsService.normalizePhone(request.getPhone()));
        return issueSession(user, response);
    }

    // POST /api/auth/logout
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    long seconds = (jwtUtil.getExpiration(cookie.getValue()).getTime() - System.currentTimeMillis()) / 1000;
                    redisTemplate.opsForValue().set("blacklist:" + cookie.getValue(), "1", Duration.ofSeconds(seconds));
                    authCookieSupport.clear(response);
                }
            }
        }
        return Result.success();
    }

    // GET /api/auth/me —— 当前登录用户信息
    @GetMapping("/me")
    public Result<User> me(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || !jwtUtil.validateToken(token)) {
            return Result.error(401, "未登录或 token 无效");
        }
        Long userId = jwtUtil.getUserId(token);
        User user = userService.findById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    // DELETE /api/auth/delete-account —— 销号(删 user)
    @DeleteMapping("/delete-account")
    public Result<Void> deleteAccount(@RequestBody PasswordRequest request,
                                      HttpServletRequest httpRequest,
                                      HttpServletResponse response) {
        String token = extractToken(httpRequest);
        if (token == null || !jwtUtil.validateToken(token)) {
            return Result.error(401, "未登录或 token 无效");
        }
        Long userId = jwtUtil.getUserId(token);
        userService.deleteAccount(userId, request.getPassword());
        // 销号成功 → 当前 token 进黑名单,Cookie 清掉
        blacklistToken(response, token);
        return Result.success();
    }

    // PUT /api/auth/change-password —— 改密码
    @PutMapping("/change-password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request,
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse response) {
        String token = extractToken(httpRequest);
        if (token == null || !jwtUtil.validateToken(token)) {
            return Result.error(401, "未登录或 token 无效");
        }
        Long userId = jwtUtil.getUserId(token);
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        // 改密码成功 → 当前 token 进黑名单,强制重新登录
        blacklistToken(response, token);
        return Result.success();
    }

    // ===== 工具方法 =====

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void blacklistToken(HttpServletResponse response, String token) {
        long seconds = (jwtUtil.getExpiration(token).getTime() - System.currentTimeMillis()) / 1000;
        if (seconds > 0) {
            redisTemplate.opsForValue().set("blacklist:" + token, "1", Duration.ofSeconds(seconds));
        }
        authCookieSupport.clear(response);
    }

    private Result<User> issueSession(User user, HttpServletResponse response) {
        user.setPassword(null);
        authCookieSupport.setToken(response, jwtUtil.generateToken(user.getId(), user.getUsername()));
        return Result.success(user);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

// 放在同一个文件里
class RegisterRequest {
    private String username;
    private String password;
    private String email;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

class LoginRequest {
    private String username;
    private String password;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class SmsSendRequest {
    private String phone;
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}

class SmsLoginRequest {
    private String phone;
    private String code;
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}

class PasswordRequest {
    private String password;
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;
    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}