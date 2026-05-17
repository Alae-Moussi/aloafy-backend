package com.bd.aloafy.serviceImpl;

import com.bd.aloafy.dto.request.SongRequest;
import com.bd.aloafy.dto.response.MessageResponse;
import com.bd.aloafy.dto.response.PaginatedResponse;
import com.bd.aloafy.dto.response.SongAiInsightsResponse;
import com.bd.aloafy.dto.response.SongResponse;
import com.bd.aloafy.entity.AppUser;
import com.bd.aloafy.entity.Song;
import com.bd.aloafy.repository.AppUserRepository;
import com.bd.aloafy.repository.PlaylistSongRepository;
import com.bd.aloafy.repository.SongRepository;
import com.bd.aloafy.service.GenericGeminiService;
import com.bd.aloafy.service.SongService;
import com.bd.aloafy.util.FileHandlerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Autowired
    private GenericGeminiService geminiService;

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

    @Override
    public Object getAllSongs(Long userId, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Song> songPage;

        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasUserId = userId != null;

        if (hasUserId && hasSearch) {

            songPage = songRepository.findByAppUserIdAndTitleContainingIgnoreCaseOrAppUserIdAndArtistContainingIgnoreCase(
                    userId, search.trim(), userId, search.trim(), pageable);
        } else if (hasSearch) {
            songPage = songRepository.findByTitleContainingIgnoreCaseOrArtistContainingIgnoreCase(
                    search.trim(), search.trim(), pageable);
        } else if (hasUserId) {
            songPage = songRepository.findByAppUserId(userId, pageable);
        } else {
            songPage = songRepository.findAll(pageable);
        }

        List<SongResponse> songResponses = songPage.getContent().stream()
                .map(song -> SongResponse.fromEntity(song, baseUrl))
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                songResponses,
                songPage.getNumber(),
                songPage.getSize(),
                songPage.getTotalElements(),
                songPage.getTotalPages(),
                songPage.isLast(),
                songPage.isFirst()
        );

    }
    @Override
    public SongResponse getSongById(Long id) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found"));
        return SongResponse.fromEntity(song, baseUrl);
    }

    @Override
    public SongResponse updateSong(Long id, SongRequest request, MultipartFile songFile, MultipartFile imageFile, String email) {
        // 1. Validation des accès (propriétaire ou admin)
        Song song = validateSongAccess(id, email);

        // 2. Mise à jour du titre et de l'artiste
        updateSongMetadata(song, request);

        // 3. Si un nouveau fichier audio est fourni, on supprime l'ancien du stockage informatique
        if (songFile != null && !songFile.isEmpty()) {
            deleteOldSongFile(song.getSongUrl());
            String uniqueId = UUID.randomUUID().toString();
            String songUrl = processSongFile(songFile, uniqueId);
            song.setSongUrl(songUrl);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            deleteOldImageFile(song.getImageUrl());
            String uniqueId = UUID.randomUUID().toString();
            String imageUrl = processImageFile(imageFile, uniqueId);
            song.setImageUrl(imageUrl);
        }

        Song updatedSong = songRepository.save(song);

        return SongResponse.fromEntity(updatedSong, baseUrl);
    }

    @Override
    public MessageResponse deleteSong(Long id, String email) {
        Song song = validateSongAccess(id, email);
        playlistSongRepository.deleteBySongId(id);
        deleteSongFiles(song);
        songRepository.delete(song);
        return new MessageResponse("Song deleted successfully");
    }

    @Override
    public SongAiInsightsResponse getSongAiInsights(Long songId) {

            // 1. Recherche de la chanson dans la base de données, lève une erreur si elle n'existe pas
            Song song = songRepository.findById(songId)
                    .orElseThrow(() -> new RuntimeException("Song not found"));

            // 2. Construction du prompt textuel à envoyer à l'IA Gemini
            String prompt =buildSongAnalysisPrompt(song);
        return geminiService.generateContent(prompt, SongAiInsightsResponse.class);
    }

    private String buildSongAnalysisPrompt(Song song) {
        return String.format("""
        Analyze the song '%s' by '%s' and provide detailed insights in JSON format.
        
        Return a JSON object with the following structure:
        {
          "analysis": "A detailed 2-3 sentence analysis of the track's musical characteristics, production quality, and emotional impact",
          "moods": ["List", "of", "4-6", "mood", "keywords"],
          "genre": "Primary genre classification",
          "tempo": 120,
          "key": "Musical key (e.g., C Major, D Minor)",
          "energy": 7,
          "similarArtists": ["List", "of", "4-6", "similar", "artists"],
          "recommendedFor": "A 1-2 sentence recommendation about when and where to listen to this song"
        }
        
        Important:
        - The 'tempo' should be an estimated BPM (beats per minute) between 60-200
        - The 'energy' should be a rating from 1-10
        - Base your analysis on the artist's typical style and the song title
        - Be creative but realistic
        - Return ONLY the JSON object, no additional text
        """,
                song.getTitle(),
                song.getArtist()
        );
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

    private Song validateSongAccess(Long id, String email) {
        // 1. Recherche de la chanson ou exception si elle n'existe pas
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found"));

        // 2. Récupération de l'utilisateur connecté via son email
        AppUser appUser = getUserByEmail(email);

        // 3. Vérifications des privilèges (Propriétaire ou Administrateur)
        boolean isOwner = song.getAppUser().getId().equals(appUser.getId());
        boolean isAdmin = "ADMIN".equals(appUser.getRole());

        // 4. Blocage si l'utilisateur n'est ni l'un ni l'autre
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You don't have permission to modify this song");
        }

        // 5. Retourne la chanson validée
        return song;
    }

    private void deleteOldSongFile(String songUrl) {
        if (songUrl != null) {
            String oldSongFilename = fileHandlerUtil.extractFilename(songUrl);
            if (oldSongFilename != null) {
                fileHandlerUtil.deleteSongFile(oldSongFilename);
            }
        }
    }

    private void deleteOldImageFile(String imageUrl) {
        if (imageUrl != null) {
            String oldImageFilename = fileHandlerUtil.extractFilename(imageUrl);
            if (oldImageFilename != null) {
                fileHandlerUtil.deleteImageFile(oldImageFilename);
            }
        }
    }
    private void deleteSongFiles(Song song) {
        deleteOldSongFile(song.getSongUrl());
        deleteOldImageFile(song.getImageUrl());
    }
}