package com.bd.aloafy.service;

import com.bd.aloafy.dto.request.SongRequest;
import com.bd.aloafy.dto.response.SongResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SongService {
    SongResponse addSong(SongRequest request, MultipartFile songFile, MultipartFile imageFile, String email);

}