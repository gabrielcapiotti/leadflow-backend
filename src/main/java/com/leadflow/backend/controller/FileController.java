package com.leadflow.backend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileController {

    @PostMapping("/upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {

        // Save to disk or S3
        String url = "http://localhost:8080/uploads/" + file.getOriginalFilename();

        return Map.of("file_url", url);
    }
}