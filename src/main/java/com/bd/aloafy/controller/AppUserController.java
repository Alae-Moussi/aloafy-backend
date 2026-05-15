package com.bd.aloafy.controller;

import com.bd.aloafy.dto.request.AppUserRequest;
import com.bd.aloafy.dto.response.AppUserResponse;
import com.bd.aloafy.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
@RestController
@RequestMapping("/api/appUser")
public class AppUserController {

    @Autowired
    private AppUserService appUserService;

    @GetMapping("/getUserProfile")
    public ResponseEntity<AppUserResponse> getUserProfile(Authentication authentication) {
        String email = authentication.getName();

        AppUserResponse response = appUserService.getUserProfile(email);
        return ResponseEntity.ok(response);
    }
    @PutMapping("/updateUserProfile")
    public ResponseEntity<AppUserResponse> updateUserProfile(@Valid @RequestBody AppUserRequest request,
                                                             Authentication authentication) {
        String email = authentication.getName();

        AppUserResponse response = appUserService.updateUserProfile(request, email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getAllUsers")
    public ResponseEntity<?> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         Authentication authentication) {

        String email = authentication.getName();
        return ResponseEntity.ok(appUserService.getAllUsers(page, size));
    }
    @PatchMapping("/updateUserRole/{userId}")
    public ResponseEntity<AppUserResponse> updateUserRole(
            @PathVariable Long userId,
            @RequestParam @NotBlank(message = "Role is requried") @Pattern(regexp = "^(USER|ADMIN)$",
                    message = "Role must be either USER or ADMIN") String role,
            Authentication authentication) {

        String email = authentication.getName();

        AppUserResponse response = appUserService.updateUserRole(userId, role, email);
        return ResponseEntity.ok(response);
    }

}

