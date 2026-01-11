package com.techup.spring_tourist.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.techup.spring_tourist.service.SupabaseStorageService;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

  private final SupabaseStorageService supabaseStorageService;

  @PostMapping("/upload")
  public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
    String url = supabaseStorageService.uploadFile(file);
    return ResponseEntity.ok(Map.of("url", url));
  }

  @PostMapping("/upload-multiple")
  public ResponseEntity<Map<String, List<String>>> uploadMultiple(@RequestParam("files") MultipartFile[] files) {
    if (files.length > 5) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", List.of("Maximum 5 files allowed")));
    }
    
    if (files.length == 0) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", List.of("At least 1 file is required")));
    }

    String[] urls = supabaseStorageService.uploadFiles(files);
    return ResponseEntity.ok(Map.of("urls", Arrays.asList(urls)));
  }
}