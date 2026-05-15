package com.bd.aloafy.controller;

import com.bd.aloafy.dto.request.ForgotPasswordRequest;
import com.bd.aloafy.dto.request.RefreshTokenRequest;
import com.bd.aloafy.dto.request.RegisterUserRequest;
import com.bd.aloafy.dto.request.LoginUserRequest;
import com.bd.aloafy.dto.response.AppUserResponse;
import com.bd.aloafy.dto.response.MessageResponse;
import com.bd.aloafy.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/registerUser")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        MessageResponse response = authService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/loginUser")
    public ResponseEntity<AppUserResponse> loginUser(@Valid @RequestBody LoginUserRequest request) {
        AppUserResponse response = authService.loginUser(request);
        return ResponseEntity.ok(response);
}
    @PostMapping("/refreshAccessToken")
    public ResponseEntity<AppUserResponse> refreshAccessToken(@Valid @RequestBody RefreshTokenRequest request) {
        AppUserResponse response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        MessageResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }


}
