package com.LinkedKnowledge.controller;
import com.LinkedKnowledge.common.JwtUtil;
import com.LinkedKnowledge.common.Result;
import com.LinkedKnowledge.entity.User;
import com.LinkedKnowledge.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController{
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    // POST /api/auth/register
    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request) {
        User user = userService.register(
                request.getUsername(),
                request.getPassword(),
                request.getEmail()
        );
        user.setPassword(null); // 返回时不带密码
        return Result.success(user);
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginRequest request,HttpServletResponse response) {
        User user = userService.login(
                request.getUsername(),
                request.getPassword()
        );
        user.setPassword(null);
        Cookie c=new Cookie("token",jwtUtil.generateToken(user.getId(),user.getUsername()));
        c.setHttpOnly(true);c.setSecure(true);c.setPath("/");
        response.addCookie(c);
        return Result.success(user);
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
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
        return Result.success(null);
    }
    // GET /api/auth/me
    @GetMapping("/me")
    public Result<void> me(HttpServletRequest request,HttpServletResponse response){
        Cookie cookie =request.getCookies();
        String userid= jwtUtil.parseToken(cookie);

    }
}

// 放在同一个文件里?/
class RegisterRequest {
    private String username;
    private String password;
    private String email;
    // getter/setter
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
