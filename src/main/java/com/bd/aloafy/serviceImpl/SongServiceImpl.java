package com.bd.aloafy.serviceImpl;

import com.bd.aloafy.dto.request.SongRequest;
import com.bd.aloafy.dto.response.SongResponse;
import com.bd.aloafy.entity.AppUser;
import com.bd.aloafy.entity.Song;
import com.bd.aloafy.repository.AppUserRepository;
import com.bd.aloafy.repository.PlaylistSongRepository;
import com.bd.aloafy.repository.SongRepository;
import com.bd.aloafy.service.SongService;
import com.bd.aloafy.util.FileHandlerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class SongServiceImpl implements SongService {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PlaylistSongRepository playlistSongRepository;

    @Autowired
    private FileHandlerUtil fileHandlerUtil;

    @Value("${app.base.url}")
    private String baseUrl;

    @Override
    public SongResponse addSong(SongRequest request, MultipartFile songFile, MultipartFile imageFile, String email) {
        // Récupération de l'utilisateur
        AppUser appUser = getUserByEmail(email);
        String uniqueId = UUID.randomUUID().toString();

        // Création de l'entité Song
        Song song = new Song();
        song.setAppUser(appUser);
        updateSongMetadata(song, request);

        // Traitement du fichier Audio
        String songUrl = processSongFile(songFile, uniqueId);
        song.setSongUrl(songUrl);

        // Traitement du fichier Image
        String imageUrl = processImageFile(imageFile, uniqueId);
        song.setImageUrl(imageUrl);

        // Sauvegarde en base de données
        Song savedSong = songRepository.save(song);

        // Retourne la réponse mappée
        return SongResponse.fromEntity(savedSong, baseUrl);
    }

    // Méthode pour uploader le fichier audio
    private String processSongFile(MultipartFile songFile, String uniqueId) {
        String songExtension = fileHandlerUtil.getFileExtension(songFile.getOriginalFilename());
        String songFilename = uniqueId + songExtension;
        fileHandlerUtil.saveSongFileWithName(songFile, songFilename);
        return "/api/file/song/" + songFilename;
    }

    // Méthode pour uploader l'image de couverture
    private String processImageFile(MultipartFile imageFile, String uniqueId) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        String imageExtension = fileHandlerUtil.getFileExtension(imageFile.getOriginalFilename());
        String imageFilename = uniqueId + imageExtension;
        fileHandlerUtil.saveImageFileWithName(imageFile, imageFilename);
        return "/api/file/image/" + imageFilename;
    }

    // Mise à jour du titre et de l'artiste
    private void updateSongMetadata(Song song, SongRequest request) {
        song.setTitle(request.getTitle());
        song.setArtist(request.getArtist());
    }

    // Recherche de l'utilisateur
    private AppUser getUserByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}