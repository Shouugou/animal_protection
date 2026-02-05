package com.animalprotection.controller;

import com.animalprotection.common.ApiResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class UploadController {
    @PostMapping("/upload")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + (ext != null ? ("." + ext) : "");
        Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");
        Files.createDirectories(uploadDir);
        File dest = uploadDir.resolve(fileName).toFile();
        file.transferTo(dest);
        Map<String, Object> data = new HashMap<>();
        data.put("file_url", "/api/uploads/" + fileName);
        data.put("sha256", "");
        return ApiResponse.ok(data);
    }
}
