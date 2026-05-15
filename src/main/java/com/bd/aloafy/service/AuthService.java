package com.bd.aloafy.service;

import com.bd.aloafy.dto.request.ForgotPasswordRequest;
import com.bd.aloafy.dto.request.RefreshTokenRequest;
import com.bd.aloafy.dto.request.RegisterUserRequest;
import com.bd.aloafy.dto.response.MessageResponse;
import com.bd.aloafy.dto.request.LoginUserRequest;
import com.bd.aloafy.dto.response.AppUserResponse;
import jakarta.validation.Valid;

public interface AuthService {

    MessageResponse registerUser(RegisterUserRequest request);

    AppUserResponse loginUser(LoginUserRequest request);

    AppUserResponse refreshAccessToken(RefreshTokenRequest request);

    MessageResponse forgotPassword(ForgotPasswordRequest request);
}
