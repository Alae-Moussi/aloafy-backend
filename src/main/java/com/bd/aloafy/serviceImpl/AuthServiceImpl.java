package com.bd.aloafy.serviceImpl;

import com.bd.aloafy.dto.request.ForgotPasswordRequest;
import com.bd.aloafy.dto.request.LoginUserRequest;
import com.bd.aloafy.dto.request.RefreshTokenRequest;
import com.bd.aloafy.dto.request.RegisterUserRequest;
import com.bd.aloafy.dto.response.AppUserResponse;
import com.bd.aloafy.dto.response.MessageResponse;
import com.bd.aloafy.entity.AppUser;
import com.bd.aloafy.exception.EmailAlreadyExistsException;
import com.bd.aloafy.exception.InvalidCredentialsException;
import com.bd.aloafy.exception.InvalidTokenException;
import com.bd.aloafy.exception.ResourceNotFoundException;
import com.bd.aloafy.repository.AppUserRepository;
import com.bd.aloafy.service.AuthService;
import com.bd.aloafy.service.EmailService;
import com.bd.aloafy.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Override
    public MessageResponse registerUser(RegisterUserRequest request) {
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        String tempPassword = generateTemporaryPassword();

        AppUser appUser = new AppUser();
        appUser.setName(request.getName());
        appUser.setEmail(request.getEmail());
        appUser.setPassword(passwordEncoder.encode(tempPassword));

        // CORRIGÉ : Attribution sécurisée du rôle par défaut
        appUser.setRole("USER");

        // Sauvegarde impérative en Base de Données
        appUserRepository.save(appUser);

        // Bloc de sécurité pour l'envoi de mail SMTP
        try {
            emailService.sendWelcomeEmail(appUser.getEmail(), appUser.getName(), tempPassword);
        } catch (Exception e) {
            System.err.println("============== ALOAFY SECURE MAIL SYSTEM ==============");
            System.err.println("❌ L'envoi du mail de bienvenue a échoué ! Vérifie ton SMTP.");
            System.err.println("🔑 MOT DE PASSE DE SECOURS GÉNÉRÉ POUR " + appUser.getEmail() + " : " + tempPassword);
            System.err.println("Erreur exacte : " + e.getMessage());
            System.err.println("=======================================================");
        }

        return new MessageResponse("Account created successfully. A temporary password has been sent to your email");
    }

    @Override
    public AppUserResponse loginUser(LoginUserRequest request) {
        AppUser appUser = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), appUser.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(appUser.getId(), appUser.getName(), appUser.getEmail(), appUser.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(appUser.getId(), appUser.getEmail());

        appUser.setRefreshToken(refreshToken);
        appUserRepository.save(appUser);

        return AppUserResponse.fromEntity(appUser, accessToken, refreshToken);
    }

    @Override
    public AppUserResponse refreshAccessToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String email = jwtUtil.extractEmail(refreshToken);

        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid token type");
        }

        AppUser appUser = appUserRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid Refresh token "));

        if (!jwtUtil.validateToken(refreshToken, email)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
        String newAccessToken = jwtUtil.generateAccessToken(appUser.getId(), appUser.getName(), appUser.getEmail(), appUser.getRole());

        return AppUserResponse.fromEntity(appUser, newAccessToken, refreshToken);
    }

    @Override
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        AppUser appUser = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        String tempPassword = generateTemporaryPassword();

        appUser.setPassword(passwordEncoder.encode(tempPassword));
        appUserRepository.save(appUser);

        try {
            emailService.sendCredentialsEmail(appUser.getEmail(), appUser.getName(), tempPassword);
        } catch (Exception e) {
            System.err.println("============== ALOAFY SECURE MAIL SYSTEM ==============");
            System.err.println("❌ L'envoi du mail de récupération a échoué !");
            System.err.println("🔑 NOUVEAU MOT DE PASSE DE SECOURS GÉNÉRÉ POUR " + appUser.getEmail() + " : " + tempPassword);
            System.err.println("=======================================================");
        }

        return new MessageResponse("Temporary password has been sent to your email");
    }

    private String generateTemporaryPassword() {
        // Alphanumérique pur pour éviter les conflits de caractères spéciaux dans le template HTML du mail
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(10);

        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}