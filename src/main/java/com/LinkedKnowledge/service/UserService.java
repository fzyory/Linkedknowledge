package com.LinkedKnowledge.service;

import com.LinkedKnowledge.entity.User;

public interface UserService {
    User register(String username, String password, String email);

    User login(String username, String password);

    User loginOrRegisterByPhone(String phone);

    User findById(Long id);

    void deleteAccount(Long userId, String password);

    void changePassword(Long userId, String oldPassword, String newPassword);
}