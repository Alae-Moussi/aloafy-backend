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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final RestTemplate restTemplate = new RestTemplate();
    private final String ANALYZE_URL = "https://alaemoussi-aloafy-api.hf.space/analyze/song";

    @Override
    public SongResponse addSong(SongRequest request, MultipartFile songFile, MultipartFile imageFile, String email) {
        AppUser appUser = getUserByEmail(email);
        String uniqueId = UUID.randomUUID().toString();
        Song song = new Song();
        song.setAppUser(appUser);
        updateSongMetadata(song, request);
        String songUrl = processSongFile(songFile, uniqueId);
        song.setSongUrl(songUrl);
        String imageUrl = processImageFile(imageFile, uniqueId);
        song.setImageUrl(imageUrl);
        Song savedSong = songRepository.save(song);
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
        Song song = validateSongAccess(id, email);
        updateSongMetadata(song, request);
        if (songFile != null && !songFile.isEmpty()) {
            deleteOldSongFile(song.getSongUrl());
            String uniqueId = UUID.randomUUID().toString();
            song.setSongUrl(processSongFile(songFile, uniqueId));
        }
        if (imageFile != null && !imageFile.isEmpty()) {
            deleteOldImageFile(song.getImageUrl());
            String uniqueId = UUID.randomUUID().toString();
            song.setImageUrl(processImageFile(imageFile, uniqueId));
        }
        return SongResponse.fromEntity(songRepository.save(song), baseUrl);
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
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found"));

        Map<String, String> body = new HashMap<>();
        body.put("title", song.getTitle());
        body.put("artist", song.getArtist());

        try {
            return restTemplate.postForObject(ANALYZE_URL, body, SongAiInsightsResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Aloafy analyze error: " + e.getMessage(), e);
        }
    }

    private String processSongFile(MultipartFile songFile, String uniqueId) {
        String songExtension = fileHandlerUtil.getFileExtension(songFile.getOriginalFilename());
        String songFilename = uniqueId + songExtension;
        fileHandlerUtil.saveSongFileWithName(songFile, songFilename);
        return "/api/file/song/" + songFilename;
    }

    private String processImageFile(MultipartFile imageFile, String uniqueId) {
        if (imageFile == null || imageFile.isEmpty()) return null;
        String imageExtension = fileHandlerUtil.getFileExtension(imageFile.getOriginalFilename());
        String imageFilename = uniqueId + imageExtension;
        fileHandlerUtil.saveImageFileWithName(imageFile, imageFilename);
        return "/api/file/image/" + imageFilename;
    }

    private void updateSongMetadata(Song song, SongRequest request) {
        song.setTitle(request.getTitle());
        song.setArtist(request.getArtist());
    }

    private AppUser getUserByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Song validateSongAccess(Long id, String email) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found"));
        AppUser appUser = getUserByEmail(email);
        boolean isOwner = song.getAppUser().getId().equals(appUser.getId());
        boolean isAdmin = "ADMIN".equals(appUser.getRole());
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You don't have permission to modify this song");
        }
        return song;
    }

    private void deleteOldSongFile(String songUrl) {
        if (songUrl != null) {
            String oldSongFilename = fileHandlerUtil.extractFilename(songUrl);
            if (oldSongFilename != null) fileHandlerUtil.deleteSongFile(oldSongFilename);
        }
    }

    private void deleteOldImageFile(String imageUrl) {
        if (imageUrl != null) {
            String oldImageFilename = fileHandlerUtil.extractFilename(imageUrl);
            if (oldImageFilename != null) fileHandlerUtil.deleteImageFile(oldImageFilename);
        }
    }

    private void deleteSongFiles(Song song) {
        deleteOldSongFile(song.getSongUrl());
        deleteOldImageFile(song.getImageUrl());
    }
}