package com.LinkedKnowledge.service;

import com.LinkedKnowledge.entity.User;

public interface UserService {
    User register(String username, String password, String email);

    User login(String username, String password);

    User findById(Long id);
}