package com.vraj.qrdrop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@Controller
public class SessionController {

    private final String uploadDir = "uploads/";

    @GetMapping("/")
    public String home(Model model) {
        String sessionId = UUID.randomUUID().toString();
        model.addAttribute("sessionId", sessionId);
        return "home";
    }

    @PostMapping("/upload/{sessionId}")
    @ResponseBody
    public String upload(@PathVariable String sessionId,
                         @RequestParam("file") MultipartFile file) throws IOException {

        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        File uploaded = new File(uploadDir + sessionId + "_" + file.getOriginalFilename());
        file.transferTo(uploaded);

        return "Uploaded";
    }

    @GetMapping("/check/{sessionId}")
    @ResponseBody
    public String check(@PathVariable String sessionId) {
        File folder = new File(uploadDir);
        File[] files = folder.listFiles((dir, name) -> name.startsWith(sessionId));
        if (files != null && files.length > 0) {
            return files[0].getName();
        }
        return "WAIT";
    }

    @GetMapping("/download/{fileName}")
    @ResponseBody
    public byte[] download(@PathVariable String fileName) throws IOException {
        File file = new File(uploadDir + fileName);
        byte[] data = Files.readAllBytes(file.toPath());
        file.delete(); // AUTO DELETE
        return data;
    }
    
    @GetMapping("/upload/{sessionId}")
    public String uploadPage(@PathVariable String sessionId, Model model) {
        model.addAttribute("sessionId", sessionId);
        return "upload";
    }
}