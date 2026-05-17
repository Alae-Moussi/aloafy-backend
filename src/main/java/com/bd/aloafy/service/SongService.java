package com.bd.aloafy.service;

import com.bd.aloafy.dto.request.SongRequest;
import com.bd.aloafy.dto.response.MessageResponse;
import com.bd.aloafy.dto.response.SongAiInsightsResponse;
import com.bd.aloafy.dto.response.SongResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SongService {
    SongResponse addSong(SongRequest request, MultipartFile songFile, MultipartFile imageFile, String email);

    Object getAllSongs(Long userId, int page, int size, String search);

    SongResponse getSongById(Long id);

    SongResponse updateSong(Long id, SongRequest songRequest, MultipartFile songFile, MultipartFile imageFile, String email);

    MessageResponse deleteSong(Long id, String email);

    SongAiInsightsResponse getSongAiInsights(Long songId);
}