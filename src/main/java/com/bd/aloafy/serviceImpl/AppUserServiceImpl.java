package com.bd.aloafy.serviceImpl;
import com.bd.aloafy.dto.request.AppUserRequest;
import com.bd.aloafy.dto.response.AppUserResponse;
import com.bd.aloafy.dto.response.PaginatedResponse;
import com.bd.aloafy.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.bd.aloafy.repository.AppUserRepository;
import com.bd.aloafy.service.AppUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppUserServiceImpl implements AppUserService {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public AppUserResponse getUserProfile(String email) {
        AppUser appUser = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return AppUserResponse.fromEntity(appUser, null, null);
    }

    @Override
    public AppUserResponse updateUserProfile(AppUserRequest request, String email) {
        AppUser appUser = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            appUser.setName(request.getName().trim());
        }
        // 3. Gestion du changement de mot de passe
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            // Vérifier si l'ancien mot de passe est fourni
            if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
                throw new RuntimeException("Old password is required to update password");
            }
            // Vérifier si l'ancien mot de passe correspond à celui en base (hashé)
            if (!passwordEncoder.matches(request.getOldPassword(), appUser.getPassword())) {
                throw new RuntimeException("Old password is incorrect");
            }
            // Encoder et enregistrer le nouveau mot de passe
            appUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        // 4. Sauvegarder les modifications
        AppUser updatedUser = appUserRepository.save(appUser);
        // 5. Retourner la réponse sans nouveaux tokens (null)
        return AppUserResponse.fromEntity(updatedUser, null, null);
    }

    @Override
    public PaginatedResponse<AppUserResponse> getAllUsers(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        // 2. Récupération de la page d'utilisateurs depuis le repository
        Page<AppUser> userPage = appUserRepository.findAll(pageable);

        // 3. Transformation de la liste d'entités (AppUser) en liste de DTOs (AppUserResponse)
        List<AppUserResponse> userResponses = userPage.getContent().stream()
                .map(user -> AppUserResponse.fromEntity(user, null, null))
                .collect(Collectors.toList());

        // 4. Construction et retour de la réponse paginée avec toutes les métadonnées
        return new PaginatedResponse<>(
                userResponses,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast(),
                userPage.isFirst()
        );
    }

    @Override
    public AppUserResponse updateUserRole(Long userId, String role, String email) {
        // 1. On récupère l'utilisateur qui fait la requête (l'admin)
        AppUser adminUser = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Vérification de sécurité : Seul un ADMIN peut changer les rôles
        if (!"ADMIN".equals(adminUser.getRole())) {
            throw new RuntimeException("Only admin can update user roles");
        }

        // 3. On récupère l'utilisateur à modifier par son ID
        AppUser userToUpdate = appUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 4. Normalisation et mise à jour du rôle
        String normalizedRole = role.trim().toUpperCase();
        userToUpdate.setRole(normalizedRole);

        // 5. Sauvegarde en base de données
        AppUser updatedUser = appUserRepository.save(userToUpdate);

        // 6. Retour de la réponse
        return AppUserResponse.fromEntity(updatedUser, null, null);
    }
}
