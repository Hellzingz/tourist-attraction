package com.techup.spring_tourist.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Service
public class SupabaseStorageService {

  @Value("${supabase.url:}")
  private String supabaseUrl;

  @Value("${supabase.bucket:uploads}")
  private String bucket;

  @Value("${supabase.apiKey:}")
  private String apiKey;
  
  @Value("${supabase.serviceRoleKey:}")
  private String serviceRoleKey;

  private final WebClient webClient = WebClient.builder().build();

  public String uploadFile(MultipartFile file) {
    if (supabaseUrl == null || supabaseUrl.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Supabase URL is not configured");
    }
    
    String authKey = (serviceRoleKey != null && !serviceRoleKey.isEmpty()) ? serviceRoleKey : apiKey;
    if (authKey == null || authKey.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
          "Supabase API key or Service Role Key is not configured");
    }
    
    String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file.bin";
    String sanitizedOriginal = original.replaceAll("[^a-zA-Z0-9._-]", "_");
    String fileName = System.currentTimeMillis() + "_" + sanitizedOriginal;
    String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucket, fileName);

    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot read file bytes", e);
    }

    try {
      webClient.put()
          .uri(uploadUrl)
          .header("Authorization", "Bearer " + authKey)
          .header("Content-Type", file.getContentType() != null ? file.getContentType() : "application/octet-stream")
          .header("x-upsert", "true")
          .bodyValue(bytes)
          .retrieve()
          .onStatus(HttpStatusCode::isError, res ->
              res.bodyToMono(String.class).defaultIfEmpty("Upload failed").flatMap(msg -> {
                  String errorMsg = "Supabase upload failed: " + msg;
                  if (msg.contains("row-level security") || msg.contains("RLS") || res.statusCode().value() == 403) {
                    errorMsg += " (Hint: Make sure you're using Service Role Key, not Anon Key. Also check RLS policies on the bucket)";
                  }
                  return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, errorMsg));
              })
          )
          .toBodilessEntity()
          .block();

      return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucket, fileName);

    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, 
          "Unexpected error while uploading to Supabase: " + ex.getMessage(), ex);
    }
  }

  public String[] uploadFiles(MultipartFile[] files) {
    String[] urls = new String[files.length];
    for (int i = 0; i < files.length; i++) {
      MultipartFile file = files[i];
      
      if (file.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
            String.format("File at index %d is empty", i));
      }
      
      urls[i] = uploadFile(file);
    }
    
    return urls;
  }
}