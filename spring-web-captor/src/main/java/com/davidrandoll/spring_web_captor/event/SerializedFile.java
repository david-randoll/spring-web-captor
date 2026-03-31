package com.davidrandoll.spring_web_captor.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SerializedFile {
    private String filename;
    private String contentType;
    private long size;
    private String base64Content;

    public SerializedFile(MultipartFile file) {
        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            this.filename = file.getOriginalFilename();
            this.contentType = file.getContentType();
            this.size = file.getSize();
            this.base64Content = base64;
        } catch (IOException e) {
            log.warn("Failed to serialize file '{}': {}", file.getOriginalFilename(), e.getMessage());
            this.filename = file.getOriginalFilename();
            this.contentType = file.getContentType();
            this.size = file.getSize();
        }
    }
}