package com.bd.aloafy.service;

import com.bd.aloafy.dto.request.PlaylistRequest;
import com.bd.aloafy.dto.response.MessageResponse;
import com.bd.aloafy.dto.response.PaginatedResponse;
import com.bd.aloafy.dto.response.PlaylistResponse;
import com.bd.aloafy.dto.response.PlaylistWithSongsResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PlaylistService {
    PlaylistResponse createPlaylist(PlaylistRequest request, MultipartFile imageFile, String email);

    PlaylistResponse updatePlaylistPrivacy(Long id, Boolean isPublic, String email);

    MessageResponse addSongToPlaylist(Long playlistId, Long songId, String email);

    MessageResponse removeSongFromPlaylist(Long playlistId, Long songId, String email);

    MessageResponse reorderSongInPlaylist(Long playlistId, Long songId, Integer newPosition, String email);

    PaginatedResponse<PlaylistResponse> getAllPublicPlaylists(int page, int size, String search);

    PaginatedResponse<PlaylistResponse> getMyPlaylists(String email, int page, int size, String search);

    PlaylistWithSongsResponse getPlaylistWithSongs(Long playlistId, String email);

    MessageResponse deletePlaylist(Long playlistId, String email);
}
