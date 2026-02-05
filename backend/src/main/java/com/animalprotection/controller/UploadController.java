package com.animalprotection.controller;

import com.animalprotection.common.ApiResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class UploadController {
    @PostMapping("/upload")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + (ext != null ? ("." + ext) : "");
        File dir = new File("uploads");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File dest = new File(dir, fileName);
        file.transferTo(dest);
        Map<String, Object> data = new HashMap<>();
        data.put("file_url", "/uploads/" + fileName);
        data.put("sha256", "");
        return ApiResponse.ok(data);
    }
}
