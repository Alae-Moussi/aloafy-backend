package com.bd.aloafy.controller;

import com.bd.aloafy.dto.response.SongAiInsightsResponse;
import com.bd.aloafy.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/song")
public class SongController {

    @Autowired
    private SongService songService;

    @GetMapping("/getSongAiInsights/{songId}")
    public ResponseEntity<SongAiInsightsResponse> getSongAiInsights(@PathVariable Long songId) {
        SongAiInsightsResponse response = songService.getSongAiInsights(songId);
        return ResponseEntity.ok(response);
    }
}