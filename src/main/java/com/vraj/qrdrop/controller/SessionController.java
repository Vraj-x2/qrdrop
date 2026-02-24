package com.vraj.qrdrop.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class SessionController {

    private final String uploadDir = "uploads/";
    private final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private final long SESSION_TIMEOUT_SECONDS = 300; // 5 minutes

    private final Map<String, Instant> sessionExpiry = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
    }

    @GetMapping("/")
    public String home(Model model) {
        String sessionId = UUID.randomUUID().toString();
        sessionExpiry.put(sessionId, Instant.now().plusSeconds(SESSION_TIMEOUT_SECONDS));
        model.addAttribute("sessionId", sessionId);
        return "home";
    }

    @GetMapping("/upload/{sessionId}")
    public String uploadPage(@PathVariable String sessionId, Model model) {
        if (!isSessionValid(sessionId)) return "expired";
        model.addAttribute("sessionId", sessionId);
        return "upload";
    }

    @PostMapping("/upload/{sessionId}")
    @ResponseBody
    public String upload(@PathVariable String sessionId,
                         @RequestParam("file") MultipartFile file) throws IOException {

        if (!isSessionValid(sessionId)) return "Session expired";

        if (file.getSize() > MAX_FILE_SIZE) {
            return "File too large (Max 10MB)";
        }

        File sessionFolder = new File(uploadDir + sessionId);
        if (!sessionFolder.exists()) sessionFolder.mkdirs();

        String safeFileName = file.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");

        File uploaded = new File(sessionFolder, safeFileName);
        file.transferTo(uploaded);

        return "Uploaded";
    }

    @GetMapping("/check/{sessionId}")
    @ResponseBody
    public String check(@PathVariable String sessionId) {

        if (!isSessionValid(sessionId)) return "EXPIRED";

        File sessionFolder = new File(uploadDir + sessionId);
        if (!sessionFolder.exists()) return "WAIT";

        File[] files = sessionFolder.listFiles();
        if (files != null && files.length > 0) {
            return sessionId + "/" + files[0].getName();
        }

        return "WAIT";
    }

    @GetMapping("/download/{sessionId}/{fileName}")
    public ResponseEntity<byte[]> download(@PathVariable String sessionId,
                                           @PathVariable String fileName) throws IOException {

        File file = new File(uploadDir + sessionId + "/" + fileName);
        if (!file.exists()) return ResponseEntity.notFound().build();

        byte[] data = Files.readAllBytes(file.toPath());

        FileSystemUtils.deleteRecursively(new File(uploadDir + sessionId));
        sessionExpiry.remove(sessionId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(data);
    }

    private boolean isSessionValid(String sessionId) {
        Instant expiry = sessionExpiry.get(sessionId);
        if (expiry == null) return false;
        if (Instant.now().isAfter(expiry)) {
            FileSystemUtils.deleteRecursively(new File(uploadDir + sessionId));
            sessionExpiry.remove(sessionId);
            return false;
        }
        return true;
    }
}