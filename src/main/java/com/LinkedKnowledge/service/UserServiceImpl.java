package com.LinkedKnowledge.service;

import com.LinkedKnowledge.entity.User;
import com.LinkedKnowledge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public User register(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail((email == null || email.isBlank()) ? null : email);
        return userRepository.save(user);
    }

    @Override
    public User login(String username, String password) {
        String failKey = "login:fail:" + username;
        String lockKey = "login:lock:" + username;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new RuntimeException("尝试次数过多，请 15 分钟后再试");
        }
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            Long fails = redisTemplate.opsForValue().increment(failKey);
            if (fails != null && fails == 1) {
                redisTemplate.expire(failKey, Duration.ofMinutes(15));
            }
            if (fails != null && fails >= 5) {
                redisTemplate.opsForValue().set(lockKey, "1", Duration.ofMinutes(15));
                redisTemplate.delete(failKey);
            }
            throw new RuntimeException("用户名或密码错误");
        }
        redisTemplate.delete(failKey);
        redisTemplate.delete(lockKey);
        return user;
    }

    @Override
    public User loginOrRegisterByPhone(String phone) {
        return userRepository.findByPhone(phone).orElseGet(() -> {
            User existing = userRepository.findByUsername(phone).orElse(null);
            if (existing != null) {
                if (existing.getPhone() == null) {
                    existing.setPhone(phone);
                    return userRepository.save(existing);
                }
                throw new RuntimeException("该手机号无法登录");
            }
            User user = new User();
            user.setUsername(phone);
            user.setPhone(phone);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            return userRepository.save(user);
        });
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteAccount(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        // 必须校验密码才能销号,防止 token 被偷后别人直接销你号
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        userRepository.delete(user);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}