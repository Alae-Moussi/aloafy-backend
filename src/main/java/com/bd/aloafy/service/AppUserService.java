package com.bd.aloafy.service;

import com.bd.aloafy.dto.request.AppUserRequest;
import com.bd.aloafy.dto.response.AppUserResponse;
import com.bd.aloafy.dto.response.PaginatedResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public interface AppUserService {
    AppUserResponse getUserProfile(String email);

    AppUserResponse updateUserProfile(AppUserRequest request, String email);

    PaginatedResponse<AppUserResponse> getAllUsers(int page, int size);

    AppUserResponse updateUserRole(Long userId,  String role, String email);
}
