package com.bd.aloafy.controller;

import com.bd.aloafy.util.FileHandlerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileHandlerUtil fileHandlerUtil;

    // Endpoint pour récupérer une musique
    @GetMapping("/song/{filename}")
    public ResponseEntity<?> getSong(@PathVariable String filename) {
        try {
            Resource resource = fileHandlerUtil.loadSongFile(filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"File not found\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    // Endpoint pour récupérer une image
    @GetMapping("/image/{filename}")
    public ResponseEntity<?> getImage(@PathVariable String filename) {
        try {
            Resource resource = fileHandlerUtil.loadImageFile(filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"File not found\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }
}
