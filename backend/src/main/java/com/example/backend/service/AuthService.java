package com.example.backend.service;

import com.example.backend.model.dto.AuthResponse;
import com.example.backend.model.dto.LoginRequest;
import com.example.backend.model.dto.SignupRequest;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
}

